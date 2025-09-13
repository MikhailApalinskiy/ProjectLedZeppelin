package com.javarush.apalinskiy.repository.notify;

import com.javarush.apalinskiy.domain.notify.Notification;

import java.util.List;

/**
 * Repository interface for managing {@link Notification} entities.
 * <p>
 * Provides persistence operations for storing, retrieving,
 * and updating notifications for a specific user.
 * </p>
 *
 * <h3>Contract</h3>
 * <ul>
 *   <li>Each notification belongs to a single user (see {@link Notification#getUserId()}).</li>
 *   <li>Unread/read status must be preserved and retrievable.</li>
 *   <li>Implementations should return results sorted by creation time descending,
 *       unless explicitly documented otherwise.</li>
 *   <li>All methods must be thread-safe if the implementation is used concurrently.</li>
 * </ul>
 */
public interface NotificationRepository {

    /**
     * Persists the given notification.
     * If a notification with the same ID already exists, it should be replaced or updated.
     *
     * @param n notification to save
     */
    void save(Notification n);

    /**
     * Returns a paginated list of notifications for the given user.
     *
     * @param userId user identifier
     * @param limit  maximum number of items to return (if ≤ 0, implementation may apply a default)
     * @param offset number of items to skip before starting to collect the result
     * @return list of notifications, typically sorted by creation time (newest first)
     */
    List<Notification> list(String userId, int limit, int offset);

    /**
     * Returns the count of unread notifications for the given user.
     *
     * @param userId user identifier
     * @return number of notifications not marked as read
     */
    int unreadCount(String userId);

    /**
     * Marks all notifications of the given user as read.
     *
     * @param userId user identifier
     */
    void markAllRead(String userId);

    /**
     * Removes all notifications of the given user.
     *
     * @param userId user identifier
     */
    void clearAll(String userId);

    /**
     * Marks a single notification as read, if it exists.
     *
     * @param userId user identifier
     * @param id     notification identifier
     */
    void markRead(String userId, String id);
}
