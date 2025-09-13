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
 * Controller that renders quest nodes and processes user choices for both
 * the built-in (main) quest and custom quests from the catalog.
 *
 * <p><b>GET</b> resolves the target node (by {@code id} or {@code node} parameter; otherwise start)
 * and forwards to the quest view.</p>
 *
 * <p><b>POST</b> processes a user choice from a given node:
 * validates {@code fromId}, resolves the next node, optionally records completion statistics,
 * and redirects to the next node or re-renders the current one on error.</p>
 *
 * <h3>Services</h3>
 * <ul>
 *   <li>Required: {@link QuestService} (production quest content).</li>
 *   <li>Optional: {@link QuestAuthoringService} (to serve custom quests from the catalog).</li>
 *   <li>Optional: {@link UserStatsService} (quest-completion analytics).</li>
 * </ul>
 *
 * <h3>Custom quests</h3>
 * <ul>
 *   <li>Presence of a normalized {@code custom} parameter switches to a custom quest service
 *       resolved via {@link #resolveCustomService(String)}.</li>
 *   <li>If a provided custom quest id is missing or deleted, responds with 404.</li>
 * </ul>
 *
 * <h3>View</h3>
 * <ul>
 *   <li>Forwards to {@code WebConst.Jsp.QUEST} with attributes:
 *     <ul>
 *       <li>{@code node} — current {@link QuestNode}</li>
 *       <li>{@code version} — underlying content version (main/custom)</li>
 *       <li>optional {@code error} — message when re-rendering after a bad choice</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h3>Security notes</h3>
 * <ul>
 *   <li>Mutating requests (POST) should be CSRF-protected by upstream middleware/filters.</li>
 * </ul>
 */
public class QuestServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(QuestServlet.class);

    private transient QuestService prodService;
    private transient QuestAuthoringService authoring;
    private transient UserStatsService userStats;

    /**
     * Resolves required/optional services from the servlet context.
     *
     * <p>Required: {@link QuestService}. Optional: {@link QuestAuthoringService} (if present under
     * {@code WebConst.Ctx.AUTHORING_SERVICE}) and {@link UserStatsService}.</p>
     *
     * @param config servlet config
     * @throws ServletException when required services are missing
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
     * Renders the requested quest node (main or custom).
     *
     * <p>Flow:</p>
     * <ol>
     *   <li>Normalize custom quest id via {@link Web#normalizedCustomParam(HttpServletRequest)}.</li>
     *   <li>If {@link Web#isMissingCustomId(String, QuestAuthoringService)} returns true, send 404.</li>
     *   <li>Choose a backing service: main ({@code prodService}) or custom ({@link #resolveCustomService(String)}).</li>
     *   <li>Resolve the node id (first of {@code id}, {@code node}); default to start node.</li>
     *   <li>If the node is missing, set an error message and fall back to the start node.</li>
     *   <li>Pull flash, set request attributes (custom id and quest title), and forward to the view.</li>
     * </ol>
     *
     * @param req  HTTP request with optional {@code id}/{@code node} and {@code custom}
     * @param resp HTTP response
     * @throws ServletException if forwarding fails
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
     * Processes a choice from a given node and navigates to the next node.
     *
     * <p>Flow:</p>
     * <ol>
     *   <li>Normalize custom quest id and validate that the referenced custom quest exists; else 404.</li>
     *   <li>Resolve the backing quest service (main or custom).</li>
     *   <li>Require {@code fromId}; if missing, re-render the start node with a {@code BAD_FROM_ID} error.</li>
     *   <li>Execute {@link TempService#choose(int, String)} with the submitted {@code answer}.</li>
     *   <li>If OK:
     *     <ul>
     *       <li>If the next node is final and {@code userStats} is available and a user is logged in, record completion via {@link UserStatsService#onQuestCompleted(String, String, Integer)}.</li>
     *       <li>Redirect to the next node URL via {@link Web#questUrl(HttpServletRequest, int, String)}.</li>
     *     </ul>
     *   </li>
     *   <li>If error: re-render the current (or start) node, attach error message.</li>
     * </ol>
     *
     * @param req  HTTP request that contains {@code fromId} and {@code answer}
     * @param resp HTTP response
     * @throws ServletException if forwarding fails
     * @throws IOException      on redirect/I-O errors
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
            if (next != null && next.isFin() && userStats != null) {
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
     * Builds a temporary service facade backed by a custom quest from the catalog.
     *
     * @param customId custom quest id (must exist in catalog)
     * @return a {@link TempService} wrapping a {@link QuestNavigator} for the custom quest
     * @throws ServletException if authoring service is missing or the quest cannot be found
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
     * Forwards to the quest JSP with the current node, content version, and optional error message.
     *
     * @param req     HTTP request
     * @param resp    HTTP response
     * @param node    node to render
     * @param version backing content version string
     * @param error   optional error message to display (nullable/blank ignored)
     * @throws ServletException if forwarding fails
     * @throws IOException      on I/O errors
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
     * Minimal interface to abstract over two backends:
     * the production {@link QuestService} and an in-memory {@link QuestNavigator} for custom quests.
     */
    private interface TempService {

        /**
         * @return start node of the quest
         */
        QuestNode getStart();

        /**
         * @return node by id or {@code null} if absent
         */
        QuestNode getById(int id);

        /**
         * Resolves the next node based on the user's choice.
         */
        ChooseResult choose(int fromId, String answer);

        /**
         * @return a version string to expose in the view (e.g., build hash or custom id)
         */
        String version();

        /**
         * Factory: wraps a production {@link QuestService}.
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
         * Factory: wraps a {@link QuestNavigator} built from a custom quest and exposes a fixed version label.
         *
         * @param nav     navigator over custom quest nodes
         * @param version version label to expose to the view
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