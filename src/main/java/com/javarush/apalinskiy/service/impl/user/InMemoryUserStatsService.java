package com.javarush.apalinskiy.service.impl.user;

import com.javarush.apalinskiy.domain.user.UserStats;
import com.javarush.apalinskiy.service.user.UserStatsService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryUserStatsService implements UserStatsService {

    private static final class Entry {
        final AtomicLong created = new AtomicLong();
        final AtomicLong completed = new AtomicLong();
        final Set<Integer> mainFinals = ConcurrentHashMap.newKeySet();
    }

    private final ConcurrentMap<String, Entry> byUser = new ConcurrentHashMap<>();

    @Override
    public void incCreated(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        byUser.computeIfAbsent(userId, k -> new Entry()).created.incrementAndGet();
    }

    @Override
    public void onQuestCompleted(String userId, String questKey, Integer finalNodeId) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        Entry e = byUser.computeIfAbsent(userId, k -> new Entry());
        e.completed.incrementAndGet();
        if ("main".equals(questKey) && finalNodeId != null) {
            e.mainFinals.add(finalNodeId);
        }
    }

    @Override
    public UserStats statsOf(String userId) {
        Entry e = byUser.get(userId);
        if (e == null) {
            return new UserStats(0, 0, 0);
        }
        return new UserStats(e.created.get(), e.completed.get(), e.mainFinals.size());
    }
}
