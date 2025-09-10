package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.service.quest.QuestAuthoringService;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.service.social.FriendService;
import com.javarush.apalinskiy.domain.notify.NotificationEvent;
import com.javarush.apalinskiy.service.notify.NotificationService;
import com.javarush.apalinskiy.domain.notify.NotificationType;
import com.javarush.apalinskiy.service.user.UserService;
import com.javarush.apalinskiy.service.user.UserStatsService;
import com.javarush.apalinskiy.web.util.Web;
import com.javarush.apalinskiy.app.WebConst;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.http.HttpServlet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BaseQuestAdminServlet extends HttpServlet {

    protected transient QuestAuthoringService authoring;
    protected transient NotificationService notify;
    protected transient UserService users;
    protected transient UserStatsService userStats;
    protected transient FriendService friendService;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        ServletContext ctx = config.getServletContext();
        try {
            this.authoring = Web.ctxBean(ctx, WebConst.Ctx.AUTHORING_SERVICE, QuestAuthoringService.class);
            this.notify = Web.ctxBean(ctx, WebConst.Ctx.NOTIFY_SERVICE, NotificationService.class);
            this.users = Web.ctxBean(ctx, WebConst.Ctx.USER_SERVICE, UserService.class);
            try {
                this.userStats = Web.ctxBean(ctx, WebConst.Ctx.USER_STATS_SERVICE, UserStatsService.class);
            } catch (IllegalStateException ignore) {
                this.userStats = null;
            }
            try {
                this.friendService = Web.ctxBean(ctx, WebConst.Ctx.FRIEND_SERVICE, FriendService.class);
            } catch (IllegalStateException ignore) {
                this.friendService = null;
            }
        } catch (IllegalStateException e) {
            throw new UnavailableException("Required services not found: " + e.getMessage());
        }
    }

    protected String resolveUserIdByLogin(String login) {
        if (login == null || login.isBlank() || users == null) {
            return null;
        }
        return users.findByLogin(login).map(User::getUserId).orElse(null);
    }

    protected void notifyQuestAdminChanged(User admin, String targetLogin, String questName) {
        if (admin == null || notify == null || targetLogin == null) {
            return;
        }
        String targetUserId = resolveUserIdByLogin(targetLogin);
        if (targetUserId == null) {
            return;
        }
        Map<String, String> data = new HashMap<>();
        data.put("questName", (questName == null || questName.isBlank()) ? "Quest" : questName);
        data.put("what", "Admin updated your quest");
        notify.notify(NotificationEvent.of(
                NotificationType.QUEST_ADMIN_CHANGED,
                admin.getUserId(),
                targetUserId,
                data
        ));
    }

    protected void incCreatedByUserId(String userId) {
        if (userStats == null || userId == null || userId.isBlank()) {
            return;
        }
        try {
            userStats.incCreated(userId);
        } catch (Exception ignore) {
        }
    }

    protected void incCreatedByLogin(String login) {
        incCreatedByUserId(resolveUserIdByLogin(login));
    }

    protected void notifyFriendsPublishedByLogin(String ownerLogin, String questName) {
        if (ownerLogin == null || notify == null || friendService == null) {
            return;
        }
        String ownerId = resolveUserIdByLogin(ownerLogin);
        if (ownerId == null) {
            return;
        }
        notifyFriendsPublishedByUserId(ownerId, questName);
    }

    protected void notifyFriendsPublishedByUserId(String ownerUserId, String questName) {
        if (ownerUserId == null || notify == null || friendService == null) {
            return;
        }
        List<String> friendIds;
        try {
            friendIds = friendService.listFriendIds(ownerUserId);
        } catch (Exception e) {
            return;
        }
        if (friendIds == null || friendIds.isEmpty()) {
            return;
        }
        Map<String, String> data = new HashMap<>();
        data.put("questName", (questName == null || questName.isBlank()) ? "Quest" : questName);
        for (String fid : friendIds) {
            if (fid == null || fid.isBlank() || fid.equals(ownerUserId)) {
                continue;
            }
            notify.notify(NotificationEvent.of(
                    NotificationType.FRIEND_PUBLISHED_QUEST,
                    ownerUserId,
                    fid,
                    data
            ));
        }
    }
}
