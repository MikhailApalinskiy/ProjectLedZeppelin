package com.javarush.apalinskiy.service.impl.notify;

import com.javarush.apalinskiy.utils.HibernateUtil;
import com.javarush.apalinskiy.domain.notify.Notification;
import com.javarush.apalinskiy.domain.notify.NotificationEvent;
import com.javarush.apalinskiy.repository.notify.NotificationRepository;
import com.javarush.apalinskiy.domain.notify.NotificationType;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.service.user.UserService;
import com.javarush.apalinskiy.service.notify.NotificationService;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Default implementation of {@link com.javarush.apalinskiy.service.notify.NotificationService}
 * that handles creation and dispatching of user notifications through Hibernate.
 *
 * <p>This service acts as a bridge between domain events ({@link com.javarush.apalinskiy.domain.notify.NotificationEvent})
 * and persistence layer ({@link com.javarush.apalinskiy.repository.notify.NotificationRepository}).</p>
 *
 * <p>All operations are executed within transactional Hibernate sessions,
 * automatically creating {@link com.javarush.apalinskiy.domain.notify.Notification} entities
 * linked to the target {@link com.javarush.apalinskiy.domain.user.User}.</p>
 *
 * <p>The service supports both direct notification creation via {@link #add(String, NotificationType, String, String)}
 * and higher-level event handling via {@link #notify(NotificationEvent)} which generates notifications
 * based on predefined event semantics.</p>
 */
public class DefaultNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(DefaultNotificationService.class);

    private final SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
    private final NotificationRepository repo;
    private final UserService users;

    /**
     * Constructs the default notification service.
     *
     * @param repo  repository used for persisting {@link Notification} entities
     * @param users service used for resolving {@link User} entities by ID
     */
    public DefaultNotificationService(NotificationRepository repo, UserService users) {
        this.repo = repo;
        this.users = users;
    }

    /**
     * Creates and persists a new {@link Notification} for a target user.
     *
     * <p>Starts a local transaction if none is active, assigns all required
     * fields (ID, type, user, timestamps, read flag), and commits immediately
     * unless a higher-level transaction is already in progress.</p>
     *
     * @param userId target user ID (must exist)
     * @param type   notification type
     * @param title  short notification title
     * @param body   notification body (may include HTML markup)
     * @throws IllegalArgumentException if the target user does not exist
     * @throws RuntimeException         if the persistence operation fails
     */

    @Override
    public void add(String userId, NotificationType type, String title, String body) {
        log.info("Notify.add type={} targetUserId={} title='{}'", type, userId, title);
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.getTransaction();
        boolean started = false;
        boolean touchedRO = false;
        boolean prevRO = false;
        try {
            if (!tx.isActive()) {
                tx = session.beginTransaction();
                started = true;
                prevRO = session.isDefaultReadOnly();
                session.setDefaultReadOnly(false);
                touchedRO = true;
                log.debug("Notify.add: tx started, defaultReadOnly={} -> false", prevRO);
            }
            User target = users.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
            Notification n = new Notification();
            n.setId(UUID.randomUUID().toString());
            n.setUser(target);
            n.setType(type);
            n.setTitle(title);
            n.setBody(body);
            n.setCreatedAt(Instant.now());
            n.setRead(false);
            repo.save(n);
            if (started) {
                tx.commit();
                log.debug("Notify.add: tx committed");
            }
            log.debug("Notification added for userId={}", userId);
        } catch (RuntimeException e) {
            if (started) {
                tx.rollback();
                log.debug("Notify.add: tx rolled back");
            }
            log.error("Notify.add failed type={} targetUserId={} title='{}'", type, userId, title, e);
            throw e;
        } finally {
            if (touchedRO) {
                session.setDefaultReadOnly(prevRO);
                log.trace("Notify.add: defaultReadOnly restored to {}", prevRO);
            }
        }
    }

    /**
     * Processes a {@link NotificationEvent} and generates one or more
     * notifications depending on its type.
     *
     * <p>This method resolves the actor (sender) name using {@link UserService},
     * substitutes localized or default text, and dispatches notifications
     * using {@link #add(String, NotificationType, String, String)}.</p>
     *
     * <p>Supported event types include (but are not limited to):</p>
     * <ul>
     *   <li>{@code FRIEND_REQUEST} – incoming friend request</li>
     *   <li>{@code FRIEND_ACCEPTED} – friend request accepted</li>
     *   <li>{@code FRIEND_PUBLISHED_QUEST} – friend published a quest</li>
     *   <li>{@code QUEST_MODERATED} – quest moderation result</li>
     *   <li>{@code QUEST_ADMIN_CHANGED} – administrative quest updates</li>
     *   <li>{@code USER_ADMIN_CHANGED} – profile updated by administrator</li>
     *   <li>{@code FRIEND_REMOVED} – friend removed the user</li>
     * </ul>
     *
     * @param e domain event containing type, actor, target, and optional data
     * @throws RuntimeException if an error occurs while persisting notifications
     */
    @Override
    public void notify(NotificationEvent e) {
        log.info("Notify.event type={} targetUserId={} actorUserId={}",
                e.type(), e.targetUserId(), e.actorUserId());
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.getTransaction();
        boolean started = false;
        boolean touchedRO = false;
        boolean prevRO = false;
        try {
            if (!tx.isActive()) {
                tx = session.beginTransaction();
                started = true;
                prevRO = session.isDefaultReadOnly();
                session.setDefaultReadOnly(false);
                touchedRO = true;
                log.debug("Notify.event: tx started, defaultReadOnly={} -> false", prevRO);
            }
            String actorName;
            if (e.actorUserId() == null) {
                actorName = "System";
                log.debug("Notify.event: actor is System");
            } else {
                actorName = users.findById(e.actorUserId())
                        .map(User::getUserName)
                        .orElseGet(() -> {
                            log.warn("Notify.event actor not found actorUserId={}", e.actorUserId());
                            return "User";
                        });
            }
            switch (e.type()) {
                case FRIEND_REQUEST -> {
                    log.debug("Notify.event: case FRIEND_REQUEST");
                    add(e.targetUserId(), e.type(),
                            "New friend request",
                            "User <b>" + actorName + "</b> wants to add you as a friend.");
                }
                case FRIEND_ACCEPTED -> {
                    log.debug("Notify.event: case FRIEND_ACCEPTED");
                    add(e.targetUserId(), e.type(),
                            "The application has been accepted",
                            "<b>" + actorName + "</b> accepted your request — now you're friends.");
                }
                case FRIEND_PUBLISHED_QUEST -> {
                    log.debug("Notify.event: case FRIEND_PUBLISHED_QUEST");
                    String name = safe(e.data(), "questName", "A new quest");
                    add(e.targetUserId(), e.type(),
                            "A friend posted a quest",
                            "<b>" + actorName + "</b> published a quest: <b>" + name + "</b>.");
                }
                case QUEST_MODERATED -> {
                    log.debug("Notify.event: case QUEST_MODERATED");
                    String q = safe(e.data(), "questName", "Quest");
                    String res = safe(e.data(), "result", "updated");
                    add(e.targetUserId(), e.type(),
                            "Moderation of the quest",
                            "Quest <b>" + q + "</b> passed moderation: <b>" + res + "</b>.");
                }
                case QUEST_ADMIN_CHANGED -> {
                    log.debug("Notify.event: case QUEST_ADMIN_CHANGED");
                    String q = safe(e.data(), "questName", "Quest");
                    String what = safe(e.data(), "what", "The parameters have been updated");
                    add(e.targetUserId(), e.type(),
                            "The admin changed your quest.",
                            "Changes in <b>" + q + "</b>: " + what + ".");
                }
                case USER_ADMIN_CHANGED -> {
                    log.debug("Notify.event: case USER_ADMIN_CHANGED");
                    String what = safe(e.data(), "what", "The parameters have been updated");
                    add(e.targetUserId(), e.type(),
                            "The admin changed the profile",
                            "The administrator has updated your profile settings: " + what + ".");
                }
                case FRIEND_REMOVED -> {
                    log.debug("Notify.event: case FRIEND_REMOVED");
                    add(e.targetUserId(), e.type(),
                            "Friend removed",
                            "User <b>" + actorName + "</b> removed you from friends.");
                }
            }
            if (started) {
                tx.commit();
            }
        } catch (RuntimeException ex) {
            if (started) {
                tx.rollback();
                log.debug("Notify.event: tx committed");
            }
            log.error("Notify.event failed type={} targetUserId={} actorUserId={}",
                    e.type(), e.targetUserId(), e.actorUserId(), ex);
            throw ex;
        } finally {
            if (touchedRO) {
                session.setDefaultReadOnly(prevRO);
                log.trace("Notify.event: defaultReadOnly restored to {}", prevRO);
            }
        }
    }

    /**
     * Safely retrieves a value from the event data map.
     *
     * @param m   map of event data; may be {@code null}
     * @param k   key to lookup
     * @param def default value if key is absent or map is null
     * @return resolved value or default fallback
     */
    private static String safe(Map<String, String> m, String k, String def) {
        return (m == null) ? def : m.getOrDefault(k, def);
    }
}
