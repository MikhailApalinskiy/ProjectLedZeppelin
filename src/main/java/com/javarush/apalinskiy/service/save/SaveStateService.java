package com.javarush.apalinskiy.service.save;

import com.javarush.apalinskiy.domain.save.SaveState;

import java.time.Instant;
import java.util.Optional;

/**
 * Service interface for managing user save states and global quest slots.
 *
 * <p>This interface provides methods for creating, retrieving, and updating
 * persistent save data related to quest progress. It also defines accessors
 * for global save slots that store the player’s current quest position across
 * multiple quests.</p>
 */
public interface SaveStateService {

    /**
     * Retrieves an existing {@link SaveState} for the specified user or creates a new one if absent.
     *
     * @param userId unique identifier of the user
     * @return an existing or newly created {@link SaveState}
     */
    SaveState getOrCreate(String userId);

    /**
     * Immutable record representing a global save slot.
     *
     * <p>A global slot acts as a lightweight checkpoint linking a user to a specific
     * quest and node. It allows quick resume functionality across different quests.</p>
     *
     * @param index     slot index number
     * @param nodeId    current quest node identifier
     * @param title     short title or label for the saved state
     * @param questId   quest identifier
     * @param questName human-readable quest name
     * @param updatedAt timestamp of the last update
     */
    record GlobalSlot(int index, Integer nodeId, String title, String questId, String questName, Instant updatedAt) {
    }

    /**
     * Retrieves a global save slot for the specified user and index.
     *
     * @param userId user identifier
     * @param slot   slot index number
     * @return optional containing the slot if found; otherwise empty
     */
    Optional<GlobalSlot> getGlobalSlot(String userId, int slot);

    /**
     * Updates or creates a global save slot for the given user.
     *
     * <p>This method associates the specified quest, node, and title with the given slot index.</p>
     *
     * @param userId    user identifier
     * @param slot      slot index number
     * @param questId   quest identifier
     * @param questName quest name
     * @param nodeId    quest node identifier
     * @param title     human-readable title or checkpoint label
     */
    void setGlobalSlot(String userId, int slot, String questId, String questName, int nodeId, String title);

    /**
     * Clears the data stored in the specified global slot for the user.
     *
     * @param userId user identifier
     * @param slot   slot index to clear
     */
    void clearGlobalSlot(String userId, int slot);
}
