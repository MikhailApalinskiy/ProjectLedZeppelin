package com.javarush.apalinskiy.domain.save;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents persistent save data for a user's quest progress.
 * <p>
 * A {@code SaveState} stores both global slots (manual saves across quests)
 * and per-quest transient progress markers.
 * </p>
 *
 * <h3>Contents</h3>
 * <ul>
 *   <li>{@link #userId} – ID of the user owning this save state.</li>
 *   <li>{@link #slotCount} – number of available global slots.</li>
 *   <li>{@link #globalSlots} – array of {@link SaveSlot} objects
 *       representing explicit saves, sized by {@code slotCount}.</li>
 *   <li>{@link #slotsByQuest} – per-quest mapping of slot data (internal use).</li>
 *   <li>{@link #lastNodeByQuest} – last visited node per quest (internal use).</li>
 *   <li>{@link #updatedAt} – last modification timestamp.</li>
 *   <li>{@link #version} – schema version for compatibility (default: 3).</li>
 * </ul>
 *
 * <h3>Key conventions</h3>
 * <ul>
 *   <li>{@link #MAIN_QUEST_KEY} = {@code "main"} is used when quest ID is absent.</li>
 *   <li>{@link #questKey(String)} ensures a non-null, non-blank key for indexing saves.</li>
 * </ul>
 *
 * <h3>Thread-safety</h3>
 * The per-quest maps are {@link ConcurrentHashMap}, so concurrent access is safe.
 * The {@link #globalSlots} array is not inherently thread-safe and should be guarded externally.
 */
@Getter
@Setter
public class SaveState {

    /**
     * Fallback quest key used when quest ID is missing or blank.
     */
    public static final String MAIN_QUEST_KEY = "main";

    /**
     * ID of the user this save state belongs to.
     */
    private final String userId;
    /**
     * Number of available global save slots (always >= 1).
     */
    private final int slotCount;

    /**
     * Per-quest save slots (internal).
     */
    private final Map<String, int[]> slotsByQuest = new ConcurrentHashMap<>();
    /**
     * Last visited node ID per quest (internal).
     */
    private final Map<String, Integer> lastNodeByQuest = new ConcurrentHashMap<>();

    /**
     * Represents a single global save slot.
     * <p>
     * Each slot stores the quest reference, node position,
     * a user-facing title, and last update timestamp.
     * </p>
     */
    @Getter
    @Setter
    public static class SaveSlot {

        /**
         * ID of the quest (or {@link SaveState#MAIN_QUEST_KEY} for main quest).
         */
        private String questId;
        /**
         * Name of the quest.
         */
        private String questName;
        /**
         * Current node ID where the player is located.
         */
        private int nodeId;
        /**
         * Optional user-facing title for the save slot.
         */
        private String title;
        /**
         * Timestamp when this slot was last updated.
         */
        private Instant updatedAt;
    }

    /**
     * Global save slots for this user.
     */
    private final SaveSlot[] globalSlots;

    /**
     * Last modification timestamp of this save state.
     */
    private Instant updatedAt = Instant.now();
    /**
     * Schema version of this save state (default = 3).
     */
    private int version = 3;

    /**
     * Creates a new save state with default number of slots (10).
     *
     * @param userId owner user ID
     */
    public SaveState(String userId) {
        this(userId, 10);
    }

    /**
     * Creates a new save state with custom slot count.
     *
     * @param userId    owner user ID
     * @param slotCount desired slot count (minimum 1)
     */
    public SaveState(String userId, int slotCount) {
        this.userId = userId;
        this.slotCount = Math.max(1, slotCount);
        this.globalSlots = new SaveSlot[this.slotCount];
    }

    /**
     * Normalizes a quest key, substituting {@link #MAIN_QUEST_KEY}
     * if the input is null or blank.
     *
     * @param questIdOrNull quest ID (nullable/blank allowed)
     * @return normalized key
     */
    public static String questKey(String questIdOrNull) {
        return (questIdOrNull == null || questIdOrNull.isBlank()) ? MAIN_QUEST_KEY : questIdOrNull;
    }

    /**
     * Returns the global save slot at the given index, or {@code null}
     * if the index is invalid or the slot is empty.
     *
     * @param slot index of the global slot
     * @return save slot or {@code null}
     */
    public SaveSlot getGlobalSlot(int slot) {
        if (slot < 0 || slot >= globalSlots.length) {
            return null;
        }
        return globalSlots[slot];
    }

    /**
     * Sets a global slot at the given index with new save data.
     * <p>
     * Updates {@link #updatedAt} after modification.
     * </p>
     *
     * @param slot      index of the slot (0-based, must be in bounds)
     * @param questId   quest ID (normalized via {@link #questKey(String)})
     * @param questName quest name
     * @param nodeId    node ID where the player is located
     * @param title     user-facing title for the save
     * @throws IllegalArgumentException if {@code slot} is out of bounds
     */
    public void setGlobalSlot(int slot, String questId, String questName, int nodeId, String title) {
        if (slot < 0 || slot >= globalSlots.length) {
            throw new IllegalArgumentException("slot out of bounds: " + slot);
        }
        SaveSlot s = new SaveSlot();
        s.setQuestId(questKey(questId));
        s.setQuestName(questName);
        s.setNodeId(nodeId);
        s.setTitle(title);
        s.setUpdatedAt(Instant.now());
        globalSlots[slot] = s;
        touch();
    }

    /**
     * Clears the global save slot at the given index, if valid.
     * <p>
     * Updates {@link #updatedAt} after modification.
     * </p>
     *
     * @param slot index of the slot (ignored if out of bounds)
     */
    public void clearGlobalSlot(int slot) {
        if (slot < 0 || slot >= globalSlots.length) {
            return;
        }
        globalSlots[slot] = null;
        touch();
    }

    /**
     * Updates the {@link #updatedAt} timestamp to {@link Instant#now()}.
     */
    public void touch() {
        this.updatedAt = Instant.now();
    }
}
