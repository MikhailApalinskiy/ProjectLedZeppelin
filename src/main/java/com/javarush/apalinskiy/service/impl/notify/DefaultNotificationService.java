package com.javarush.apalinskiy.service.impl.notify;

import com.javarush.apalinskiy.domain.notify.Notification;
import com.javarush.apalinskiy.domain.notify.NotificationEvent;
import com.javarush.apalinskiy.repository.notify.NotificationRepository;
import com.javarush.apalinskiy.domain.notify.NotificationType;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.service.user.UserService;
import com.javarush.apalinskiy.service.notify.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Default implementation of {@link NotificationService}.
 * <p>
 * Responsible for creating and delivering {@link Notification} entities
 * in response to user or system events.
 * </p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Persist notifications via {@link NotificationRepository}.</li>
 *   <li>Resolve actor user names via {@link UserService}.</li>
 *   <li>Format notification messages based on {@link NotificationType} and {@link NotificationEvent}.</li>
 *   <li>Log all notification events and failures.</li>
 * </ul>
 *
 * <h3>Supported event types</h3>
 * <ul>
 *   <li>{@link NotificationType#FRIEND_REQUEST} – informs user of a new friend request.</li>
 *   <li>{@link NotificationType#FRIEND_ACCEPTED} – informs user their request was accepted.</li>
 *   <li>{@link NotificationType#FRIEND_PUBLISHED_QUEST} – notifies friends about a new quest publication.</li>
 *   <li>{@link NotificationType#QUEST_MODERATED} – informs quest owner about moderation results.</li>
 *   <li>{@link NotificationType#QUEST_ADMIN_CHANGED} – informs quest owner about admin changes.</li>
 *   <li>{@link NotificationType#USER_ADMIN_CHANGED} – informs user about admin changes to their profile.</li>
 *   <li>{@link NotificationType#FRIEND_REMOVED} – informs user that a friend removed them.</li>
 * </ul>
 *
 * <p>
 * This service is transactional in the sense that failures when saving a notification
 * are logged and rethrown to the caller.
 * </p>
 */
public class DefaultNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(DefaultNotificationService.class);

    private final NotificationRepository repo;
    private final UserService users;

    /**
     * Creates a new notification service with the given dependencies.
     *
     * @param repo  repository used to persist notifications
     * @param users user service used to resolve actor names
     */
    public DefaultNotificationService(NotificationRepository repo, UserService users) {
        this.repo = repo;
        this.users = users;
    }

    /**
     * Adds a single notification for a user.
     *
     * @param userId target user ID
     * @param type   notification type
     * @param title  short notification title
     * @param body   notification body (HTML formatted)
     * @throws RuntimeException if persistence fails
     */
    @Override
    public void add(String userId, NotificationType type, String title, String body) {
        log.info("Notify.add type={} targetUserId={} title='{}'", type, userId, title);
        try {
            repo.save(Notification.of(userId, type, title, body));
        } catch (RuntimeException e) {
            log.error("Notify.add failed type={} targetUserId={} title='{}'", type, userId, title, e);
            throw e;
        }
    }

    /**
     * Handles a high-level notification event by generating
     * and persisting the appropriate notification.
     * <p>
     * Resolves the actor's display name via {@link UserService}.
     * If the actor is {@code null}, the name defaults to "System".
     * </p>
     *
     * @param e notification event to process
     */
    @Override
    public void notify(NotificationEvent e) {
        log.info("Notify.event type={} targetUserId={} actorUserId={}", e.type(), e.targetUserId(), e.actorUserId());
        String actorName;
        if (e.actorUserId() == null) {
            actorName = "System";
        } else {
            actorName = users.findById(e.actorUserId())
                    .map(User::getUserName)
                    .orElseGet(() -> {
                        log.warn("Notify.event actor not found actorUserId={}", e.actorUserId());
                        return "User";
                    });
        }
        switch (e.type()) {
            case FRIEND_REQUEST -> add(e.targetUserId(), e.type(),
                    "New friend request",
                    "User <b>" + actorName + "</b> wants to add you as a friend.");
            case FRIEND_ACCEPTED -> add(e.targetUserId(), e.type(),
                    "The application has been accepted",
                    "<b>" + actorName + "</b> accepted your request — now you're friends.");
            case FRIEND_PUBLISHED_QUEST -> {
                String name = safe(e.data(), "questName", "A new quest");
                add(e.targetUserId(), e.type(),
                        "A friend posted a quest",
                        "<b>" + actorName + "</b> published a quest: <b>" + name + "</b>.");
            }
            case QUEST_MODERATED -> {
                String q = safe(e.data(), "questName", "Quest");
                String res = safe(e.data(), "result", "updated");
                add(e.targetUserId(), e.type(),
                        "Moderation of the quest",
                        "Quest <b>" + q + "</b> passed moderation: <b>" + res + "</b>.");
            }
            case QUEST_ADMIN_CHANGED -> {
                String q = safe(e.data(), "questName", "Quest");
                String what = safe(e.data(), "what", "The parameters have been updated");
                add(e.targetUserId(), e.type(),
                        "The admin changed your quest.",
                        "Changes in <b>" + q + "</b>: " + what + ".");
            }
            case USER_ADMIN_CHANGED -> {
                String what = safe(e.data(), "what", "The parameters have been updated\n");
                add(e.targetUserId(), e.type(),
                        "The admin changed the profile",
                        "The administrator has updated your profile settings: " + what + ".");
            }
            case FRIEND_REMOVED -> add(e.targetUserId(), e.type(),
                    "Friend removed",
                    "User <b>" + actorName + "</b> removed you from friends.");
        }
    }

    /**
     * Safely extracts a value from the event data map,
     * falling back to a default if missing or {@code null}.
     *
     * @param m   data map (may be null)
     * @param k   key
     * @param def default value if key missing
     * @return resolved value
     */
    private static String safe(Map<String, String> m, String k, String def) {
        return (m == null) ? def : m.getOrDefault(k, def);
    }
}
