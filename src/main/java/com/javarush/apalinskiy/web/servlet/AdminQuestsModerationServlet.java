package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.repository.quest.CustomQuestRepository;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.domain.notify.NotificationEvent;
import com.javarush.apalinskiy.domain.notify.NotificationType;
import com.javarush.apalinskiy.domain.quest.custom.CustomQuest;
import com.javarush.apalinskiy.web.util.Web;
import com.javarush.apalinskiy.app.WebConst;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * Admin servlet responsible for moderating user-submitted Custom Quests.
 * <p>
 * <b>GET</b> shows a list of pending publications and edits.<br>
 * <b>POST</b> applies a moderation action to a specific item:
 * <ul>
 *   <li><code>approveCreate</code> — publish a new quest</li>
 *   <li><code>rejectCreate</code> — reject a new quest</li>
 *   <li><code>approveEdit</code> — apply a pending edit to an existing quest</li>
 *   <li><code>rejectEdit</code> — reject a pending edit</li>
 * </ul>
 * Side effects include updating repositories, incrementing per-user publication counters,
 * sending notifications to authors and their friends, storing flash messages in session,
 * and redirecting back to the moderation page.
 * <p>
 * <b>Security:</b> This servlet assumes access is guarded by an admin-only filter/interceptor.
 * It reads the current user from the HTTP session to attribute the moderation action.
 *
 * @author Your Name
 * @since 1.0
 */
public class AdminQuestsModerationServlet extends BaseQuestAdminServlet {

    private static final Logger log = LoggerFactory.getLogger(AdminQuestsModerationServlet.class);

