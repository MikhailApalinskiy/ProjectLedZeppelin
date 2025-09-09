package com.javarush.apalinskiy.stats;

import lombok.Getter;

@Getter
public class UserStats {
    private final long questsCreated;
    private final long questsCompleted;
    private final long endingsUnlocked;

    public UserStats(long questsCreated, long questsCompleted, long endingsUnlocked) {
        this.questsCreated = questsCreated;
        this.questsCompleted = questsCompleted;
        this.endingsUnlocked = endingsUnlocked;
    }
}
