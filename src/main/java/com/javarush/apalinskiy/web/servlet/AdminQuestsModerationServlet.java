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

public class AdminQuestsModerationServlet extends BaseQuestAdminServlet {

    private static final Logger log = LoggerFactory.getLogger(AdminQuestsModerationServlet.class);

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

    private CustomQuestRepository.PendingNew findPendingNew(String pendingId) {
        return authoring.listPendingNew().stream()
                .filter(x -> pendingId.equals(x.getPendingId()))
                .findFirst().orElse(null);
    }

    private CustomQuestRepository.PendingEdit findPendingEdit(String questId) {
        return authoring.listPendingEdits().stream()
                .filter(x -> questId.equals(x.getQuestId()))
                .findFirst().orElse(null);
    }

    private String safeQuestNameForEdit(String questId, CustomQuestRepository.PendingEdit pe) {
        if (pe != null && pe.getName() != null) return pe.getName();
        return authoring.getFromCatalog(questId).map(CustomQuest::getName).orElse("Quest");
    }

    private String safeOwnerLoginForEdit(String questId, CustomQuestRepository.PendingEdit pe) {
        if (pe != null && pe.getOwnerLogin() != null) return pe.getOwnerLogin();
        return authoring.getFromCatalog(questId).map(CustomQuest::getOwnerLogin).orElse(null);
    }

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
