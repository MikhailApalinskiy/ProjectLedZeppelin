package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.domain.quest.index.QuestNavigator;
import com.javarush.apalinskiy.domain.quest.QuestNode;
import com.javarush.apalinskiy.service.quest.QuestAuthoringService;
import com.javarush.apalinskiy.service.quest.QuestService;
import com.javarush.apalinskiy.domain.quest.choice.ChoiceError;
import com.javarush.apalinskiy.domain.quest.choice.ChooseResult;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.domain.quest.custom.CustomQuest;
import com.javarush.apalinskiy.service.user.UserStatsService;
import com.javarush.apalinskiy.web.util.Web;
import com.javarush.apalinskiy.app.WebConst;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

/**
 * Handles rendering and progression of the quest (main or custom).
 *
 * <p>Supports two sources of quest content:
 * <ul>
 *   <li><b>Main quest</b> – served by a production {@link QuestService}.</li>
 *   <li><b>Custom quest</b> – resolved from the catalog via {@link QuestAuthoringService}
 *       and navigated with {@link QuestNavigator}.</li>
 * </ul>
 *
 * <p>On POST (choice submission), the servlet computes the next node using the active
 * service and redirects to that node. When a final node is reached, it records completion
 * via {@link UserStatsService} (if available).</p>
 */
