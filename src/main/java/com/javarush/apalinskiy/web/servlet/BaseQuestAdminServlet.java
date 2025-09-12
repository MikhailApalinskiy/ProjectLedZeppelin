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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BaseQuestAdminServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(BaseQuestAdminServlet.class);

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
            log.debug("BaseQuestAdminServlet init: authoring={}, notify={}, users={}, userStats={}, friendService={}",
                    (authoring == null ? "null" : authoring.getClass().getSimpleName()),
                    (notify == null ? "null" : notify.getClass().getSimpleName()),
                    (users == null ? "null" : users.getClass().getSimpleName()),
                    (userStats == null ? "null" : userStats.getClass().getSimpleName()),
                    (friendService == null ? "null" : friendService.getClass().getSimpleName())
            );
        } catch (IllegalStateException e) {
            log.error("BaseQuestAdminServlet init failed: required services not found", e);
            throw new UnavailableException("Required services not found: " + e.getMessage());
        }
    }

    protected String resolveUserIdByLogin(String login) {
        if (login == null || login.isBlank() || users == null) {
            if (login == null || login.isBlank()) {
                log.warn("resolveUserIdByLogin skipped: blank login");
            } else if (users == null) {
                log.warn("resolveUserIdByLogin skipped: users service is null login='{}'", login);
            }
            return null;
        }
        String id = users.findByLogin(login).map(User::getUserId).orElse(null);
        if (id == null) {
            log.warn("resolveUserIdByLogin: user not found login='{}'", login);
        } else {
            log.debug("resolveUserIdByLogin: login='{}' -> userId={}", login, id);
        }
        return id;
    }

    protected void notifyQuestAdminChanged(User admin, String targetLogin, String questName) {
        if (admin == null || notify == null || targetLogin == null) {
            log.warn("notifyQuestAdminChanged skipped: admin={}, notify={}, targetLoginPresent={}",
                    admin == null ? "null" : admin.getUserId(),
                    notify == null ? "null" : notify.getClass().getSimpleName(),
                    targetLogin != null);
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
        log.info("notifyQuestAdminChanged: adminId={} targetUserId={} quest='{}'",
                admin.getUserId(), targetUserId, data.get("questName"));
    }

    protected void incCreatedByUserId(String userId) {
        if (userStats == null || userId == null || userId.isBlank()) {
            log.warn("incCreatedByUserId skipped: userStatsPresent={} userId='{}'",
                    userStats != null, userId);
            return;
        }
        try {
            userStats.incCreated(userId);
            log.debug("incCreatedByUserId ok userId={}", userId);
        } catch (Exception ignore) {
            log.warn("incCreatedByUserId ignored exception userId={}", userId);
        }
    }

    protected void incCreatedByLogin(String login) {
        incCreatedByUserId(resolveUserIdByLogin(login));
    }

    protected void notifyFriendsPublishedByLogin(String ownerLogin, String questName) {
        if (ownerLogin == null || notify == null || friendService == null) {
            log.warn("notifyFriendsPublishedByLogin skipped: ownerLoginPresent={} notifyPresent={} friendServicePresent={}",
                    ownerLogin != null, notify != null, friendService != null);
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
            log.warn("notifyFriendsPublishedByUserId skipped: ownerIdPresent={} notifyPresent={} friendServicePresent={}",
                    ownerUserId != null, notify != null, friendService != null);
            return;
        }
        List<String> friendIds;
        try {
            friendIds = friendService.listFriendIds(ownerUserId);
        } catch (Exception e) {
            log.warn("notifyFriendsPublishedByUserId: failed to load friends ownerId={}", ownerUserId, e);
            return;
        }
        if (friendIds == null || friendIds.isEmpty()) {
            log.debug("notifyFriendsPublishedByUserId: no friends to notify ownerId={}", ownerUserId);
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
        log.info("notifyFriendsPublished: ownerId={} quest='{}'", ownerUserId, data.get("questName"));
    }
}
