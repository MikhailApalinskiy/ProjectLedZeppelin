package com.javarush.apalinskiy.domain.user;

import lombok.Getter;

/**
 * Aggregated statistics for a user.
 * <p>
 * A {@code UserStats} instance is immutable and contains counters
 * reflecting the user's activity in the quest system.
 * </p>
 *
 * <ul>
 *   <li>{@link #questsCreated} – number of quests authored by the user.</li>
 *   <li>{@link #questsCompleted} – number of quests completed by the user.</li>
 *   <li>{@link #endingsUnlocked} – number of unique endings reached by the user.</li>
 * </ul>
 *
 * <p>All counters are represented as {@code long} values and are expected
 * to be non-negative.</p>
 */
@Getter
public class UserStats {

    /**
     * Number of quests created by the user.
     */
    private final long questsCreated;
    /**
     * Number of quests completed by the user.
     */
    private final long questsCompleted;
    /**
     * Number of unique endings unlocked by the user.
     */
    private final long endingsUnlocked;

    /**
     * Creates a new immutable {@code UserStats}.
     *
     * @param questsCreated   number of quests authored
     * @param questsCompleted number of quests completed
     * @param endingsUnlocked number of endings unlocked
     */
    public UserStats(long questsCreated, long questsCompleted, long endingsUnlocked) {
        this.questsCreated = questsCreated;
        this.questsCompleted = questsCompleted;
        this.endingsUnlocked = endingsUnlocked;
    }
}
