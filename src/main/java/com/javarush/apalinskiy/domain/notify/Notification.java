package com.javarush.apalinskiy.domain.notify;

import com.javarush.apalinskiy.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Represents a user notification entity in the system.
 *
 * <p>Each {@code Notification} is stored in the {@code notifications} table and
 * belongs to a specific {@link User}. Notifications contain information such as
 * title, body, creation time, type, and read status.</p>
 *
 * <p>Instances are typically created using the factory method
 * {@link #of(User, NotificationType, String, String)}.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notifications")
public class Notification {

    /** Unique notification identifier (UUID as a string). */
    @Id
    @Column(name = "notification_id", nullable = false, length = 36)
    private String id;

    /** Type of the notification (e.g., FRIEND_REQUEST, SYSTEM, MESSAGE). */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private NotificationType type;

    /** Notification title shown in user interface. */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /** Optional detailed message body (may be {@code null}). */
    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    /** Timestamp when the notification was created. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Indicates whether the notification has been read by the user. */
    @Column(name = "is_read", nullable = false)
    private Boolean read;

    /** The user who owns this notification. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Creates and initializes a new {@code Notification} instance.
     *
     * <p>Automatically generates a unique UUID, sets {@code createdAt} to
     * the current time, and marks the notification as unread.</p>
     *
     * @param user  the target user who will receive this notification
     * @param type  the {@link NotificationType} describing the notification category
     * @param title the short notification title
     * @param body  the detailed message body (optional)
     * @return a fully initialized {@code Notification} instance
     */
    public static Notification of(User user, NotificationType type, String title, String body) {
        Notification n = new Notification();
        n.id = UUID.randomUUID().toString();
        n.user = user;
        n.type = type;
        n.title = title;
        n.body = body;
        n.createdAt = Instant.now();
        n.read = false;
        return n;
    }

    /**
     * Returns the {@link Date} representation of the creation timestamp.
     *
     * @return creation date as a {@link java.util.Date} instance
     */
    @SuppressWarnings("unused")
    public Date getCreatedAtDate() {
        return Date.from(createdAt);
    }
}
