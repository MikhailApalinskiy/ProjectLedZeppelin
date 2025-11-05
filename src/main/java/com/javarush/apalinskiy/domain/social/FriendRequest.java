package com.javarush.apalinskiy.domain.social;

import com.javarush.apalinskiy.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Represents a friend request between two {@link User} entities.
 *
 * <p>Each {@code FriendRequest} defines a directional connection attempt
 * between a sender ({@link #fromUser}) and a recipient ({@link #toUser}).
 * Requests are created in the {@link Status#PENDING} state and may later be
 * accepted, declined, or canceled.</p>
 *
 * <p>This entity is stored in the {@code friend_requests} table and
 * uses a generated UUID string as its primary key.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "friend_requests")
public class FriendRequest {

    /**
     * Enumeration representing possible friend request states.
     * <ul>
     *   <li>{@link #PENDING} — request has been sent but not yet answered</li>
     *   <li>{@link #ACCEPTED} — recipient accepted the friend request</li>
     *   <li>{@link #DECLINED} — recipient declined the request</li>
     *   <li>{@link #CANCELED} — sender canceled the request before response</li>
     * </ul>
     */
    public enum Status {PENDING, ACCEPTED, DECLINED, CANCELED}

    /**
     * Unique identifier of the friend request (UUID string).
     */
    @Id
    @Column(name = "friend_request_id", nullable = false, length = 36)
    private String id;

    /**
     * The user who initiated (sent) this friend request.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_friend_req_from"))
    private User fromUser;

    /**
     * The user who received this friend request.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_friend_req_to"))
    private User toUser;

    /**
     * Current status of the friend request.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private Status status = Status.PENDING;

    /**
     * Timestamp of when the friend request was created.
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * Timestamp of when the recipient responded (accepted or declined).
     */
    @Column(name = "responded_at")
    private Instant respondedAt;

    /**
     * Creates a new pending friend request between two users.
     *
     * @param from the user who sends the request
     * @param to   the user who receives the request
     * @return a new {@code FriendRequest} instance in {@link Status#PENDING} state
     */
    public static FriendRequest of(User from, User to) {
        FriendRequest fr = new FriendRequest();
        fr.setFromUser(from);
        fr.setToUser(to);
        fr.setStatus(Status.PENDING);
        fr.setCreatedAt(Instant.now());
        return fr;
    }

    /**
     * Returns the creation timestamp as a legacy {@link Date} object.
     *
     * @return creation time as {@link java.util.Date}
     */
    @SuppressWarnings("unused")
    public Date getCreatedAtDate() {
        return Date.from(createdAt);
    }

    /**
     * Lifecycle callback — initializes ID, timestamps, and status before insertion.
     */
    @PrePersist
    public void prePersist() {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = Status.PENDING;
        }
    }
}
