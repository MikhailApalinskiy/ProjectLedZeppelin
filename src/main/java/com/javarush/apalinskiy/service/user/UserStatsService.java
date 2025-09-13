package com.javarush.apalinskiy.service.user;

import com.javarush.apalinskiy.domain.user.UserStats;

/**
 * Service for tracking and retrieving user quest-related statistics.
 * <p>
 * Provides counters for quests created, quests completed, and unique
 * final endings unlocked in the main quest.
 * </p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Increment statistics when a user creates a quest.</li>
 *   <li>Record quest completions and unlocked endings.</li>
 *   <li>Expose aggregated statistics via {@link #statsOf(String)}.</li>
 * </ul>
 */
public interface UserStatsService {

    /**
     * Increments the counter of created quests for the specified user.
     *
     * @param userId unique user identifier (ignored if {@code null} or blank)
     */
    void incCreated(String userId);

    /**
     * Records a quest completion event for the specified user.
     * <p>
     * If the quest key is {@code "main"} and a final node ID is provided,
     * it is stored to count unique endings unlocked.
     * </p>
     *
     * @param userId      unique user identifier (ignored if {@code null} or blank)
     * @param questKey    identifier of the quest (e.g., "main" for the primary quest)
     * @param finalNodeId ID of the final node, or {@code null} if not applicable
     */
    void onQuestCompleted(String userId, String questKey, Integer finalNodeId);

    /**
     * Retrieves aggregated statistics for a given user.
     *
     * @param userId unique user identifier
     * @return a {@link UserStats} instance containing the user's statistics,
     * or an empty statistics object if none are recorded
     */
    UserStats statsOf(String userId);
}
