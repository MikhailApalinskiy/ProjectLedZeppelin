package com.javarush.apalinskiy.repository.notify;

import com.javarush.apalinskiy.domain.notify.Notification;

import java.util.List;

/**
 * Repository interface for managing user {@link Notification} entities.
 *
 * <p>Defines persistence operations for saving, listing, counting, and updating
 * notification read states. Implementations may use Hibernate, JPA, or any other
 * persistence backend.</p>
 *
 * <p>All operations are expected to run within an active transaction context
 * provided by the service layer.</p>
 */
public interface NotificationRepository {

    /**
     * Persists or updates a notification entity.
     *
     * @param n notification to save
     */
    void save(Notification n);

    /**
     * Retrieves a paginated list of notifications for a specific user,
     * ordered by creation date (usually descending).
     *
     * @param userId user identifier
     * @param limit  maximum number of records to return
     * @param offset starting offset for pagination
     * @return list of notifications for the user
     */
    List<Notification> list(String userId, int limit, int offset);

    /**
     * Counts all unread notifications for the given user.
     *
     * @param userId user identifier
     * @return number of unread notifications
     */
    int unreadCount(String userId);

    /**
     * Marks all notifications for the given user as read.
     *
     * @param userId user identifier
     */
    void markAllRead(String userId);

    /**
     * Deletes all notifications associated with the given user.
     *
     * @param userId user identifier
     */
    void clearAll(String userId);

    /**
     * Marks a specific notification as read for the given user.
     *
     * @param userId user identifier
     * @param id     notification ID
     */
    void markRead(String userId, String id);
}
