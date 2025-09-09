package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.application.ports.CustomQuestRepository;
import com.javarush.apalinskiy.application.quests.QuestAuthoringService;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.mail.NotificationEvent;
import com.javarush.apalinskiy.mail.NotificationService;
import com.javarush.apalinskiy.mail.NotificationType;
import com.javarush.apalinskiy.quest.CustomQuest;
import com.javarush.apalinskiy.service.UserService;
import com.javarush.apalinskiy.web.util.Web;
import com.javarush.apalinskiy.web.util.WebConst;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

public class AdminQuestsModerationServlet extends HttpServlet {

    private QuestAuthoringService authoring;
    private NotificationService notify;
    private UserService users;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        ServletContext ctx = config.getServletContext();
        try {
            this.authoring = Web.ctxBean(ctx, WebConst.Ctx.AUTHORING_SERVICE, QuestAuthoringService.class);
            this.notify = Web.ctxBean(ctx, WebConst.Ctx.NOTIFY_SERVICE, NotificationService.class);
            this.users = Web.ctxBean(ctx, WebConst.Ctx.USER_SERVICE, UserService.class);
        } catch (IllegalStateException e) {
            throw new UnavailableException(e.getMessage());
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Web.pullFlash(req, WebConst.Attr.FLASH);
        req.setAttribute("pendingNew", authoring.listPendingNew());
        req.setAttribute("pendingEdit", authoring.listPendingEdits());
        Web.forward(req, resp, WebConst.Jsp.QUESTS_MOD);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        final String action = Web.trimOrNull(req.getParameter("action"));
        final String id = Web.trimOrNull(req.getParameter("id"));
        if (action == null || id == null) {
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
                    notifyModeration(actorId, ownerLogin, questName, "approved", newId);
                    req.getSession().setAttribute(WebConst.Attr.FLASH,
                            "The quest has been published (id=" + newId + ").");
                }
                case "rejectCreate" -> {
                    CustomQuestRepository.PendingNew pn = findPendingNew(id);
                    String questName = (pn != null && pn.getName() != null) ? pn.getName() : "Quest";
                    String ownerLogin = (pn != null) ? pn.getOwnerLogin() : null;
                    authoring.rejectCreate(id);
                    notifyModeration(actorId, ownerLogin, questName, "rejected", null);
                    req.getSession().setAttribute(WebConst.Attr.FLASH, "New publication rejected.");
                }
                case "approveEdit" -> {
                    CustomQuestRepository.PendingEdit pe = findPendingEdit(id);
                    String questName = safeQuestNameForEdit(id, pe);
                    String ownerLogin = safeOwnerLoginForEdit(id, pe);
                    authoring.approveEdit(id);
                    notifyModeration(actorId, ownerLogin, questName, "approved", null);
                    req.getSession().setAttribute(WebConst.Attr.FLASH, "Edits approved and applied.");
                }
                case "rejectEdit" -> {
                    CustomQuestRepository.PendingEdit pe = findPendingEdit(id);
                    String questName = (pe != null && pe.getName() != null) ? pe.getName() : "Quest";
                    String ownerLogin = (pe != null) ? pe.getOwnerLogin() : null;
                    authoring.rejectEdit(id);
                    notifyModeration(actorId, ownerLogin, questName, "edit-rejected", null);
                    req.getSession().setAttribute(WebConst.Attr.FLASH, "The edits were rejected.");
                }
                default -> {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown action");
                    return;
                }
            }
            resp.sendRedirect(resp.encodeRedirectURL(req.getContextPath() + WebConst.Path.QUESTS_MOD));
        } catch (RuntimeException ex) {
            Web.redirect(req, resp, WebConst.Path.QUESTS_MOD,
                    Map.of(WebConst.Attr.ERROR, ex.getMessage() == null ? "Operation failed" : ex.getMessage()));
        }
    }

    private String resolveUserIdByLogin(String login) {
        if (login == null || login.isBlank()) {
            return null;
        }
        return users.findByLogin(login).map(User::getUserId).orElse(null);
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
        if (pe != null && pe.getName() != null) {
            return pe.getName();
        }
        return authoring.getFromCatalog(questId).map(CustomQuest::getName).orElse("Quest");
    }

    private String safeOwnerLoginForEdit(String questId, CustomQuestRepository.PendingEdit pe) {
        if (pe != null && pe.getOwnerLogin() != null) {
            return pe.getOwnerLogin();
        }
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
        Map<String, String> data = new java.util.HashMap<>();
        data.put("questName", questName);
        data.put("result", result);
        if (questIdOpt != null) {
            data.put("questId", questIdOpt);
        }
        notify.notify(NotificationEvent.of(
                NotificationType.QUEST_MODERATED, actorId, targetUserId, data));
    }
}
