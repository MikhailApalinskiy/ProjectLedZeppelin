package com.javarush.apalinskiy.domain.notify;

import lombok.Getter;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Represents a user notification within the system.
 * <p>
 * Each notification is immutable and contains:
 * <ul>
 *   <li>A unique identifier {@link #id}.</li>
 *   <li>The target user ID {@link #userId}.</li>
 *   <li>A {@link NotificationType} indicating the notification category.</li>
 *   <li>A short title and optional body message.</li>
 *   <li>The creation timestamp {@link #createdAt}.</li>
 *   <li>A {@code read} flag indicating if the notification was acknowledged.</li>
 * </ul>
 * <p>
 * Instances are created via the static factory method {@link #of(String, NotificationType, String, String)}
 * or as a new copy through {@link #markRead()}.
 *
 * <p>This class is annotated with Lombok {@code @Getter}, so all fields have getters generated automatically.</p>
 */
@Getter
public class Notification {

    /**
     * Unique identifier of the notification.
     */
    private final String id;
    /**
     * ID of the user this notification belongs to.
     */
    private final String userId;
    /**
     * Type/category of the notification.
     */
    private final NotificationType type;
    /**
     * Short title of the notification.
     */
    private final String title;
    /**
     * Optional body message with more details.
     */
    private final String body;
    /**
     * Creation timestamp of the notification.
     */
    private final Instant createdAt;
    /**
     * Flag indicating whether this notification has been read.
     */
    private final boolean read;

    /**
     * Constructs a new {@code Notification}.
     *
     * @param id        unique identifier
     * @param userId    target user ID
     * @param type      type of notification
     * @param title     short title
     * @param body      detailed message body (may be {@code null})
     * @param createdAt creation timestamp (if {@code null}, defaults to {@link Instant#now()})
     * @param read      whether the notification is marked as read
     */
    private Notification(String id, String userId, NotificationType type,
                         String title, String body, Instant createdAt, boolean read) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
        this.read = read;
    }

    /**
     * Creates a new unread notification with a random unique ID and current timestamp.
     *
     * @param userId target user ID
     * @param type   type of notification
     * @param title  short title
     * @param body   detailed message body
     * @return newly created {@code Notification} instance
     */
    public static Notification of(String userId, NotificationType type, String title, String body) {
        return new Notification(UUID.randomUUID().toString(), userId, type, title, body, Instant.now(), false);
    }

    /**
     * Returns a copy of this notification marked as read.
     *
     * @return new {@code Notification} instance with {@code read=true}
     */
    public Notification markRead() {
        return new Notification(id, userId, type, title, body, createdAt, true);
    }

    /**
     * Returns the creation timestamp as a legacy {@link Date} instance.
     * <p>
     * Useful for JSP/EL bindings or APIs expecting {@code java.util.Date}.
     * </p>
     *
     * @return creation time as {@link Date}
     */
    @SuppressWarnings("unused")
    public Date getCreatedAtDate() {
        return Date.from(createdAt);
    }
}
