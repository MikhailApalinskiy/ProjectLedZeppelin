package com.javarush.apalinskiy.domain.notify;

/**
 * Defines all possible types of notifications that can be sent to users.
 *
 * <p>Each {@link NotificationType} represents a distinct event or state change
 * within the TextQuest platform that may trigger user-facing notifications.</p>
 *
 * <h2>Available types</h2>
 * <ul>
 *   <li>{@link #FRIEND_REQUEST} — a new friend request has been received</li>
 *   <li>{@link #FRIEND_ACCEPTED} — a sent friend request has been accepted</li>
 *   <li>{@link #FRIEND_PUBLISHED_QUEST} — a friend has published a new quest</li>
 *   <li>{@link #QUEST_MODERATED} — a user’s quest has been moderated by an admin</li>
 *   <li>{@link #QUEST_ADMIN_CHANGED} — administrative changes were made to a quest</li>
 *   <li>{@link #USER_ADMIN_CHANGED} — the user’s account permissions were modified by an admin</li>
 *   <li>{@link #FRIEND_REMOVED} — a friend has removed the user from their friend list</li>
 * </ul>
 */
public enum NotificationType {

    /**
     * A new friend request has been received.
     */
    FRIEND_REQUEST,

    /**
     * A friend request was accepted by the recipient.
     */
    FRIEND_ACCEPTED,

    /**
     * A friend has published a new quest.
     */
    FRIEND_PUBLISHED_QUEST,

    /**
     * A quest has been reviewed or moderated by an administrator.
     */
    QUEST_MODERATED,

    /**
     * Administrative changes were made to a quest (e.g. forced update or unpublish).
     */
    QUEST_ADMIN_CHANGED,

    /**
     * Administrative changes were made to a user account (e.g. role update or ban).
     */
    USER_ADMIN_CHANGED,

    /**
     * A friend relationship has been removed.
     */
    FRIEND_REMOVED
}
