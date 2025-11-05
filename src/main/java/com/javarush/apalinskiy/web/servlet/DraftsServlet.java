package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.utils.CurrentUserContext;
import com.javarush.apalinskiy.utils.HibernateUtil;
import com.javarush.apalinskiy.app.WebConst;
import com.javarush.apalinskiy.domain.quest.custom.CustomQuest;
import com.javarush.apalinskiy.domain.quest.custom.DraftRow;
import com.javarush.apalinskiy.repository.hibernate.quest.HDraftRepository;
import com.javarush.apalinskiy.service.quest.QuestAuthoringService;
import com.javarush.apalinskiy.web.util.Web;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Servlet for managing user drafts of custom quests.
 *
 * <p>Handles the draft list page and provides actions to open, create,
 * rename, and delete drafts.</p>
 *
 * <p>GET — lists all drafts belonging to the current user, with pagination and search.</p>
 * <p>POST — processes actions via {@code action} parameter:
 * <ul>
 *   <li>{@code open} — loads selected draft into the editor</li>
 *   <li>{@code new} — creates a blank new draft</li>
 *   <li>{@code rename} — renames existing draft</li>
 *   <li>{@code delete} — removes a draft</li>
 * </ul>
 */
public class DraftsServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(DraftsServlet.class);

    private QuestAuthoringService authoring;
    private HDraftRepository drafts;

    /**
     * Initializes dependencies for draft management.
     *
     * @param config servlet config
     * @throws ServletException if initialization fails
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        log.debug("Initializing DraftsServlet...");
        ServletContext ctx = config.getServletContext();
        try {
            this.authoring = Web.ctxBean(ctx, WebConst.Ctx.AUTHORING_SERVICE, QuestAuthoringService.class);
            this.drafts = new HDraftRepository(HibernateUtil.getSessionFactory());
            log.info("DraftsServlet initialized successfully with QuestAuthoringService and HDraftRepository.");
        } catch (Exception e) {
            log.error("Failed to initialize DraftsServlet", e);
            throw new ServletException("Failed to initialize DraftsServlet", e);
        }
    }

    /**
     * Renders the draft list page for the current user.
     *
     * <p>Includes pagination and search by name or keyword. If the user is not logged in,
     * redirects to the login page.</p>
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        log.trace("DraftsServlet.doGet: start uri={} query={}", req.getRequestURI(), req.getQueryString());
        Web.pullFlash(req, WebConst.Attr.OK);
        Web.pullFlash(req, WebConst.Attr.ERROR);
        String uid = CurrentUserContext.require();
        if (uid.isBlank()) {
            log.warn("DraftsServlet.doGet: no user in context, redirecting to login");
            Web.redirectErr(req, resp, WebConst.Path.LOGIN, "Please login");
            return;
        }
        log.debug("DraftsServlet.doGet: userId={}", uid);
        Web.Params params = Web.extract(req);
        log.trace("DraftsServlet.doGet: extracted params page={} size={} q='{}'",
                params.page, params.size, params.q);
        int total = drafts.countByOwner(uid, params.q);
        int pages = Math.max(1, (int) Math.ceil(total / (double) params.size));
        int safePage = Math.min(params.page, pages);
        log.debug("DraftsServlet.doGet: total={} pages={} safePage={}", total, pages, safePage);
        List<DraftRow> items = (total == 0)
                ? List.of()
                : drafts.findByOwnerPaged(uid, safePage, params.size, params.q);
        log.trace("DraftsServlet.doGet: fetched {} drafts for userId={}", items.size(), uid);
        req.setAttribute("items", items);
        req.setAttribute("total", total);
        req.setAttribute("pages", pages);
        req.setAttribute("page", safePage);
        req.setAttribute("q", params.q);
        String selfPathOnly = req.getServletPath();
        req.setAttribute("selfPathOnly", selfPathOnly);
        log.debug("DraftsServlet.doGet: forwarding to JSP={}, userId={}", WebConst.Jsp.DRAFTS, uid);
        Web.forward(req, resp, WebConst.Jsp.DRAFTS);
    }

    /**
     * Handles POST actions from the draft list.
     *
     * <p>Actions:</p>
     * <ul>
     *   <li><b>open</b> — loads selected draft into the quest editor</li>
     *   <li><b>new</b> — creates a blank draft</li>
     *   <li><b>rename</b> — renames a draft</li>
     *   <li><b>delete</b> — deletes a draft</li>
     * </ul>
     *
     * <p>Redirects to the relevant page with a success or error message in session.</p>
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String act = Web.trimOrNull(req.getParameter("action"));
        if (act == null) {
            log.warn("POST /drafts received without 'action' parameter");
            Web.redirectErr(req, resp, WebConst.Path.DRAFTS, "Action not specified");
            return;
        }
        String uid = CurrentUserContext.require();
        log.debug("POST /drafts action='{}' from user {}", act, uid);
        try {
            switch (act) {
                case "open" -> {
                    String draftId = Web.trimOrNull(req.getParameter("draftId"));
                    log.info("User {} opening draft {}", uid, draftId);
                    authoring.loadDraftIntoEditor(draftId);
                    HttpSession s = req.getSession(true);
                    DraftRow dr = drafts.get(draftId).orElse(null);
                    if (dr != null) {
                        log.trace("Draft {} loaded successfully", draftId);
                        String tq = Web.trimOrNull(dr.getTargetQuestId());
                        if (tq != null) {
                            Optional<CustomQuest> opt = authoring.getFromCatalog(tq);
                            if (opt.isPresent() && opt.get().getModerationStatus() == CustomQuest.ModerationStatus.LIVE) {
                                s.setAttribute("editingQuestId", tq);
                                s.setAttribute("editingQuestName", opt.get().getName());
                                log.debug("Linked to live quest '{}' ({})", opt.get().getName(), tq);
                            } else {
                                s.removeAttribute("editingQuestId");
                                s.removeAttribute("editingQuestName");
                                log.debug("Target quest {} not live or not found", tq);
                            }
                        } else {
                            s.removeAttribute("editingQuestId");
                            s.removeAttribute("editingQuestName");
                            log.trace("Draft {} not linked to any quest", draftId);
                        }
                    } else {
                        s.removeAttribute("editingQuestId");
                        s.removeAttribute("editingQuestName");
                        log.warn("Draft {} not found for user {}", draftId, uid);
                    }

                    Web.redirectOk(req, resp, WebConst.Path.CREATE, "The draft is open in the editor");
                }
                case "new" -> {
                    String name = Web.trimOrNull(req.getParameter("name"));
                    log.info("User {} creating new draft '{}'", uid, name);
                    authoring.newEmptyDraft(name);
                    HttpSession s = req.getSession(true);
                    s.removeAttribute("editingQuestId");
                    s.removeAttribute("editingQuestName");
                    Web.redirectOk(req, resp, WebConst.Path.CREATE, "A blank draft has been created");
                }
                case "rename" -> {
                    String draftId = Web.trimOrNull(req.getParameter("draftId"));
                    String name = Objects.requireNonNull(req.getParameter("name"), "name");
                    log.info("User {} renaming draft {} -> '{}'", uid, draftId, name);
                    drafts.rename(draftId, name, uid);
                    Web.redirectOk(req, resp, WebConst.Path.DRAFTS, "Renamed");
                }
                case "delete" -> {
                    String draftId = Web.trimOrNull(req.getParameter("draftId"));
                    log.info("User {} deleting draft {}", uid, draftId);
                    drafts.delete(draftId, uid);
                    Web.redirectOk(req, resp, WebConst.Path.DRAFTS, "Deleted");
                }
                default -> {
                    log.warn("Unknown POST action '{}' from user {}", act, uid);
                    Web.redirectErr(req, resp, WebConst.Path.DRAFTS, "Unknown action");
                }
            }
        } catch (Exception e) {
            log.error("Error while processing POST /drafts action='{}' for user {}", act, uid, e);
            Web.redirectErr(req, resp, WebConst.Path.DRAFTS, e.getMessage());
        }
    }
}