    /**
     * Renders the moderation dashboard with two lists: pending new publications and pending edits.
     * The method also resolves quest owners for display and injects:
     * <ul>
     *   <li><code>pendingNew</code> — {@code List<CustomQuestRepository.PendingNew>}</li>
     *   <li><code>pendingEdit</code> — {@code List<CustomQuestRepository.PendingEdit>}</li>
     *   <li><code>ownerById</code> — {@code Map<String, User>} for quick owner lookup</li>
     * </ul>
     * A previously stored flash message (if any) is pulled from the request/session scope.
     * Finally, the request is forwarded to {@link WebConst.Jsp#QUESTS_MOD}.
     *
     * @param req  current HTTP request
     * @param resp current HTTP response
     * @throws ServletException if the JSP forward fails
     * @throws IOException      if forwarding the request/response fails
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Web.pullFlash(req, WebConst.Attr.FLASH);
        List<CustomQuestRepository.PendingNew> newList = authoring.listPendingNew();
        List<CustomQuestRepository.PendingEdit> editList = authoring.listPendingEdits();
        Set<String> ownerIds = new HashSet<>();
        for (var pn : newList) {
            if (pn != null && pn.getOwnerId() != null && !pn.getOwnerId().isBlank()) {
                ownerIds.add(pn.getOwnerId());
            }
        }
        for (var pe : editList) {
            if (pe != null && pe.getOwnerId() != null && !pe.getOwnerId().isBlank()) {
                ownerIds.add(pe.getOwnerId());
            }
        }
        Map<String, User> ownerById = new HashMap<>();
        for (String id : ownerIds) {
            try {
                users.findById(id).ifPresent(u -> ownerById.put(id, u));
            } catch (Exception e) {
                log.warn("Owner resolve failed ownerId={}", id, e);
            }
        }
        req.setAttribute("pendingNew", newList);
        req.setAttribute("pendingEdit", editList);
        req.setAttribute("ownerById", ownerById);
        log.info("Moderation list opened pendingNew={} pendingEdit={}", newList.size(), editList.size());
        Web.forward(req, resp, WebConst.Jsp.QUESTS_MOD);
    }

    /**
     * Applies a moderation action to a pending quest publication or edit.
     * <p>
     * Required request parameters:
     * <ul>
     *   <li><code>action</code> — one of <em>approveCreate</em>, <em>rejectCreate</em>,
     *       <em>approveEdit</em>, <em>rejectEdit</em></li>
     *   <li><code>id</code> — pending item identifier (for create) or quest id (for edit)</li>
     * </ul>
     * Behavior:
     * <ul>
     *   <li>Validates parameters; on missing/unknown action responds with HTTP 400.</li>
     *   <li>Resolves acting admin from the session for auditing/notifications.</li>
     *   <li>Executes the requested operation via {@code authoring} service.</li>
     *   <li>Sends user notifications (author & friends) when applicable.</li>
     *   <li>Stores a flash message and redirects back to the moderation page.</li>
     *   <li>On runtime errors, logs the exception and redirects with an error message.</li>
     * </ul>
     *
     * @param req  current HTTP request containing the action and id
     * @param resp current HTTP response; used for 400 errors and redirects
     * @throws IOException if sending an error or redirect fails
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        final String action = Web.trimOrNull(req.getParameter("action"));
        final String id = Web.trimOrNull(req.getParameter("id"));
        if (action == null || id == null) {
            log.warn("Moderation POST missing params action={} id={}", action, id);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing parameters");
            return;
        }
        User admin = (User) req.getSession().getAttribute(WebConst.Attr.USER);
        final String actorId = (admin == null) ? null : admin.getUserId();
        try {
            switch (action) {
                case "approveCreate" -> {
                    CustomQuestRepository.PendingNew pn = findPendingNew(id);
                    String questName = (pn != null && pn.getName() != null) ? pn.getName() : "Quest";
                    String ownerId = (pn != null) ? pn.getOwnerId() : null;
                    String newId = authoring.approveCreate(id);
                    incCreatedByUserId(ownerId);
                    notifyModerationById(actorId, ownerId, questName, "approved", newId);
                    notifyFriendsPublishedByUserId(ownerId, questName);
                    req.getSession().setAttribute(WebConst.Attr.FLASH,
                            "The quest has been published (id=" + newId + ").");
                    log.info("approveCreate done actorId={} ownerId={} name='{}' newId={}",
                            actorId, ownerId, questName, newId);
                }
                case "rejectCreate" -> {
                    CustomQuestRepository.PendingNew pn = findPendingNew(id);
                    String questName = (pn != null && pn.getName() != null) ? pn.getName() : "Quest";
                    String ownerId = (pn != null) ? pn.getOwnerId() : null;
                    authoring.rejectCreate(id);
                    notifyModerationById(actorId, ownerId, questName, "rejected", null);
                    req.getSession().setAttribute(WebConst.Attr.FLASH, "New publication rejected.");
                    log.info("rejectCreate done actorId={} ownerId={} name='{}'",
                            actorId, ownerId, questName);
                }
                case "approveEdit" -> {
                    CustomQuestRepository.PendingEdit pe = findPendingEdit(id);
                    String questName = safeQuestNameForEdit(id, pe);
                    String ownerId = safeOwnerIdForEdit(id, pe);
                    authoring.approveEdit(id);
                    notifyModerationById(actorId, ownerId, questName, "approved", null);
                    req.getSession().setAttribute(WebConst.Attr.FLASH, "Edits approved and applied.");
                    log.info("approveEdit done actorId={} ownerId={} name='{}' questId={}",
                            actorId, ownerId, questName, id);
                }
                case "rejectEdit" -> {
                    CustomQuestRepository.PendingEdit pe = findPendingEdit(id);
                    String questName = (pe != null && pe.getName() != null) ? pe.getName() : "Quest";
                    String ownerId = (pe != null) ? pe.getOwnerId() : null;
                    authoring.rejectEdit(id);
                    notifyModerationById(actorId, ownerId, questName, "edit-rejected", null);
                    req.getSession().setAttribute(WebConst.Attr.FLASH, "The edits were rejected.");
                    log.info("rejectEdit done actorId={} ownerId={} name='{}' questId={}",
                            actorId, ownerId, questName, id);
                }
                default -> {
                    log.warn("Moderation POST unknown action action={} id={}", action, id);
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown action");
                    return;
                }
            }
            resp.sendRedirect(resp.encodeRedirectURL(req.getContextPath() + WebConst.Path.QUESTS_MOD));
        } catch (RuntimeException ex) {
            log.error("Moderation action failed action={} id={} actorId={}", action, id, actorId, ex);
            Web.redirect(req, resp, WebConst.Path.QUESTS_MOD,
                    Map.of(WebConst.Attr.ERROR, ex.getMessage() == null ? "Operation failed" : ex.getMessage()));
        }
    }

    /**
     * Finds a {@link CustomQuestRepository.PendingNew} item by its pending id.
     *
     * @param pendingId identifier of the pending new quest publication
     * @return the matching pending item, or {@code null} if not found
     */
    private CustomQuestRepository.PendingNew findPendingNew(String pendingId) {
        return authoring.listPendingNew().stream()
                .filter(x -> pendingId.equals(x.getPendingId()))
                .findFirst().orElse(null);
    }

