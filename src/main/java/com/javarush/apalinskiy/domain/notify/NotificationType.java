package com.javarush.apalinskiy.domain.notify;

/**
 * Enumeration of supported notification types in the system.
 * <p>
 * Each value represents a specific category of event that can generate a {@link Notification}.
 * These types are typically used in {@link NotificationEvent} and processed by
 * {@code NotificationService}.
 * </p>
 *
 * <ul>
 *   <li>{@link #FRIEND_REQUEST} – a user has sent a friend request.</li>
 *   <li>{@link #FRIEND_ACCEPTED} – a friend request was accepted.</li>
 *   <li>{@link #FRIEND_PUBLISHED_QUEST} – a friend published a new quest.</li>
 *   <li>{@link #QUEST_MODERATED} – a quest has been moderated (approved or rejected).</li>
 *   <li>{@link #QUEST_ADMIN_CHANGED} – an admin has changed quest details.</li>
 *   <li>{@link #USER_ADMIN_CHANGED} – an admin has changed user details.</li>
 *   <li>{@link #FRIEND_REMOVED} – a friend has been removed from the list.</li>
 * </ul>
 */
public enum NotificationType {
    /**
     * A user has sent a friend request.
     */
    FRIEND_REQUEST,
    /**
     * A friend request was accepted.
     */
    FRIEND_ACCEPTED,
    /**
     * A friend published a new quest.
     */
    FRIEND_PUBLISHED_QUEST,
    /**
     * A quest has been moderated (approved or rejected).
     */
    QUEST_MODERATED,
    /**
     * An admin has changed quest details.
     */
    QUEST_ADMIN_CHANGED,
    /**
     * An admin has changed user details.
     */
    USER_ADMIN_CHANGED,
    /**
     * A friend has been removed from the list.
     */
    FRIEND_REMOVED
}
