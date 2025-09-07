package com.javarush.apalinskiy.friends;

import lombok.Getter;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Getter
public class FriendRequest {
    private final String id;
    private final String fromUserId;
    private final String toUserId;
    private final Instant createdAt;

    private FriendRequest(String id, String fromUserId, String toUserId, Instant createdAt) {
        this.id = id;
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public static FriendRequest of(String fromId, String toId) {
        return new FriendRequest(UUID.randomUUID().toString(), fromId, toId, Instant.now());
    }

    public Date getCreatedAtDate() {
        return Date.from(createdAt);
    }
}
