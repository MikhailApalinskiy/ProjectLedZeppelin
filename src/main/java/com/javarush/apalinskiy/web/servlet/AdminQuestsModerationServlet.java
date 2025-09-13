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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin-only servlet for moderating user-submitted custom quests.
 * <p>
 * Displays pending submissions (new quests and edits) and handles approve/reject actions.
 * Authentication/authorization is expected to be enforced upstream (e.g., via a filter).
 * </p>
 *
 * <h3>GET</h3>
 * <ul>
 *   <li>Loads pending items via {@code authoring.listPendingNew()} and {@code authoring.listPendingEdits()}.</li>
 *   <li>Sets request attributes:
 *     <ul>
 *       <li>{@code "pendingNew"} — list of {@code CustomQuestRepository.PendingNew}</li>
 *       <li>{@code "pendingEdit"} — list of {@code CustomQuestRepository.PendingEdit}</li>
 *     </ul>
 *   </li>
 *   <li>Pulls flash message ({@code WebConst.Attr.FLASH}) and forwards to {@code WebConst.Jsp.QUESTS_MOD}.</li>
 * </ul>
 *
 * <h3>POST</h3>
 * <p>Consumes parameters:</p>
 * <ul>
 *   <li>{@code action} — one of:
 *     <ul>
 *       <li>{@code approveCreate} — publish a newly submitted quest;</li>
 *       <li>{@code rejectCreate} — decline a newly submitted quest;</li>
 *       <li>{@code approveEdit} — accept and apply pending edits for an existing quest;</li>
 *       <li>{@code rejectEdit} — decline pending edits.</li>
 *     </ul>
 *   </li>
 *   <li>{@code id} — identifier of the item being moderated:
 *     <ul>
 *       <li>For create actions: {@code pendingId}.</li>
 *       <li>For edit actions: {@code questId}.</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h3>Side effects</h3>
 * <ul>
 *   <li>Invokes corresponding methods on {@code authoring} (approve/reject).</li>
 *   <li>Sends notifications to the quest owner:
 *     {@code NotificationType.QUEST_MODERATED} with data: {@code questName}, {@code result}, and optional {@code questId}.</li>
 *   <li>On successful create approval:
 *     increments author's "created" counter and notifies friends about publication.</li>
 *   <li>Sets a flash message describing the outcome and redirects back to the moderation list.</li>
 * </ul>
 *
 * <h3>Error handling</h3>
 * <ul>
 *   <li>400 — missing or unknown parameters.</li>
 *   <li>On runtime errors, logs the failure and redirects with {@code WebConst.Attr.ERROR} message.</li>
 * </ul>
 *
 * @see BaseQuestAdminServlet
 * @see CustomQuestRepository.PendingNew
 * @see CustomQuestRepository.PendingEdit
 * @see NotificationType
 * @see WebConst
 * @see Web
 */
public class AdminQuestsModerationServlet extends BaseQuestAdminServlet {

    private static final Logger log = LoggerFactory.getLogger(AdminQuestsModerationServlet.class);

