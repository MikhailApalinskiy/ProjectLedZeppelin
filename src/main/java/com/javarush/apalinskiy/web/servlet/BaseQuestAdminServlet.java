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
 * Base servlet for administration of custom quests.
 * <p>
 * Provides common initialization and utility methods for servlets that
 * manage quest moderation, editing, publishing, and notifications.
 * This class centralizes access to backend services (authoring, users,
 * notifications, stats, friends) so that subclasses can focus on request handling.
 * <p>
 * On {@link #init(ServletConfig)}, it attempts to resolve all required
 * beans from the servlet context, logging which services are available.
 * Missing optional services ({@code UserStatsService}, {@code FriendService})
 * are tolerated and replaced with {@code null}.
 * <b>Subclasses:</b> Servlets such as {@code AdminQuestsModerationServlet}
 * extend this base to reuse initialization and helper methods.
 *
 * @author Your Name
 * @since 1.0
 */
public class BaseQuestAdminServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(BaseQuestAdminServlet.class);

    /**
     * Quest authoring and catalog operations.
     */
    protected transient QuestAuthoringService authoring;

    /**
     * Notification delivery service for moderation, admin, and friend events.
     */
    protected transient NotificationService notify;

    /**
     * Service for user lookup and profile access.
     */
    protected transient UserService users;

    /**
     * Tracks per-user quest statistics (optional, may be {@code null}).
     */
    protected transient UserStatsService userStats;

    /**
     * Manages friendship relations (optional, may be {@code null}).
     */
    protected transient FriendService friendService;

    /**
     * Initializes the servlet by resolving backend services from the servlet context.
     * <p>
     * Mandatory:
     * <ul>
     *   <li>{@link QuestAuthoringService}</li>
     *   <li>{@link NotificationService}</li>
     *   <li>{@link UserService}</li>
     * </ul>
     * Optional:
     * <ul>
     *   <li>{@link UserStatsService}</li>
     *   <li>{@link FriendService}</li>
     * </ul>
     * If a required service is missing, the servlet is marked unavailable.
     *
     * @param config servlet configuration
     * @throws ServletException     if superclass initialization fails
     * @throws UnavailableException if required services are missing
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
     * Sends a {@link NotificationType#QUEST_ADMIN_CHANGED} event to the quest owner
     * when an admin updates their quest.
     * <p>
     * Behavior:
     * <ul>
     *   <li>Skips notification if admin, target user, or notify service is missing.</li>
     *   <li>Attaches quest name (defaulting to "Quest") and a fixed "what" message.</li>
     *   <li>Logs the operation for auditing.</li>
     * </ul>
     *
     * @param admin        the admin user performing the action (may be {@code null})
     * @param targetUserId id of the quest owner (must be non-blank)
     * @param questName    human-readable quest name (defaults to "Quest" if blank)
     */
    protected void notifyQuestAdminChangedById(User admin, String targetUserId, String questName) {
        if (admin == null || notify == null || targetUserId == null || targetUserId.isBlank()) {
            log.warn("notifyQuestAdminChangedById skipped: adminPresent={} notifyPresent={} targetPresent={}",
                    admin != null, notify != null, targetUserId != null);
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
        log.info("notifyQuestAdminChangedById: adminId={} targetUserId={} quest='{}'",
                admin.getUserId(), targetUserId, data.get("questName"));
    }

    /**
     * Increments the "created quests" counter for the given user.
     * <p>
     * Behavior:
     * <ul>
     *   <li>Skips if {@code userStats} is not configured or {@code userId} is blank.</li>
     *   <li>Catches and logs exceptions thrown by {@code userStats.incCreated}.</li>
     * </ul>
     *
     * @param userId identifier of the user whose counter should be incremented
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
     * Notifies all friends of a user when that user publishes a quest.
     * <p>
     * Behavior:
     * <ul>
     *   <li>Skips if owner id, notification service, or friend service is missing.</li>
     *   <li>Retrieves friend IDs via {@code friendService}; logs and aborts if failed.</li>
     *   <li>Sends {@link NotificationType#FRIEND_PUBLISHED_QUEST} events to each friend,
     *       excluding the owner themselves.</li>
     *   <li>Quest name defaults to "Quest" if blank.</li>
     * </ul>
     *
     * @param ownerUserId id of the user who published the quest
     * @param questName   human-readable quest name (defaults to "Quest" if blank)
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
