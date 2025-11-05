package com.javarush.apalinskiy.domain.notify;

import java.time.Instant;
import java.util.Map;

/**
 * Immutable domain event representing a notification that occurred in the system.
 *
 * <p>Each {@code NotificationEvent} captures a specific action or change (for example,
 * a friend request, message, or quest update) that involves two users — the actor
 * (who triggered the event) and the target (who receives or is affected by it).</p>
 *
 * <p>The event also stores a small {@link Map} of contextual data and a precise
 * timestamp of when the event occurred.</p>
 *
 * <p>This record is typically used for asynchronous notification processing,
 * message delivery, or event-based audit tracking.</p>
 *
 * @param type         the type of notification that occurred
 * @param actorUserId  the ID of the user who initiated the action
 * @param targetUserId the ID of the user who is the target of the event
 * @param data         optional map of extra data associated with the event
 * @param occurredAt   the timestamp when the event occurred
 */
public record NotificationEvent(NotificationType type, String actorUserId, String targetUserId,
                                Map<String, String> data, Instant occurredAt) {

    /**
     * Creates a new {@code NotificationEvent} instance with the current timestamp.
     *
     * @param t      the {@link NotificationType} describing the event category
     * @param actor  the ID of the user who triggered the event
     * @param target the ID of the user who is the target of the event
     * @param data   additional contextual key–value data
     * @return a new immutable {@code NotificationEvent} instance with {@code occurredAt = Instant.now()}
     */
    public static NotificationEvent of(NotificationType t, String actor, String target, Map<String, String> data) {
        return new NotificationEvent(t, actor, target, data, Instant.now());
    }
}
