package com.javarush.apalinskiy.service.save;

import com.javarush.apalinskiy.domain.save.SaveState;

import java.time.Instant;
import java.util.Optional;

/**
 * Service for managing persistent save states of quests for users.
 * <p>
 * Provides access to per-user save slots, allowing storing and restoring
 * progress across quests. Each user has multiple slots that can be
 * set, retrieved, or cleared independently.
 * </p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Create and manage {@link SaveState} instances per user.</li>
 *   <li>Provide access to global save slots with metadata about quest progress.</li>
 *   <li>Allow updating or clearing slots for continuing or resetting progress.</li>
 * </ul>
 */
public interface SaveStateService {

    /**
     * Returns the {@link SaveState} for the given user,
     * creating a new one if none exists yet.
     *
     * @param userId unique identifier of the user
     * @return existing or newly created save state
     */
    SaveState getOrCreate(String userId);

    /**
     * A lightweight projection of a save slot with metadata.
     *
     * @param index     slot index (0-based or 1-based depending on usage convention)
     * @param nodeId    ID of the last visited quest node (may be {@code null})
     * @param title     user-defined or quest-defined title of the save
     * @param questId   identifier of the quest this slot belongs to
     * @param questName display name of the quest
     * @param updatedAt timestamp when the slot was last updated
     */
    record GlobalSlot(int index, Integer nodeId, String title, String questId, String questName, Instant updatedAt) {
    }

    /**
     * Returns the global slot for the given user and slot index, if it exists.
     *
     * @param userId user identifier
     * @param slot   slot index
     * @return optional containing the slot metadata, or empty if not found
     */
    Optional<GlobalSlot> getGlobalSlot(String userId, int slot);

    /**
     * Stores or updates a global slot with quest progress.
     *
     * @param userId    user identifier
     * @param slot      slot index
     * @param questId   quest identifier
     * @param questName quest display name
     * @param nodeId    ID of the current quest node
     * @param title     descriptive title for the slot
     */
    void setGlobalSlot(String userId, int slot, String questId, String questName, int nodeId, String title);

    /**
     * Clears the global slot for the given user, making it empty.
     *
     * @param userId user identifier
     * @param slot   slot index
     */
    void clearGlobalSlot(String userId, int slot);
}
