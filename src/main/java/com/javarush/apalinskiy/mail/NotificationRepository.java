package com.javarush.apalinskiy.mail;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository {

    void save(Notification n);

    List<Notification> list(String userId, int limit, int offset);

    int unreadCount(String userId);

    void markAllRead(String userId);

    void clearAll(String userId);

    Optional<Notification> find(String userId, String id);

    void markRead(String userId, String id);
}
