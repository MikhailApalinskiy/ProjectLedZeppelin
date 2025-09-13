package com.javarush.apalinskiy.service.impl.user;

import com.javarush.apalinskiy.domain.user.UserStats;
import com.javarush.apalinskiy.service.user.UserStatsService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory implementation of {@link UserStatsService}.
 * <p>
 * Tracks user statistics such as created quests, completed quests,
 * and unique endings unlocked in the main quest. Data is stored
 * only in memory and is lost when the application restarts.
 * </p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Increment the counter of created quests for a user.</li>
 *   <li>Increment completed quest counter and record unlocked endings.</li>
 *   <li>Provide aggregate statistics via {@link #statsOf(String)}.</li>
 * </ul>
 *
 * <h3>Limitations</h3>
 * <ul>
 *   <li><b>Not suitable for production</b>: no persistence, all stats are volatile.</li>
 *   <li>Concurrency is supported via {@link ConcurrentHashMap} and {@link AtomicLong},
 *   but there is no transactional consistency across multiple updates.</li>
 * </ul>
 */
public class InMemoryUserStatsService implements UserStatsService {

    /**
     * Per-user stats entry.
     * Stores counters for created and completed quests,
     * and a set of unlocked final nodes in the main quest.
     */
    private static final class Entry {
        final AtomicLong created = new AtomicLong();
        final AtomicLong completed = new AtomicLong();
        final Set<Integer> mainFinals = ConcurrentHashMap.newKeySet();
    }

    private final ConcurrentMap<String, Entry> byUser = new ConcurrentHashMap<>();

    /**
     * Increments the count of created quests for the given user.
     * <p>Does nothing if the userId is null or blank.</p>
     *
     * @param userId ID of the user
     */
    @Override
    public void incCreated(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        byUser.computeIfAbsent(userId, k -> new Entry()).created.incrementAndGet();
    }

    /**
     * Records that a quest has been completed by the user.
     * <ul>
     *   <li>Increments the completed quest counter.</li>
     *   <li>If the quest is the "main" quest and a final node ID is provided,
     *   adds it to the set of unlocked endings.</li>
     * </ul>
     *
     * @param userId      ID of the user
     * @param questKey    quest identifier (special handling if "main")
     * @param finalNodeId ID of the final node (may be null)
     */
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

    /**
     * Returns statistics for the given user.
     * <p>If no entry exists, returns zeroed statistics.</p>
     *
     * @param userId ID of the user
     * @return {@link UserStats} with created, completed, and unlocked endings counts
     */
    @Override
    public UserStats statsOf(String userId) {
        Entry e = byUser.get(userId);
        if (e == null) {
            return new UserStats(0, 0, 0);
        }
        return new UserStats(e.created.get(), e.completed.get(), e.mainFinals.size());
    }
}
