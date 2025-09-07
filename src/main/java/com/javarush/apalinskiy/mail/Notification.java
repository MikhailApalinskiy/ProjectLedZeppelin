package com.javarush.apalinskiy.mail;

import lombok.Getter;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Getter
public class Notification {
    private final String id;
    private final String userId;
    private final NotificationType type;
    private final String title;
    private final String body;
    private final Instant createdAt;
    private final boolean read;

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

    public static Notification of(String userId, NotificationType type, String title, String body) {
        return new Notification(UUID.randomUUID().toString(), userId, type, title, body, Instant.now(), false);
    }

    public Notification markRead() {
        return new Notification(id, userId, type, title, body, createdAt, true);
    }

    public Date getCreatedAtDate() {
        return Date.from(createdAt);
    }
}
