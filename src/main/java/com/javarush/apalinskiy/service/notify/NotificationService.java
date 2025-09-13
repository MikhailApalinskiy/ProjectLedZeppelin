package com.javarush.apalinskiy.service.notify;

import com.javarush.apalinskiy.domain.notify.NotificationEvent;
import com.javarush.apalinskiy.domain.notify.NotificationType;
import com.javarush.apalinskiy.repository.notify.NotificationRepository;

/**
 * Service for managing user notifications.
 * <p>
 * Defines operations to create and deliver notifications
 * either directly ({@link #add(String, NotificationType, String, String)})
 * or via higher-level events ({@link #notify(NotificationEvent)}).
 * </p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Create notifications targeted at specific users.</li>
 *   <li>Translate domain events into user-facing notifications.</li>
 *   <li>Delegate persistence and delivery to the underlying {@link NotificationRepository} implementation.</li>
 * </ul>
 */
public interface NotificationService {

    /**
     * Adds a new notification for a specific user.
     *
     * @param userId the ID of the target user
     * @param type   the type of notification
     * @param title  the notification title
     * @param body   the notification body (may contain HTML)
     */
    void add(String userId, NotificationType type, String title, String body);

    /**
     * Processes a domain event and generates the corresponding notification(s).
     *
     * @param ev the notification event describing actor, target, type, and additional data
     */
    void notify(NotificationEvent ev);
}
