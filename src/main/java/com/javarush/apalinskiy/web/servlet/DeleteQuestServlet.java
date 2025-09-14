package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.service.quest.QuestAuthoringService;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.service.notify.NotificationService;
import com.javarush.apalinskiy.domain.notify.NotificationType;
import com.javarush.apalinskiy.domain.quest.custom.CustomQuest;
import com.javarush.apalinskiy.service.user.UserService;
import com.javarush.apalinskiy.web.util.Web;
import com.javarush.apalinskiy.app.WebConst;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * Servlet responsible for deleting quests from the catalog.
 * <p>
 * Supports only <b>POST</b> requests. A quest may be deleted either by its owner
 * or by an administrator:
 * <ul>
 *   <li>Owners can delete only their own quests.</li>
 *   <li>Administrators can delete any quest, and the quest owner will be notified if present.</li>
 * </ul>
 * <p>
 * <b>GET</b> requests are explicitly disallowed and result in HTTP 405 (Method Not Allowed).
 * <p>
 * Side effects include:
 * <ul>
 *   <li>Removing a quest from the catalog repository.</li>
 *   <li>Setting flash or error messages in the session.</li>
 *   <li>Redirecting to a "next" page or default path.</li>
 *   <li>Sending notifications to quest owners when an administrator deletes their quest.</li>
 * </ul>
 *
 * <b>Security:</b> Assumes that user authentication is handled outside. An anonymous user
 * is redirected to the login page.
 *
 * @author Your Name
 * @since 1.0
 */
