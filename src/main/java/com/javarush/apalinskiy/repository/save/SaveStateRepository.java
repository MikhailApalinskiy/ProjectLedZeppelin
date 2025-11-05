package com.javarush.apalinskiy.repository.save;

import com.javarush.apalinskiy.domain.save.SaveState;
import com.javarush.apalinskiy.service.save.SaveStateService;

import java.util.Optional;

/**
 * Repository interface for managing {@link SaveState} persistence
 * and associated global save slots.
 *
 * <p>This repository encapsulates all database access related to user
 * save progress, including automatic creation of new save-state entries,
 * retrieval of slot data, and updates or deletions of global slots.</p>
 *
 * <p>Each {@link SaveState} entity belongs to a single {@link com.javarush.apalinskiy.domain.user.User}
 * and may contain multiple {@link com.javarush.apalinskiy.domain.save.GlobalSlot} records
 * representing saved progress for different quests.</p>
 */
public interface SaveStateRepository {

    /**
     * Retrieves the {@link SaveState} for the given user, creating a new one if it does not exist.
     *
     * @param userId           unique user identifier
     * @param defaultSlotCount default number of slots if a new save state must be created
     * @return existing or newly created {@link SaveState}
     * @throws IllegalArgumentException if the specified user does not exist
     */
    SaveState getOrCreate(String userId, int defaultSlotCount);

    /**
     * Retrieves a specific global save slot by user ID and slot index.
     *
     * @param userId user identifier
     * @param slot   slot index (0-based or 1-based, depending on implementation)
     * @return optional {@link SaveStateService.GlobalSlot} representing saved quest progress
     */
    Optional<SaveStateService.GlobalSlot> getGlobalSlot(String userId, int slot);

    /**
     * Creates or updates a global save slot for the specified user.
     * <p>If no save state exists for the user, one will be created automatically.</p>
     *
     * @param userId    user identifier
     * @param slot      slot index
     * @param questId   quest identifier; may be {@code null} to represent the main quest
     * @param questName quest display name (nullable)
     * @param nodeId    ID of the quest node where the player is currently located
     * @param title     human-readable title or checkpoint description
     */
    void setGlobalSlot(String userId, int slot, String questId, String questName, int nodeId, String title);

    /**
     * Removes (clears) a specific global save slot for a user.
     *
     * @param userId user identifier
     * @param slot   slot index to clear
     */
    void clearGlobalSlot(String userId, int slot);
}
