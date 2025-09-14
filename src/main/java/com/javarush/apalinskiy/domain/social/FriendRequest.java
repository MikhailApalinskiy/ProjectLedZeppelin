package com.javarush.apalinskiy.domain.social;

import lombok.Getter;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Represents a friend request sent from one user to another.
 * <p>
 * A {@code FriendRequest} is immutable and contains:
 * <ul>
 *   <li>{@link #id} – unique identifier of the request.</li>
 *   <li>{@link #fromUserId} – ID of the user who sent the request.</li>
 *   <li>{@link #toUserId} – ID of the target user receiving the request.</li>
 *   <li>{@link #createdAt} – timestamp when the request was created.</li>
 * </ul>
 *
 * <p>Instances are created via the static factory method {@link #of(String, String)}.</p>
 */
@Getter
public class FriendRequest {

    /**
     * Unique identifier of this friend request.
     */
    private final String id;
    /**
     * ID of the user who sent the request.
     */
    private final String fromUserId;
    /**
     * ID of the user who is the recipient.
     */
    private final String toUserId;
    /**
     * Timestamp when the request was created.
     */
    private final Instant createdAt;

    /**
     * Constructs a new immutable {@code FriendRequest}.
     *
     * @param id         unique request ID
     * @param fromUserId sender's user ID
     * @param toUserId   recipient's user ID
     * @param createdAt  creation timestamp (if {@code null}, defaults to {@link Instant#now()})
     */
    private FriendRequest(String id, String fromUserId, String toUserId, Instant createdAt) {
        this.id = id;
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    /**
     * Factory method that creates a new {@code FriendRequest}
     * with a random unique ID and current timestamp.
     *
     * @param fromId ID of the sender
     * @param toId   ID of the recipient
     * @return new {@code FriendRequest} instance
     */
    public static FriendRequest of(String fromId, String toId) {
        return new FriendRequest(UUID.randomUUID().toString(), fromId, toId, Instant.now());
    }

    /**
     * Returns the creation timestamp as a legacy {@link Date}.
     * <p>
     * Useful for JSP/EL bindings or APIs expecting {@code Date}.
     * </p>
     *
     * @return creation time as {@link Date}
     */
    @SuppressWarnings("unused")
    public Date getCreatedAtDate() {
        return Date.from(createdAt);
    }
}
