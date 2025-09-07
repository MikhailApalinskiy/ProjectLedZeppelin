package com.javarush.apalinskiy.mail;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryNotificationRepository implements NotificationRepository {

    private final ConcurrentHashMap<String, Map<String, Notification>> byUser = new ConcurrentHashMap<>();

    @Override
    public void save(Notification n) {
        byUser.compute(n.getUserId(), (uid, map) -> {
            Map<String, Notification> m = (map == null) ? new LinkedHashMap<>() : new LinkedHashMap<>(map);
            m.put(n.getId(), n);
            return m;
        });
    }

    @Override
    public List<Notification> list(String userId, int limit, int offset) {
        Map<String, Notification> m = byUser.getOrDefault(userId, Map.of());
        return m.values().stream()
                .sorted(Comparator.comparing(Notification::getCreatedAt).reversed())
                .skip(Math.max(0, offset))
                .limit(limit <= 0 ? 50 : limit)
                .collect(Collectors.toList());
    }

    @Override
    public int unreadCount(String userId) {
        Map<String, Notification> m = byUser.getOrDefault(userId, Map.of());
        int cnt = 0;
        for (Notification n : m.values()) if (!n.isRead()) cnt++;
        return cnt;
    }

    @Override
    public void markAllRead(String userId) {
        Map<String, Notification> m = byUser.get(userId);
        if (m == null) return;
        m.replaceAll((id, n) -> n.isRead() ? n : n.markRead());
    }

    @Override
    public void clearAll(String userId) {
        byUser.remove(userId);
    }

    @Override
    public Optional<Notification> find(String userId, String id) {
        Map<String, Notification> m = byUser.getOrDefault(userId, Map.of());
        return Optional.ofNullable(m.get(id));
    }

    @Override
    public void markRead(String userId, String id) {
        byUser.computeIfPresent(userId, (uid, m) -> {
            Notification n = m.get(id);
            if (n != null && !n.isRead()) m.put(id, n.markRead());
            return m;
        });
    }
}