public class QuestServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(QuestServlet.class);

    private transient QuestService prodService;
    private transient QuestAuthoringService authoring;
    private transient UserStatsService userStats;

    /**
     * Initializes dependencies from the {@link ServletContext}.
     *
     * @throws UnavailableException if the mandatory {@link QuestService} is missing
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        ServletContext ctx = config.getServletContext();
        try {
            this.prodService = Web.ctxBean(ctx, WebConst.Ctx.QUEST_SERVICE, QuestService.class);
        } catch (IllegalStateException e) {
            log.error("Init failed: QuestService is not initialized", e);
            throw new UnavailableException("QuestService is not initialized");
        }
        Object a = ctx.getAttribute(WebConst.Ctx.AUTHORING_SERVICE);
        if (a instanceof QuestAuthoringService as) {
            this.authoring = as;
        }
        try {
            this.userStats = Web.ctxBean(ctx, WebConst.Ctx.USER_STATS_SERVICE, UserStatsService.class);
        } catch (IllegalStateException ignore) {
            this.userStats = null;
        }
        log.debug("QuestServlet init: prodService={}, authoring={}, userStats={}",
                prodService.getClass().getSimpleName(),
                (authoring == null ? "none" : authoring.getClass().getSimpleName()),
                (userStats == null ? "none" : userStats.getClass().getSimpleName()));
    }

    /**
     * Renders a quest node page.
     *
     * <p>Determines the active quest (main or custom) by the {@code custom} parameter,
     * then loads either the requested node (by {@code id}/{@code node}) or the start node.
     * If the requested node is missing, falls back to the start node and shows an error.</p>
     *
     * @param req  HTTP request (expects optional {@code custom}, {@code id}/{@code node})
     * @param resp HTTP response
     * @throws ServletException on forwarding errors
     * @throws IOException      on I/O errors
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String customId = Web.normalizedCustomParam(req);
        if (Web.isMissingCustomId(customId, authoring)) {
            log.warn("Quest GET: custom quest missing or deleted customId={}", customId);
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Custom quest not found or was deleted");
            return;
        }
        TempService svc = (customId == null) ? TempService.from(prodService) : resolveCustomService(customId);
        Integer id = Web.firstIntParam(req, WebConst.Param.ID, WebConst.Param.NODE);
        QuestNode node = (id == null) ? svc.getStart() : svc.getById(id);
        if (node == null) {
            req.setAttribute(WebConst.Attr.ERROR, WebConst.Msg.NODE_NOT_FOUND_PREFIX + id);
            node = svc.getStart();
            log.warn("Quest GET: node not found, fallback to start customId={} requestedId={}", customId, id);
        }
        Web.pullFlash(req, WebConst.Attr.FLASH);
        req.setAttribute(WebConst.Attr.CUSTOM, customId);
        req.setAttribute("questTitle", Web.displayName(customId, authoring));
        log.debug("Quest GET: render node id={} customId={} version={}", node.getId(), customId, svc.version());
        forwardQuest(req, resp, node, svc.version(), null);
    }

    /**
     * Handles a choice made on a quest node.
     *
     * <p>Reads the current node id ({@code fromId}) and the chosen answer ({@code answer}),
     * resolves the next node, and redirects to it. If the next node is final, a completion
     * event is recorded (if {@link UserStatsService} is present).</p>
     *
     * <p>On invalid input, forwards the current node with an error message.</p>
     *
     * @param req  HTTP request (expects {@code fromId}, {@code answer}, optional {@code custom})
     * @param resp HTTP response
     * @throws ServletException on forwarding errors
     * @throws IOException      on I/O errors
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String customId = Web.normalizedCustomParam(req);
        if (Web.isMissingCustomId(customId, authoring)) {
            log.warn("Quest POST: custom quest missing or deleted customId={}", customId);
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Custom quest not found or was deleted");
            return;
        }
        TempService svc = (customId == null) ? TempService.from(prodService) : resolveCustomService(customId);
        Integer fromId = Web.parseIntOrNull(req.getParameter(WebConst.Param.FROM_ID));
        if (fromId == null) {
            log.warn("Quest POST: missing fromId customId={}", customId);
            forwardQuest(req, resp, svc.getStart(), svc.version(), WebConst.Msg.BAD_FROM_ID);
            return;
        }
        String answer = req.getParameter(WebConst.Param.ANSWER);
        ChooseResult result = svc.choose(fromId, answer);
        if (result.isOk()) {
            QuestNode next = result.getNext();
            if (next != null && next.getFin() && userStats != null) {
                User u = (User) req.getSession().getAttribute(WebConst.Attr.USER);
                if (u != null) {
                    String questKey = (customId == null) ? "main" : customId;
                    Integer finalId = (customId == null) ? next.getId() : null;
                    try {
                        userStats.onQuestCompleted(u.getUserId(), questKey, finalId);
                        log.info("Quest completed userId={} questKey={} finalNodeId={}", u.getUserId(), questKey, finalId);
                    } catch (Exception e) {
                        log.warn("Quest completion stats failed userId={} questKey={} finalNodeId={}",
                                u.getUserId(), questKey, finalId, e);
                    }
                }
            }
            if (next == null) {
                log.error("Quest POST: next node is null fromId={} customId={}", fromId, customId);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Next node is null");
                return;
            }
            String target = Web.questUrl(req, next.getId(), customId);
            log.info("Quest choice OK fromId={} -> nextId={} customId={} redirect={}", fromId, next.getId(), customId, target);
            resp.sendRedirect(resp.encodeRedirectURL(Web.questUrl(req, next.getId(), customId)));
        } else {
            QuestNode node = svc.getById(fromId);
            if (node == null) {
                node = svc.getStart();
                log.warn("Quest choice ERR: fromId={} not found, fallback to start customId={}", fromId, customId);
            }
            req.setAttribute(WebConst.Attr.CUSTOM, customId);
            log.info("Quest choice ERR fromId={} customId={} reason='{}'", fromId, customId, result.getMessage());
            forwardQuest(req, resp, node, svc.version(), result.getMessage());
        }
    }

    /**
     * Creates a lightweight quest service wrapper for a specific custom quest id.
     *
     * @param customId custom quest identifier
     * @return a {@link TempService} backed by {@link QuestNavigator}
     * @throws UnavailableException if the authoring service is missing or the quest cannot be resolved
     */
    private TempService resolveCustomService(String customId) throws ServletException {
        if (authoring == null) {
            log.error("resolveCustomService: authoring service is null");
            throw new UnavailableException("Authoring service is not available");
        }
        CustomQuest q = authoring.getFromCatalog(customId).orElseThrow(
                () -> new UnavailableException("Custom quest is missing"));
        QuestNavigator nav = QuestNavigator.from(q.getNodes(), q.getStartId());
        log.debug("Resolved custom service customId={} name='{}' startId={}", customId, q.getName(), q.getStartId());
        return TempService.from(nav, "custom:" + q.getId());
    }

    /**
     * Forwards to the JSP with the prepared model.
     *
     * @param req     request
     * @param resp    response
     * @param node    node to render
     * @param version content version string
     * @param error   optional error message to show on the page (nullable)
     */
    private void forwardQuest(HttpServletRequest req, HttpServletResponse resp,
                              QuestNode node, String version, String error)
            throws ServletException, IOException {
        req.setAttribute(WebConst.Attr.NODE, node);
        req.setAttribute(WebConst.Attr.VERSION, version);
        if (error != null && !error.isBlank()) {
            req.setAttribute(WebConst.Attr.ERROR, error);
        }
        Web.forward(req, resp, WebConst.Jsp.QUEST);
    }

    /**
     * Minimal abstraction that adapts either a {@link QuestService} or a {@link QuestNavigator}
     * to a uniform API for rendering and navigation.
     */
    private interface TempService {

        /**
         * @return the start node of the quest
         */
        QuestNode getStart();

        /**
         * Returns a node by its id.
         *
         * @param id node id
         * @return node or {@code null} if not found
         */
        QuestNode getById(int id);

        /**
         * Applies a user choice on a node and returns a navigation result.
         *
         * @param fromId current node id
         * @param answer raw answer string (option text)
         * @return ok result with next node, or error result with details
         */
        ChooseResult choose(int fromId, String answer);

        /**
         * @return a version string describing the underlying content
         */
        String version();

        /**
         * Adapts a production {@link QuestService}.
         */
        static TempService from(QuestService prod) {
            return new TempService() {
                @Override
                public QuestNode getStart() {
                    return prod.getStart();
                }

                @Override
                public QuestNode getById(int id) {
                    return prod.getById(id);
                }

                @Override
                public ChooseResult choose(int fromId, String answer) {
                    return prod.choose(fromId, answer);
                }

                @Override
                public String version() {
                    return prod.version();
                }
            };
        }

        /**
         * Adapts a {@link QuestNavigator} built from a custom quest.
         *
         * @param nav     in-memory navigator
         * @param version version label to expose
         */
        static TempService from(QuestNavigator nav, String version) {
            return new TempService() {
                @Override
                public QuestNode getStart() {
                    return nav.start();
                }

                @Override
                public QuestNode getById(int id) {
                    return nav.get(id);
                }

                @Override
                public ChooseResult choose(int fromId, String answer) {
                    Optional<QuestNode> opt = nav.choose(fromId, answer);
                    return opt.map(ChooseResult::ok)
                            .orElseGet(() -> ChooseResult.error(ChoiceError.NO_SUCH_OPTION, "No such option: "));
                }

                @Override
                public String version() {
                    return version;
                }
            };
        }
    }
}