public class DeleteQuestServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(DeleteQuestServlet.class);

    /**
     * Service providing quest catalog access and deletion operations.
     */
    private transient QuestAuthoringService authoring;

    /**
     * Notification service for informing users when an admin deletes their quest.
     * Optional — may be {@code null}.
     */
    private transient NotificationService notifications;

    /**
     * User service for resolving user details (optional).
     */
    private transient UserService users;

    /**
     * Initializes required and optional backend services from the servlet context.
     * <p>
     * Required:
     * <ul>
     *   <li>{@link QuestAuthoringService}</li>
     * </ul>
     * Optional:
     * <ul>
     *   <li>{@link NotificationService}</li>
     *   <li>{@link UserService}</li>
     * </ul>
     * If {@link QuestAuthoringService} is not found, the servlet is marked unavailable.
     *
     * @param config servlet configuration
     * @throws ServletException     if superclass initialization fails
     * @throws UnavailableException if required services are missing
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            this.authoring = Web.ctxBean(
                    config.getServletContext(),
                    WebConst.Ctx.AUTHORING_SERVICE,
                    QuestAuthoringService.class
            );
        } catch (IllegalStateException e) {
            log.error("Init failed: QuestAuthoringService missing", e);
            throw new UnavailableException("QuestAuthoringService not found in ServletContext");
        }
        try {
            this.notifications = Web.ctxBean(
                    config.getServletContext(),
                    WebConst.Ctx.NOTIFY_SERVICE,
                    NotificationService.class
            );
        } catch (IllegalStateException ignore) {
        }
        try {
            this.users = Web.ctxBean(
                    config.getServletContext(),
                    WebConst.Ctx.USER_SERVICE,
                    UserService.class
            );
        } catch (IllegalStateException ignore) {
        }
        log.debug("DeleteQuestServlet initialized: authoring={}, notifications={}, users={}",
                (authoring == null ? "null" : authoring.getClass().getSimpleName()),
                (notifications == null ? "null" : notifications.getClass().getSimpleName()),
                (users == null ? "null" : users.getClass().getSimpleName()));
    }

    /**
     * Handles POST requests for deleting a quest.
     * <p>
     * Behavior:
     * <ul>
     *   <li>Requires an authenticated {@link User} stored in the session.
     *       Anonymous users are redirected to the login page.</li>
     *   <li>Retrieves quest id from {@link WebConst.Param#ID} and an optional next URL
     *       from {@link WebConst.Param#NEXT}. Defaults to {@link WebConst.Path#MY_QUESTS}.</li>
     *   <li>Loads quest details to resolve quest name and owner id.</li>
     *   <li>Deletion rules:
     *     <ul>
     *       <li>If the user is an admin, deletes the quest unconditionally.</li>
     *       <li>If the user is not an admin, deletes the quest only if they are the owner.</li>
     *     </ul>
     *   </li>
     *   <li>On successful deletion:
     *     <ul>
     *       <li>Stores a flash success message.</li>
     *       <li>If deleted by admin, notifies the quest owner (when different from the actor).</li>
     *     </ul>
     *   </li>
     *   <li>On failure: stores an error message describing the reason.</li>
     *   <li>Always redirects back to the "next" URL.</li>
     * </ul>
     *
     * @param req  current HTTP request
     * @param resp current HTTP response
     * @throws IOException if sending a redirect or error response fails
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User actor = (User) req.getSession().getAttribute(WebConst.Attr.USER);
        if (actor == null) {
            log.info("Delete quest: anonymous user -> redirect to login");
            Web.redirect(req, resp, WebConst.Path.LOGIN, Map.of());
            return;
        }
        String questId = Web.trimOrNull(req.getParameter(WebConst.Param.ID));
        String next = Web.safeNextOrHome(req, req.getParameter(WebConst.Param.NEXT));
        if (next == null) {
            next = req.getContextPath() + WebConst.Path.MY_QUESTS;
        }
        if (questId == null) {
            log.warn("Delete quest: missing questId by actorId={}", actor.getUserId());
            req.getSession().setAttribute(WebConst.Attr.ERROR, "The quest ID is not specified.");
            resp.sendRedirect(resp.encodeRedirectURL(next));
            return;
        }
        Optional<CustomQuest> questOpt = authoring.getFromCatalog(questId);
        String questName = questOpt.map(CustomQuest::getName).orElse("Quest");
        String ownerId = questOpt.map(CustomQuest::getOwnerId).orElse(null);
        try {
            boolean isAdmin = "ADMIN".equals(String.valueOf(actor.getRole()));
            log.info("Delete quest attempt questId={} by actorId={} isAdmin={}", questId, actor.getUserId(), isAdmin);
            boolean removed = isAdmin
                    ? authoring.deleteFromCatalogAsAdmin(questId)
                    : authoring.deleteFromCatalogIfOwner(questId, actor.getUserId());
            if (removed) {
                req.getSession().setAttribute(WebConst.Attr.FLASH, "The quest has been deleted.");
                log.info("Delete quest success questId={} by actorId={} questName='{}'", questId, actor.getUserId(), questName);
                if (isAdmin && ownerId != null && notifications != null && !ownerId.equals(actor.getUserId())) {
                    String shortTitle = Web.shortTitle(questName);
                    notifications.add(
                            ownerId,
                            NotificationType.QUEST_ADMIN_CHANGED,
                            "The administrator deleted your quest.\n",
                            "Quest <b>" + shortTitle + "</b> was deleted by the administrator."
                    );
                    log.info("Owner notified about deletion ownerId={} questId={}", ownerId, questId);
                }
            } else {
                String msg = isAdmin
                        ? "Cannot be deleted: not found."
                        : "Cannot be deleted: not found or you are not the owner.";
                req.getSession().setAttribute(WebConst.Attr.ERROR, msg);
                log.warn("Delete quest failed questId={} by actorId={} reason='{}'", questId, actor.getUserId(), msg);
            }
        } catch (IllegalArgumentException | IllegalStateException ex) {
            req.getSession().setAttribute(WebConst.Attr.ERROR, "Deletion error: " + ex.getMessage());
            log.warn("Delete quest exception questId={} by actorId={} msg={}", questId, actor.getUserId(), ex.getMessage());
        }
        resp.sendRedirect(resp.encodeRedirectURL(next));
    }

    /**
     * Explicitly disallows GET requests.
     * Always responds with {@link HttpServletResponse#SC_METHOD_NOT_ALLOWED} (405).
     *
     * @param req  current HTTP request
     * @param resp current HTTP response
     * @throws IOException if sending the error fails
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        log.warn("Delete quest GET not allowed");
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }
}
