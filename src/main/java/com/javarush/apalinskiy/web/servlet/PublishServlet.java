package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.application.quests.QuestAuthoringService;
import com.javarush.apalinskiy.domain.user.Role;
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
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PublishServlet extends HttpServlet {

    private QuestAuthoringService authoring;
    private NotificationService notify;
    private UserService users;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            ServletContext ctx = config.getServletContext();
            this.authoring = Web.ctxBean(ctx, WebConst.Ctx.AUTHORING_SERVICE, QuestAuthoringService.class);
            this.notify = Web.ctxBean(ctx, WebConst.Ctx.NOTIFY_SERVICE, NotificationService.class);
            this.users = Web.ctxBean(ctx, WebConst.Ctx.USER_SERVICE, UserService.class);
        } catch (IllegalStateException e) {
            throw new UnavailableException("Required services not found");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        List<String> errs = authoring.validateCurrentDraft();
        if (!errs.isEmpty()) {
            String msg = "You can't go to the publication: " + String.join(" ", errs);
            Web.redirectErr(req, resp, WebConst.Path.GRAPH_SVG, msg);
            return;
        }
        Web.copyParamsToAttrs(req, WebConst.Attr.ERROR, WebConst.Attr.OK);
        Web.forward(req, resp, WebConst.Jsp.PUBLISH);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = (User) req.getSession().getAttribute(WebConst.Attr.USER);
        String owner = (user != null) ? user.getUserLogin() : "anonymous";
        boolean isAdmin = (user != null && user.getRole() == Role.ADMIN);
        HttpSession s = req.getSession(false);
        String editingId = (s != null) ? (String) s.getAttribute(WebConst.Attr.EDITING_QUEST_ID) : null;
        try {
            if (editingId != null && !editingId.isBlank()) {
                Optional<CustomQuest> cqOpt = authoring.getFromCatalog(editingId);
                String questName = cqOpt.map(CustomQuest::getName).orElse("Quest");
                String ownerLogin = cqOpt.map(CustomQuest::getOwnerLogin).orElse(null);
                authoring.updateExisting(editingId, isAdmin);
                if (isAdmin && ownerLogin != null && !ownerLogin.equals(owner)) {
                    String targetUserId = users.findByLogin(ownerLogin).map(User::getUserId).orElse(null);
                    if (targetUserId != null) {
                        Map<String, String> data = Map.of(
                                "questName", questName,
                                "what", "Admin updated your quest"
                        );
                        notify.notify(NotificationEvent.of(
                                NotificationType.QUEST_ADMIN_CHANGED,
                                user.getUserId(),
                                targetUserId,
                                data
                        ));
                    }
                }
                Web.redirectOk(req, resp, WebConst.Path.HOME,
                        isAdmin ? "Changes saved" : "Changes submitted for moderation");
                return;
            }
            String questNameRaw = req.getParameter("questName");
            String questName = questNameRaw == null ? "" : questNameRaw.trim();
            if (questName.isBlank()) {
                Web.redirect(req, resp, WebConst.Path.PUBLISH,
                        Map.of(WebConst.Attr.ERROR, "Specify the name of the quest",
                                "questName", String.valueOf(questNameRaw)));
                return;
            }
            if (questName.length() > 100) {
                Web.redirect(req, resp, WebConst.Path.PUBLISH,
                        Map.of(WebConst.Attr.ERROR, "The name is too long (maximum 100 characters)",
                                "questName", questName));
                return;
            }
            if (isAdmin) {
                authoring.publish(owner, questName);
                Web.redirectOk(req, resp, WebConst.Path.HOME, "The quest has been published");
            } else {
                authoring.submitNewForModeration(owner, questName);
                Web.redirectOk(req, resp, WebConst.Path.HOME, "The quest has been submitted for moderation");
            }
        } catch (IllegalArgumentException | IllegalStateException ex) {
            String msg = "Publication failed: " + ex.getMessage();
            Web.redirect(req, resp, WebConst.Path.PUBLISH,
                    Map.of(WebConst.Attr.ERROR, msg,
                            "questName", String.valueOf(req.getParameter("questName"))));
        }
    }
}
