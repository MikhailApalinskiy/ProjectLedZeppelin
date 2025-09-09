package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.application.quests.QuestAuthoringService;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.mail.NotificationService;
import com.javarush.apalinskiy.mail.NotificationType;
import com.javarush.apalinskiy.quest.CustomQuest;
import com.javarush.apalinskiy.service.UserService;
import com.javarush.apalinskiy.web.util.Web;
import com.javarush.apalinskiy.web.util.WebConst;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Optional;


public class DeleteQuestServlet extends HttpServlet {

    private transient QuestAuthoringService authoring;
    private transient NotificationService notifications;
    private transient UserService users;

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
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User actor = (User) req.getSession().getAttribute(WebConst.Attr.USER);
        if (actor == null) {
            Web.redirect(req, resp, WebConst.Path.LOGIN, java.util.Map.of());
            return;
        }
        String questId = Web.trimOrNull(req.getParameter(WebConst.Param.ID));
        String next = Web.safeNextOrHome(req, req.getParameter(WebConst.Param.NEXT));
        if (next == null) {
            next = req.getContextPath() + WebConst.Path.MY_QUESTS;
        }
        if (questId == null) {
            req.getSession().setAttribute(WebConst.Attr.ERROR, "The quest ID is not specified.");
            resp.sendRedirect(resp.encodeRedirectURL(next));
            return;
        }
        Optional<CustomQuest> questOpt = authoring.getFromCatalog(questId);
        String questName = questOpt.map(CustomQuest::getName).orElse("Quest");
        String ownerLogin = questOpt.map(CustomQuest::getOwnerLogin).orElse(null);
        try {
            boolean isAdmin = "ADMIN".equals(String.valueOf(actor.getRole()));
            boolean removed = isAdmin
                    ? authoring.deleteFromCatalogAsAdmin(questId)
                    : authoring.deleteFromCatalogIfOwner(questId, actor.getUserLogin());
            if (removed) {
                req.getSession().setAttribute(WebConst.Attr.FLASH, "The quest has been deleted.");
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
                        }
                    }
                }
            } else {
                String msg = isAdmin
                        ? "Cannot be deleted: not found."
                        : "Cannot be deleted: not found or you are not the owner.";
                req.getSession().setAttribute(WebConst.Attr.ERROR, msg);
            }
        } catch (IllegalArgumentException | IllegalStateException ex) {
            req.getSession().setAttribute(WebConst.Attr.ERROR, "Deletion error: " + ex.getMessage());
        }
        resp.sendRedirect(resp.encodeRedirectURL(next));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }
}
