package com.javarush.apalinskiy.repository.inmemory.notify;

import com.javarush.apalinskiy.domain.notify.Notification;
import com.javarush.apalinskiy.repository.notify.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryNotificationRepository implements NotificationRepository {

    private static final Logger log = LoggerFactory.getLogger(InMemoryNotificationRepository.class);
    private final ConcurrentHashMap<String, Map<String, Notification>> byUser = new ConcurrentHashMap<>();

    @Override
    public void save(Notification n) {
        byUser.compute(n.getUserId(), (uid, map) -> {
            Map<String, Notification> m = (map == null) ? new LinkedHashMap<>() : new LinkedHashMap<>(map);
            m.put(n.getId(), n);
            return m;
        });
        log.debug("Saved notification id={} userId={}", n.getId(), n.getUserId());
    }

    @Override
    public List<Notification> list(String userId, int limit, int offset) {
        Map<String, Notification> m = byUser.getOrDefault(userId, Map.of());
        List<Notification> result = m.values().stream()
                .sorted(Comparator.comparing(Notification::getCreatedAt).reversed())
                .skip(Math.max(0, offset))
                .limit(limit <= 0 ? 50 : limit)
                .collect(Collectors.toList());
        log.debug("Listed notifications userId={} size={} limit={} offset={}", userId, result.size(), limit, offset);
        return result;
    }

    @Override
    public int unreadCount(String userId) {
        Map<String, Notification> m = byUser.getOrDefault(userId, Map.of());
        int cnt = 0;
        for (Notification n : m.values()) if (!n.isRead()) cnt++;
        log.debug("Unread count userId={} count={}", userId, cnt);
        return cnt;
    }

    @Override
    public void markAllRead(String userId) {
        Map<String, Notification> m = byUser.get(userId);
        if (m == null) {
            return;
        }
        m.replaceAll((id, n) -> n.isRead() ? n : n.markRead());
        log.debug("All notifications marked as read userId={} size={}", userId, m.size());
    }

    @Override
    public void clearAll(String userId) {
        byUser.remove(userId);
        log.debug("Cleared all notifications userId={}", userId);
    }

    @Override
    public void markRead(String userId, String id) {
        byUser.computeIfPresent(userId, (uid, m) -> {
            Notification n = m.get(id);
            if (n != null && !n.isRead()) {
                m.put(id, n.markRead());
                log.debug("Notification marked read userId={} id={}", userId, id);
            } else if (n == null) {
                log.warn("markRead called but notification not found userId={} id={}", userId, id);
            }
            return m;
        });
    }
}