    /**
     * Renders the moderation list page with pending "new" and "edit" items.
     * <ul>
     *   <li>Attributes: {@code "pendingNew"}, {@code "pendingEdit"}.</li>
     *   <li>View: {@code WebConst.Jsp.QUESTS_MOD}.</li>
     * </ul>
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Web.pullFlash(req, WebConst.Attr.FLASH);
        List<CustomQuestRepository.PendingNew> newList = authoring.listPendingNew();
        List<CustomQuestRepository.PendingEdit> editList = authoring.listPendingEdits();
        req.setAttribute("pendingNew", authoring.listPendingNew());
        req.setAttribute("pendingEdit", authoring.listPendingEdits());
        log.info("Moderation list opened pendingNew={} pendingEdit={}", newList.size(), editList.size());
        Web.forward(req, resp, WebConst.Jsp.QUESTS_MOD);
    }

    /**
     * Executes a moderation action for the given item.
     * <p>
     * Required params: {@code action}, {@code id}. On success, redirects to {@code WebConst.Path.QUESTS_MOD}.
     * </p>
     * <p>
     * Flash messages:
     * <ul>
     *   <li>approveCreate — {@code "The quest has been published (id=<newId>)."};</li>
     *   <li>rejectCreate — {@code "New publication rejected."};</li>
     *   <li>approveEdit — {@code "Edits approved and applied."};</li>
     *   <li>rejectEdit — {@code "The edits were rejected."}.</li>
     * </ul>
     * </p>
     * <p>
     * On {@link RuntimeException}, redirects with {@code WebConst.Attr.ERROR}.
     * </p>
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
                    String ownerLogin = (pn != null) ? pn.getOwnerLogin() : null;
                    String newId = authoring.approveCreate(id);
                    incCreatedByLogin(ownerLogin);
                    notifyModeration(actorId, ownerLogin, questName, "approved", newId);
                    notifyFriendsPublishedByLogin(ownerLogin, questName);
                    req.getSession().setAttribute(WebConst.Attr.FLASH,
                            "The quest has been published (id=" + newId + ").");
                    log.info("approveCreate done actorId={} owner={} name='{}' newId={}",
                            actorId, ownerLogin, questName, newId);
                }
                case "rejectCreate" -> {
                    CustomQuestRepository.PendingNew pn = findPendingNew(id);
                    String questName = (pn != null && pn.getName() != null) ? pn.getName() : "Quest";
                    String ownerLogin = (pn != null) ? pn.getOwnerLogin() : null;
                    authoring.rejectCreate(id);
                    notifyModeration(actorId, ownerLogin, questName, "rejected", null);
                    req.getSession().setAttribute(WebConst.Attr.FLASH, "New publication rejected.");
                    log.info("rejectCreate done actorId={} owner={} name='{}'",
                            actorId, ownerLogin, questName);
                }
                case "approveEdit" -> {
                    CustomQuestRepository.PendingEdit pe = findPendingEdit(id);
                    String questName = safeQuestNameForEdit(id, pe);
                    String ownerLogin = safeOwnerLoginForEdit(id, pe);
                    authoring.approveEdit(id);
                    notifyModeration(actorId, ownerLogin, questName, "approved", null);
                    req.getSession().setAttribute(WebConst.Attr.FLASH, "Edits approved and applied.");
                    log.info("approveEdit done actorId={} owner={} name='{}' questId={}",
                            actorId, ownerLogin, questName, id);
                }
                case "rejectEdit" -> {
                    CustomQuestRepository.PendingEdit pe = findPendingEdit(id);
                    String questName = (pe != null && pe.getName() != null) ? pe.getName() : "Quest";
                    String ownerLogin = (pe != null) ? pe.getOwnerLogin() : null;
                    authoring.rejectEdit(id);
                    notifyModeration(actorId, ownerLogin, questName, "edit-rejected", null);
                    req.getSession().setAttribute(WebConst.Attr.FLASH, "The edits were rejected.");
                    log.info("rejectEdit done actorId={} owner={} name='{}' questId={}",
                            actorId, ownerLogin, questName, id);
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
     * Finds a pending "new quest" item by its {@code pendingId}.
     *
     * @param pendingId identifier from the moderation UI
     * @return the pending item or {@code null} if not found
     */
    private CustomQuestRepository.PendingNew findPendingNew(String pendingId) {
        return authoring.listPendingNew().stream()
                .filter(x -> pendingId.equals(x.getPendingId()))
                .findFirst().orElse(null);
    }

    /**
     * Finds a pending "edit quest" item by its {@code questId}.
     *
     * @param questId quest identifier
     * @return the pending edit or {@code null} if not found
     */
    private CustomQuestRepository.PendingEdit findPendingEdit(String questId) {
        return authoring.listPendingEdits().stream()
                .filter(x -> questId.equals(x.getQuestId()))
                .findFirst().orElse(null);
    }

    /**
     * Returns a safe quest name for logging/notifications when moderating edits.
     * Prefers the name from the pending edit; falls back to the catalog entry; defaults to {@code "Quest"}.
     */
    private String safeQuestNameForEdit(String questId, CustomQuestRepository.PendingEdit pe) {
        if (pe != null && pe.getName() != null) return pe.getName();
        return authoring.getFromCatalog(questId).map(CustomQuest::getName).orElse("Quest");
    }

    /**
     * Returns a safe owner login for logging/notifications when moderating edits.
     * Prefers the login from the pending edit; falls back to the catalog entry; may return {@code null}.
     */
    private String safeOwnerLoginForEdit(String questId, CustomQuestRepository.PendingEdit pe) {
        if (pe != null && pe.getOwnerLogin() != null) return pe.getOwnerLogin();
        return authoring.getFromCatalog(questId).map(CustomQuest::getOwnerLogin).orElse(null);
    }

    /**
     * Sends a moderation result notification to the quest owner (if resolvable).
     * <p>
     * Data payload:
     * <ul>
     *   <li>{@code questName} — display name used in the message;</li>
     *   <li>{@code result} — e.g., {@code "approved"}, {@code "rejected"}, {@code "edit-rejected"};</li>
     *   <li>{@code questId} — optional; included for "approveCreate".</li>
     * </ul>
     */
    private void notifyModeration(String actorId,
                                  String ownerLogin,
                                  String questName,
                                  String result,
                                  String questIdOpt) {
        String targetUserId = resolveUserIdByLogin(ownerLogin);
        if (targetUserId == null) {
            return;
        }
        Map<String, String> data = new HashMap<>();
        data.put("questName", questName);
        data.put("result", result);
        if (questIdOpt != null) {
            data.put("questId", questIdOpt);
        }
        notify.notify(NotificationEvent.of(
                NotificationType.QUEST_MODERATED, actorId, targetUserId, data));
    }
}
