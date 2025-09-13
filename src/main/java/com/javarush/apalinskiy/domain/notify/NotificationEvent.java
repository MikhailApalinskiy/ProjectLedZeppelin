package com.javarush.apalinskiy.domain.notify;

import java.time.Instant;
import java.util.Map;

/**
 * Immutable event that represents an action which can trigger a notification.
 * <p>
 * A {@code NotificationEvent} captures:
 * <ul>
 *   <li>{@link #type} – the type/category of the notification.</li>
 *   <li>{@link #actorUserId} – the ID of the user who caused the event (actor).</li>
 *   <li>{@link #targetUserId} – the ID of the user who is the recipient (target).</li>
 *   <li>{@link #data} – additional contextual key-value pairs (e.g. questId, message).</li>
 *   <li>{@link #occurredAt} – the timestamp of when the event occurred.</li>
 * </ul>
 *
 * <p>This record is typically used as input to {@code NotificationService} or other
 * components that derive {@link Notification} instances from system events.</p>
 *
 * @param type         type of the notification
 * @param actorUserId  user ID of the actor who triggered the event
 * @param targetUserId user ID of the intended recipient
 * @param data         additional contextual information (never {@code null}, may be empty)
 * @param occurredAt   timestamp of when the event occurred
 */
public record NotificationEvent(NotificationType type, String actorUserId, String targetUserId,
                                Map<String, String> data, Instant occurredAt) {

    /**
     * Creates a new {@code NotificationEvent} with the current timestamp.
     *
     * @param t      type of the notification
     * @param actor  user ID of the actor
     * @param target user ID of the recipient
     * @param data   additional contextual information
     * @return newly created {@code NotificationEvent} with {@link Instant#now()} as {@code occurredAt}
     */
    public static NotificationEvent of(NotificationType t, String actor, String target, Map<String, String> data) {
        return new NotificationEvent(t, actor, target, data, Instant.now());
    }
}
