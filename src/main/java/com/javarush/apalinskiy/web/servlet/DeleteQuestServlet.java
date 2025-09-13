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
import java.util.Optional;

/**
 * Handles deletion of custom quests either by the quest owner or by an administrator.
 * <p>
 * This servlet expects the authenticated {@link User} to be present in the HTTP session and
 * dispatches a delete operation against {@link QuestAuthoringService}. When an admin deletes
 * a quest that belongs to another user, an optional notification is sent to the owner.
 * </p>
 *
 * <h3>Lifecycle</h3>
 * <ul>
 *   <li>On {@link #init(ServletConfig)}: resolves required {@link QuestAuthoringService}
 *       and optional {@link NotificationService} / {@link UserService} from the context.</li>
 *   <li>If {@code QuestAuthoringService} is missing, initialization fails with {@link UnavailableException}.</li>
 * </ul>
 *
 * <h3>POST /delete-quest</h3>
 * <p>Parameters:</p>
 * <ul>
 *   <li><b>id</b> — quest identifier (required);</li>
 *   <li><b>next</b> — optional next URL (validated via {@link Web#safeNextOrHome}).</li>
 * </ul>
 * <p>Behavior:</p>
 * <ul>
 *   <li>Requires an authenticated user in session; otherwise redirects to login.</li>
 *   <li>If the actor is an admin, calls {@code deleteFromCatalogAsAdmin(id)}; otherwise,
 *       calls {@code deleteFromCatalogIfOwner(id, actorLogin)}.</li>
 *   <li>On success: sets a flash message and, when performed by an admin on someone else’s quest,
 *       notifies the owner (if notification and user services are available).</li>
 *   <li>On failure: sets an appropriate error flash message.</li>
 *   <li>Redirects to {@code next} (or {@code /my-quests} as a fallback).</li>
 * </ul>
 *
 * <h3>GET</h3>
 * <ul>
 *   <li>Responds with 405 (method not allowed).</li>
 * </ul>
 *
 * <h3>Notifications</h3>
 * <ul>
 *   <li>When an admin deletes a quest, {@link NotificationType#QUEST_ADMIN_CHANGED} is sent to the owner
 *       with a short HTML message (if the owner is not the actor and services are available).</li>
 * </ul>
 *
 * <h3>Notes</h3>
 * <ul>
 *   <li>Logging includes actor id, quest id, and outcome.</li>
 *   <li>All service fields are {@code transient} to avoid accidental serialization.</li>
 * </ul>
 *
 * @see QuestAuthoringService#deleteFromCatalogIfOwner(String, String)
 * @see QuestAuthoringService#deleteFromCatalogAsAdmin(String)
 * @see Web#safeNextOrHome(HttpServletRequest, String)
 * @see WebConst
 */
public class DeleteQuestServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(DeleteQuestServlet.class);

    private transient QuestAuthoringService authoring;
    private transient NotificationService notifications;
    private transient UserService users;

    /**
     * Resolves required and optional services from the {@link jakarta.servlet.ServletContext}.
     *
     * <p>Required:</p>
     * <ul>
     *   <li>{@link QuestAuthoringService} under {@code WebConst.Ctx.AUTHORING_SERVICE}</li>
     * </ul>
     * <p>Optional:</p>
     * <ul>
     *   <li>{@link NotificationService} under {@code WebConst.Ctx.NOTIFY_SERVICE}</li>
     *   <li>{@link UserService} under {@code WebConst.Ctx.USER_SERVICE}</li>
     * </ul>
     *
     * @param config servlet config provided by the container
     * @throws ServletException     if initialization fails
     * @throws UnavailableException if the required {@link QuestAuthoringService} is missing
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
     * Deletes a quest from the catalog on behalf of the current user.
     * <p>
     * Behavior:
     * <ul>
     *   <li>Requires an authenticated {@link User} in session; otherwise redirects to login.</li>
     *   <li>Reads {@code id} (questId) and optional {@code next} from the request.</li>
     *   <li>If the actor has {@code ADMIN} role, invokes
     *       {@link QuestAuthoringService#deleteFromCatalogAsAdmin(String)}; otherwise
     *       {@link QuestAuthoringService#deleteFromCatalogIfOwner(String, String)} using actor's login.</li>
     *   <li>Sets {@code FLASH} on success, {@code ERROR} on failure; always redirects to {@code next}
     *       (or {@code /my-quests} fallback).</li>
     *   <li>If an admin deletes another user's quest and notifications are available, sends a
     *       {@link NotificationType#QUEST_ADMIN_CHANGED} to the owner.</li>
     * </ul>
     *
     * @param req  HTTP request; expects parameters {@code id} and optional {@code next}
     * @param resp HTTP response used for redirects
     * @throws IOException if redirect fails
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User actor = (User) req.getSession().getAttribute(WebConst.Attr.USER);
        if (actor == null) {
            log.info("Delete quest: anonymous user -> redirect to login");
            Web.redirect(req, resp, WebConst.Path.LOGIN, java.util.Map.of());
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
        String ownerLogin = questOpt.map(CustomQuest::getOwnerLogin).orElse(null);
        try {
            boolean isAdmin = "ADMIN".equals(String.valueOf(actor.getRole()));
            log.info("Delete quest attempt questId={} by actorId={} isAdmin={}", questId, actor.getUserId(), isAdmin);
            boolean removed = isAdmin
                    ? authoring.deleteFromCatalogAsAdmin(questId)
                    : authoring.deleteFromCatalogIfOwner(questId, actor.getUserLogin());
            if (removed) {
                req.getSession().setAttribute(WebConst.Attr.FLASH, "The quest has been deleted.");
                log.info("Delete quest success questId={} by actorId={} questName='{}'", questId, actor.getUserId(), questName);
                if (isAdmin && ownerLogin != null && notifications != null && users != null) {
                    Optional<User> ownerOpt = users.findByLogin(ownerLogin);
                    if (ownerOpt.isPresent()) {
                        String ownerUserId = ownerOpt.get().getUserId();
                        if (!ownerUserId.equals(actor.getUserId())) {
                            String shortTitle = Web.shortTitle(questName);
                            notifications.add(
                                    ownerUserId,
                                    NotificationType.QUEST_ADMIN_CHANGED,
                                    "The administrator deleted your quest.\n",
                                    "Quest <b>" + shortTitle + "</b> was deleted by the administrator."
                            );
                            log.info("Owner notified about deletion ownerId={} questId={}", ownerUserId, questId);
                        }
                    } else {
                        log.warn("Owner login not found for questId={} ownerLogin='{}'", questId, ownerLogin);
                    }
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
     * Rejects GET requests with 405, as deletion must be performed via POST.
     *
     * @param req  HTTP request
     * @param resp HTTP response used to send the error
     * @throws IOException if sending the error fails
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        log.warn("Delete quest GET not allowed");
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }
}
