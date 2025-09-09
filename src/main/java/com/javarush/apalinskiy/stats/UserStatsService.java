package com.javarush.apalinskiy.stats;

public interface
UserStatsService {
    void incCreated(String userId);

    void onQuestCompleted(String userId, String questKey, Integer finalNodeId);

    UserStats statsOf(String userId);
}
