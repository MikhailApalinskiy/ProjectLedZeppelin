package com.javarush.apalinskiy.domain.notify;

import java.time.Instant;
import java.util.Map;

public record NotificationEvent(NotificationType type, String actorUserId, String targetUserId,
                                Map<String, String> data, Instant occurredAt) {

    public static NotificationEvent of(NotificationType t, String actor, String target, Map<String, String> data) {
        return new NotificationEvent(t, actor, target, data, Instant.now());
    }
}
