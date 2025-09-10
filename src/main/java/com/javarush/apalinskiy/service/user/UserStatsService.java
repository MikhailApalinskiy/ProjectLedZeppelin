package com.javarush.apalinskiy.service.user;

import com.javarush.apalinskiy.domain.user.UserStats;

public interface
UserStatsService {
    void incCreated(String userId);

    void onQuestCompleted(String userId, String questKey, Integer finalNodeId);

    UserStats statsOf(String userId);
}
