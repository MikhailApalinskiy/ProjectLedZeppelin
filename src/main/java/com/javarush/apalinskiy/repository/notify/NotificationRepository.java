package com.javarush.apalinskiy.repository.notify;

import com.javarush.apalinskiy.domain.notify.Notification;

import java.util.List;

public interface NotificationRepository {

    void save(Notification n);

    List<Notification> list(String userId, int limit, int offset);

    int unreadCount(String userId);

    void markAllRead(String userId);

    void clearAll(String userId);

    void markRead(String userId, String id);
}
