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
 * Abstract base servlet providing shared functionality for quest moderation and administration.
 *
 * <p>This class is intended to be extended by administrative servlets that handle quest
 * creation, moderation, approval, and user notification. It wires core service dependencies
 * and offers helper methods for user statistics tracking and sending various notifications.</p>
 *
 * <p>Injected dependencies include:</p>
 * <ul>
 *     <li>{@link QuestAuthoringService} — access to quest authoring and moderation logic</li>
 *     <li>{@link NotificationService} — dispatch of moderation and publication events</li>
 *     <li>{@link UserService} — user profile resolution</li>
 *     <li>{@link UserStatsService} — tracking quest creation and completion stats</li>
 *     <li>{@link FriendService} — for notifying friends about published quests</li>
 * </ul>
 *
 * <p>Child classes may safely use the protected methods provided for notifying users,
 * updating statistics, and sending broadcast notifications to friends.</p>
 */
public class BaseQuestAdminServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(BaseQuestAdminServlet.class);

    protected transient QuestAuthoringService authoring;
    protected transient NotificationService notify;
    protected transient UserService users;
    protected transient UserStatsService userStats;
    protected transient FriendService friendService;

    /**
     * Initializes required service dependencies for administrative quest management.
     *
     * @param config servlet configuration provided by the container
     * @throws ServletException if one or more required services are not available
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
     * Sends a notification to a quest author when an admin modifies their quest.
     *
     * <p>This is typically used for cases where an administrator manually edits or
     * adjusts a quest after publication.</p>
     *
     * @param admin        admin user performing the change
     * @param targetUserId the affected quest owner's user ID
     * @param questName    quest name (for message personalization)
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
     * Increments the “created quests” counter for a specific user.
     *
     * <p>Silently ignores missing statistics service or invalid user IDs.</p>
     *
     * @param userId user ID whose stats should be incremented
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
     * Notifies all friends of a user when they publish a new quest.
     *
     * <p>This method retrieves the list of friend IDs and sends a
     * {@link NotificationType#FRIEND_PUBLISHED_QUEST} notification to each one.</p>
     *
     * @param ownerUserId the user who published a quest
     * @param questName   the quest name for message personalization
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
