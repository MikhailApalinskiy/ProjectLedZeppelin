package com.javarush.apalinskiy.service.impl.save;

import com.javarush.apalinskiy.service.save.SaveStateService;
import com.javarush.apalinskiy.domain.save.SaveState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of {@link SaveStateService}.
 * <p>
 * Stores and manages {@link SaveState} objects for each user, keeping track of
 * global save slots. Data is held only in memory, so it is volatile and will
 * be lost when the application restarts.
 * </p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Create and maintain {@link SaveState} for each user.</li>
 *   <li>Provide access to {@link SaveState.SaveSlot} as {@link GlobalSlot} representations.</li>
 *   <li>Allow setting, clearing, and retrieving global save slots per user.</li>
 *   <li>Log save-state operations for easier debugging and tracing.</li>
 * </ul>
 *
 * <h3>Limitations</h3>
 * <ul>
 *   <li><b>Not suitable for production</b>: no persistence, all state is volatile.</li>
 *   <li>Thread-safety is provided via {@link ConcurrentHashMap} but there is no transactional consistency across operations.</li>
 * </ul>
 */
public class InMemorySaveStateService implements SaveStateService {

    private static final Logger log = LoggerFactory.getLogger(InMemorySaveStateService.class);

    /**
     * In-memory storage of save states by user ID.
     */
    private final Map<String, SaveState> storage = new ConcurrentHashMap<>();

    /**
     * Get an existing {@link SaveState} for the given user, or create a new one if none exists.
     *
     * @param userId the user ID
     * @return the existing or newly created save state
     */
    @Override
    public SaveState getOrCreate(String userId) {
        return storage.computeIfAbsent(userId, id -> {
            log.debug("Creating new SaveState for userId={}", id);
            return new SaveState(id);
        });
    }

    /**
     * Retrieve a global save slot for a user.
     *
     * @param userId the user ID
     * @param slot   the slot index
     * @return the global slot if present, otherwise {@link Optional#empty()}
     */
    @Override
    public Optional<GlobalSlot> getGlobalSlot(String userId, int slot) {
        SaveState st = storage.get(userId);
        if (st == null) {
            log.warn("getGlobalSlot: no SaveState found userId={} slot={}", userId, slot);
            return Optional.empty();
        }
        SaveState.SaveSlot s = st.getGlobalSlot(slot);
        if (s == null) {
            log.debug("getGlobalSlot: empty slot userId={} slot={}", userId, slot);
            return Optional.empty();
        }
        GlobalSlot g = new GlobalSlot(
                slot,
                s.getNodeId(),
                s.getTitle(),
                s.getQuestId(),
                s.getQuestName(),
                s.getUpdatedAt()
        );
        log.debug("getGlobalSlot: found userId={} slot={} questId={} nodeId={}", userId, slot, g.questId(), g.nodeId());
        return Optional.of(g);
    }

    /**
     * Set or overwrite a global save slot for a user.
     *
     * @param userId    the user ID
     * @param slot      the slot index
     * @param questId   the quest ID (may be {@code null} for main quest)
     * @param questName the quest name
     * @param nodeId    the node ID inside the quest
     * @param title     the slot title
     */
    @Override
    public void setGlobalSlot(String userId, int slot, String questId, String questName, int nodeId, String title) {
        SaveState st = getOrCreate(userId);
        st.setGlobalSlot(slot, questId, questName, nodeId, title);
        log.debug("setGlobalSlot userId={} slot={} questId={} nodeId={}", userId, slot, questId, nodeId);
    }

    /**
     * Clear a global save slot for a user if it exists.
     *
     * @param userId the user ID
     * @param slot   the slot index
     */
    @Override
    public void clearGlobalSlot(String userId, int slot) {
        SaveState st = storage.get(userId);
        if (st == null) {
            log.warn("clearGlobalSlot: no SaveState found userId={} slot={}", userId, slot);
            return;
        }
        st.clearGlobalSlot(slot);
        log.debug("clearGlobalSlot userId={} slot={}", userId, slot);
    }
}
