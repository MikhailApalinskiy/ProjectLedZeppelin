package com.javarush.apalinskiy.repository.inmemory.notify;

import com.javarush.apalinskiy.domain.notify.Notification;
import com.javarush.apalinskiy.repository.notify.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of {@link NotificationRepository}.
 * <p>
 * Stores notifications per user in a {@link ConcurrentHashMap},
 * where each user ID maps to a {@code LinkedHashMap} of
 * notification ID → {@link Notification}.
 * </p>
 *
 * <h3>Characteristics</h3>
 * <ul>
 *   <li>Thread-safe at the user level due to use of {@link ConcurrentHashMap}.</li>
 *   <li>Notifications for each user are stored in insertion order
 *       (via {@link LinkedHashMap}), then explicitly sorted by
 *       {@link Notification#getCreatedAt} when listing.</li>
 *   <li>Designed for testing, prototyping, or applications without
 *       persistent storage; data is lost when the JVM stops.</li>
 * </ul>
 *
 * <h3>Operations</h3>
 * <ul>
 *   <li>{@link #save(Notification)} – stores or updates a notification.</li>
 *   <li>{@link #list(String, int, int)} – retrieves a paginated list of notifications.</li>
 *   <li>{@link #unreadCount(String)} – counts notifications that are not read.</li>
 *   <li>{@link #markAllRead(String)} – marks all notifications for a user as read.</li>
 *   <li>{@link #clearAll(String)} – deletes all notifications for a user.</li>
 *   <li>{@link #markRead(String, String)} – marks a single notification as read.</li>
 * </ul>
 *
 * <p>Logging is performed at DEBUG level for normal operations and WARN level
 * if an attempt is made to mark a non-existent notification as read.</p>
 */
public class InMemoryNotificationRepository implements NotificationRepository {

    private static final Logger log = LoggerFactory.getLogger(InMemoryNotificationRepository.class);
    /**
     * Storage: userId → (notificationId → notification).
     */
    private final ConcurrentHashMap<String, Map<String, Notification>> byUser = new ConcurrentHashMap<>();

    /**
     * Saves a notification for the given user.
     * <p>
     * Replaces any existing notification with the same ID.
     * </p>
     *
     * @param n the notification to save (non-null)
     */
    @Override
    public void save(Notification n) {
        byUser.compute(n.getUserId(), (uid, map) -> {
            Map<String, Notification> m = (map == null) ? new LinkedHashMap<>() : new LinkedHashMap<>(map);
            m.put(n.getId(), n);
            return m;
        });
        log.debug("Saved notification id={} userId={}", n.getId(), n.getUserId());
    }

    /**
     * Returns a paginated list of notifications for the given user.
     * <p>
     * Notifications are sorted by {@link Notification#getCreatedAt} in descending order.
     * </p>
     *
     * @param userId target user ID
     * @param limit  maximum number of notifications to return (default 50 if <= 0)
     * @param offset number of notifications to skip before returning results
     * @return list of notifications (never null, may be empty)
     */
    @Override
    public List<Notification> list(String userId, int limit, int offset) {
        Map<String, Notification> m = byUser.getOrDefault(userId, Map.of());
        List<Notification> result = m.values().stream()
                .sorted(Comparator.comparing(Notification::getCreatedAt).reversed())
                .skip(Math.max(0, offset))
                .limit(limit <= 0 ? 50 : limit)
                .collect(Collectors.toList());
        log.debug("Listed notifications userId={} size={} limit={} offset={}", userId, result.size(), limit, offset);
        return result;
    }

    /**
     * Counts how many notifications for a user are currently unread.
     *
     * @param userId target user ID
     * @return number of unread notifications
     */
    @Override
    public int unreadCount(String userId) {
        Map<String, Notification> m = byUser.getOrDefault(userId, Map.of());
        int cnt = 0;
        for (Notification n : m.values()) if (!n.isRead()) cnt++;
        log.debug("Unread count userId={} count={}", userId, cnt);
        return cnt;
    }

    /**
     * Marks all notifications for a given user as read.
     * <p>
     * If the user has no notifications, this is a no-op.
     * </p>
     *
     * @param userId target user ID
     */
    @Override
    public void markAllRead(String userId) {
        Map<String, Notification> m = byUser.get(userId);
        if (m == null) {
            return;
        }
        m.replaceAll((id, n) -> n.isRead() ? n : n.markRead());
        log.debug("All notifications marked as read userId={} size={}", userId, m.size());
    }

    /**
     * Deletes all notifications for a given user.
     *
     * @param userId target user ID
     */
    @Override
    public void clearAll(String userId) {
        byUser.remove(userId);
        log.debug("Cleared all notifications userId={}", userId);
    }

    /**
     * Marks a single notification as read if it exists and is unread.
     * <p>
     * Logs a warning if the notification does not exist.
     * </p>
     *
     * @param userId target user ID
     * @param id     notification ID
     */
    @Override
    public void markRead(String userId, String id) {
        byUser.computeIfPresent(userId, (uid, m) -> {
            Notification n = m.get(id);
            if (n != null && !n.isRead()) {
                m.put(id, n.markRead());
                log.debug("Notification marked read userId={} id={}", userId, id);
            } else if (n == null) {
                log.warn("markRead called but notification not found userId={} id={}", userId, id);
            }
            return m;
        });
    }
}
