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
 * Administrative servlet for moderating user-submitted quests.
 *
 * <p>This servlet allows administrators to review, approve, or reject quests and their edits.
 * It supports moderation workflows for both newly created quests and edited versions of
 * previously published quests.</p>
 *
 * <p>All moderation actions trigger notifications to the quest authors and optionally their
 * friends. The servlet relies on the {@link CustomQuestRepository} and {@link com.javarush.apalinskiy.service.quest.QuestAuthoringService}
 * for accessing pending quests and applying moderation results.</p>
 *
 * <p>Actions supported via POST requests:</p>
 * <ul>
 *     <li><b>approveCreate</b> — approve a new quest submission</li>
 *     <li><b>rejectCreate</b> — reject a new quest submission</li>
 *     <li><b>approveEdit</b> — approve an edit submission</li>
 *     <li><b>rejectEdit</b> — reject an edit submission</li>
 * </ul>
 */
public class AdminQuestsModerationServlet extends BaseQuestAdminServlet {

    private static final Logger log = LoggerFactory.getLogger(AdminQuestsModerationServlet.class);

    /**
     * Handles GET requests by displaying all pending quests and edits awaiting moderation.
     *
     * <p>The servlet retrieves both pending new and pending edit submissions, resolves their
     * corresponding owners, and forwards the data to the moderation JSP page for display.</p>
     *
     * @param req  current HTTP request
     * @param resp current HTTP response
     * @throws ServletException if forwarding fails
     * @throws IOException      if an I/O error occurs
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
     * Handles moderation actions sent via POST.
     *
     * <p>Supported actions:
     * <ul>
     *     <li>{@code approveCreate}</li>
     *     <li>{@code rejectCreate}</li>
     *     <li>{@code approveEdit}</li>
     *     <li>{@code rejectEdit}</li>
     * </ul></p>
     *
     * <p>Each action updates the quest’s moderation state, sends notifications to authors,
     * and redirects back to the moderation list page with a flash message.</p>
     *
     * @param req  HTTP request
     * @param resp HTTP response
     * @throws IOException if redirect or response writing fails
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
     * Finds a pending new quest submission by ID.
     */
    private CustomQuestRepository.PendingNew findPendingNew(String pendingId) {
        return authoring.listPendingNew().stream()
                .filter(x -> pendingId.equals(x.getPendingId()))
                .findFirst().orElse(null);
    }

    /**
     * Finds a pending quest edit submission by quest ID.
     */
    private CustomQuestRepository.PendingEdit findPendingEdit(String questId) {
        return authoring.listPendingEdits().stream()
                .filter(x -> questId.equals(x.getQuestId()))
                .findFirst().orElse(null);
    }

    /**
     * Returns a safe quest name for moderation logs and notifications.
     */
    private String safeQuestNameForEdit(String questId, CustomQuestRepository.PendingEdit pe) {
        if (pe != null && pe.getName() != null) return pe.getName();
        return authoring.getFromCatalog(questId).map(CustomQuest::getName).orElse("Quest");
    }

    /**
     * Returns a safe owner ID for moderation notifications.
     */
    private String safeOwnerIdForEdit(String questId, CustomQuestRepository.PendingEdit pe) {
        if (pe != null && pe.getOwnerId() != null) return pe.getOwnerId();
        return authoring.getFromCatalog(questId).map(CustomQuest::getOwnerId).orElse(null);
    }

    /**
     * Sends a notification to the quest owner about a moderation result.
     *
     * @param actorId      moderator user ID (can be null)
     * @param targetUserId quest owner ID
     * @param questName    quest title
     * @param result       moderation result string (e.g., "approved", "rejected")
     * @param questIdOpt   optional quest ID for approved quests
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
