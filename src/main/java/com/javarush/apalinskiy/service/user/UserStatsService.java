package com.javarush.apalinskiy.service.user;

import com.javarush.apalinskiy.domain.user.UserStats;

/**
 * Service interface for tracking and managing user gameplay statistics.
 *
 * <p>This interface defines methods for updating and retrieving user statistics
 * such as created quests, completed quests, and other performance metrics.
 * Implementations typically persist these values in a database or a cached layer.</p>
 */
public interface UserStatsService {

    /**
     * Increments the counter of quests created by the specified user.
     *
     * @param userId unique identifier of the user
     */
    void incCreated(String userId);

    /**
     * Updates user statistics when a quest is completed.
     *
     * <p>This method is called when a user finishes a quest, allowing the system
     * to record which quest was completed and at which final node the session ended.</p>
     *
     * @param userId      identifier of the user who completed the quest
     * @param questKey    unique quest key or ID
     * @param finalNodeId identifier of the final node reached in the quest
     */
    void onQuestCompleted(String userId, String questKey, Integer finalNodeId);

    /**
     * Retrieves the current statistics for a given user.
     *
     * @param userId user identifier
     * @return {@link UserStats} containing user performance metrics
     */
    UserStats statsOf(String userId);
}
