package com.javarush.apalinskiy.service.notify;

import com.javarush.apalinskiy.domain.notify.NotificationEvent;
import com.javarush.apalinskiy.domain.notify.NotificationType;

/**
 * Service responsible for creating and delivering user notifications.
 *
 * <p>This interface defines operations for adding new notifications and
 * generating them automatically from higher-level {@link NotificationEvent} objects.
 * Implementations typically persist notifications via a repository and may
 * also trigger real-time delivery mechanisms (e.g. WebSocket, e-mail, etc.).</p>
 */
public interface NotificationService {

    /**
     * Creates and saves a new notification for a specific user.
     *
     * <p>The notification includes a type, title, and body, and is typically
     * marked as unread upon creation. Implementations should ensure that
     * notifications are properly linked to the target user.</p>
     *
     * @param userId target user identifier
     * @param type   logical type of notification (e.g., FRIEND_REQUEST, QUEST_MODERATED)
     * @param title  short title describing the event
     * @param body   detailed HTML-safe text of the message body
     * @throws IllegalArgumentException if {@code userId} or {@code type} is null
     */
    void add(String userId, NotificationType type, String title, String body);

    /**
     * Processes a structured {@link NotificationEvent} and generates
     * one or more user notifications based on its type and data.
     *
     * <p>This method is typically called from other services (e.g., friend or quest services)
     * when a domain event occurs that requires notifying users.</p>
     *
     * @param ev event descriptor containing type, target user, actor user, and additional data
     * @throws IllegalArgumentException if event is null or invalid
     */
    void notify(NotificationEvent ev);
}