    /**
     * Finds a {@link CustomQuestRepository.PendingEdit} item by quest id.
     *
     * @param questId identifier of the quest being edited
     * @return the matching pending edit, or {@code null} if not found
     */
    private CustomQuestRepository.PendingEdit findPendingEdit(String questId) {
        return authoring.listPendingEdits().stream()
                .filter(x -> questId.equals(x.getQuestId()))
                .findFirst().orElse(null);
    }

    /**
     * Resolves a human-friendly quest name for an edit context.
     * Prefers the pending edit's name; falls back to the catalog entry; defaults to {@code "Quest"}.
     *
     * @param questId quest identifier
     * @param pe      pending edit (may be {@code null})
     * @return a non-blank quest name suitable for UI and notifications
     */
    private String safeQuestNameForEdit(String questId, CustomQuestRepository.PendingEdit pe) {
        if (pe != null && pe.getName() != null) return pe.getName();
        return authoring.getFromCatalog(questId).map(CustomQuest::getName).orElse("Quest");
    }

    /**
     * Resolves the owner id for an edit context.
     * Prefers the pending edit's owner id; falls back to the catalog entry; may return {@code null}.
     *
     * @param questId quest identifier
     * @param pe      pending edit (may be {@code null})
     * @return owner user id or {@code null} if unknown
     */
    private String safeOwnerIdForEdit(String questId, CustomQuestRepository.PendingEdit pe) {
        if (pe != null && pe.getOwnerId() != null) return pe.getOwnerId();
        return authoring.getFromCatalog(questId).map(CustomQuest::getOwnerId).orElse(null);
    }

    /**
     * Sends a {@link NotificationType#QUEST_MODERATED} event to the quest owner with a small payload.
     * The payload includes:
     * <ul>
     *   <li><code>questName</code> — sanitized quest title</li>
     *   <li><code>result</code> — moderation result keyword (e.g., {@code approved}, {@code rejected}, {@code edit-rejected})</li>
     *   <li><code>questId</code> — optional quest id (present when a new quest is created)</li>
     * </ul>
     * If the notification service is not configured or the target user id is blank, the call is logged and skipped.
     *
     * @param actorId    user id of the admin performing the action (may be {@code null})
     * @param targetUserId recipient user id (quest owner); must be non-blank to send
     * @param questName  human-friendly quest name; sanitized to a default when blank
     * @param result     result keyword (e.g., {@code approved}, {@code rejected}, {@code edit-rejected})
     * @param questIdOpt optional quest id to include for linking newly published quests
     */
    private void notifyModerationById(String actorId,
                                      String targetUserId,
                                      String questName,
                                      String result,
                                      String questIdOpt) {
        if (notify == null || targetUserId == null || targetUserId.isBlank()) {
            log.warn("notifyModerationById skipped: notifyPresent={} targetUserId='{}'",
                    notify != null, targetUserId);
            return;
        }
        Map<String, String> data = new HashMap<>();
        data.put("questName", (questName == null || questName.isBlank()) ? "Quest" : questName);
        data.put("result", result == null ? "" : result);
        if (questIdOpt != null) data.put("questId", questIdOpt);
        notify.notify(NotificationEvent.of(
                NotificationType.QUEST_MODERATED,
                actorId,
                targetUserId,
                data
        ));
    }
}
