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

/**
 * Base class for admin servlets that operate on custom quests and related user signals.
 * <p>
 * Centralizes lookup of core services from the {@link ServletContext} and provides
 * small helper methods for user resolution, notifications, and stats updates.
 * Subclasses are expected to enforce admin authorization upstream (e.g., via a filter).
 * </p>
 *
 * <h3>Injected services (from context)</h3>
 * <ul>
 *   <li>{@code AUTHORING_SERVICE} → {@link QuestAuthoringService} (required)</li>
 *   <li>{@code NOTIFY_SERVICE} → {@link NotificationService} (required)</li>
 *   <li>{@code USER_SERVICE} → {@link UserService} (required)</li>
 *   <li>{@code USER_STATS_SERVICE} → {@link UserStatsService} (optional)</li>
 *   <li>{@code FRIEND_SERVICE} → {@link FriendService} (optional)</li>
 * </ul>
 *
 * <h3>Helpers</h3>
 * <ul>
 *   <li>{@link #resolveUserIdByLogin(String)} — maps login to userId with logging.</li>
 *   <li>{@link #notifyQuestAdminChanged(User, String, String)} — notifies the owner that an admin updated their quest.</li>
 *   <li>{@link #incCreatedByUserId(String)} / {@link #incCreatedByLogin(String)} — increments "created" counter.</li>
 *   <li>{@link #notifyFriendsPublishedByLogin(String, String)} / {@link #notifyFriendsPublishedByUserId(String, String)}
 *       — broadcasts a "friend published quest" event to the owner’s friends.</li>
 * </ul>
 *
 * <p>All service fields are marked {@code transient} to avoid accidental session serialization.</p>
 */
public class BaseQuestAdminServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(BaseQuestAdminServlet.class);

    /**
     * Required authoring service (catalog and moderation operations).
     */
    protected transient QuestAuthoringService authoring;
    /**
     * Notification dispatch service.
     */
    protected transient NotificationService notify;
    /**
     * User directory / identity lookups.
     */
    protected transient UserService users;
    /**
     * Optional per-user statistics service.
     */
    protected transient UserStatsService userStats;
    /**
     * Optional friend graph service.
     */
    protected transient FriendService friendService;

    /**
     * Resolves required/optional services from the {@link ServletContext}.
     * <p>
     * Required services must be present; otherwise initialization fails with
     * {@link UnavailableException}. Optional services are probed and may remain {@code null}.
     * </p>
     */
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

    /**
     * Resolves a user id by their login (username) using {@link UserService}.
     * <p>
     * Returns {@code null} and logs a warning if login is blank, the user service is missing,
     * or the user cannot be found.
     * </p>
     *
     * @param login user login (may be {@code null})
     * @return resolved user id or {@code null}
     */
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

    /**
     * Sends a {@link NotificationType#QUEST_ADMIN_CHANGED} notification to the owner
     * indicating that an admin updated their quest.
     * <p>
     * Skips silently (with a warning) if required data/services are missing or the owner cannot be resolved.
     * </p>
     *
     * @param admin       acting admin (may be {@code null})
     * @param targetLogin owner's login
     * @param questName   quest display name (falls back to "Quest" if blank)
     */
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

    /**
     * Increments the "created quests" counter for the given user id using {@link UserStatsService}.
     * <p>Safely no-ops if the stats service is absent, user id is blank, or the call fails.</p>
     */
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

    /**
     * Convenience wrapper for {@link #incCreatedByUserId(String)} that accepts a login.
     */
    protected void incCreatedByLogin(String login) {
        incCreatedByUserId(resolveUserIdByLogin(login));
    }

    /**
     * Notifies all friends of the quest owner (resolved by login) that the owner has published a quest.
     * <p>Skips if any required dependency is missing or the owner cannot be resolved.</p>
     */
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

    /**
     * Notifies all friends (by user id) that the owner has published a quest.
     * <p>
     * Fetches friend ids from {@link FriendService}, filters out blanks/self, and sends
     * {@link NotificationType#FRIEND_PUBLISHED_QUEST} to each friend with payload {questName}.
     * </p>
     */
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
