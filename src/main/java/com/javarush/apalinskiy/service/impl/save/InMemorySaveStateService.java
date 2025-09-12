package com.javarush.apalinskiy.service.impl.save;

import com.javarush.apalinskiy.service.save.SaveStateService;
import com.javarush.apalinskiy.domain.save.SaveState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InMemorySaveStateService implements SaveStateService {

    private static final Logger log = LoggerFactory.getLogger(InMemorySaveStateService.class);

    private final Map<String, SaveState> storage = new ConcurrentHashMap<>();

    @Override
    public SaveState getOrCreate(String userId) {
        return storage.computeIfAbsent(userId, id -> {
            log.debug("Creating new SaveState for userId={}", id);
            return new SaveState(id);
        });
    }

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

    @Override
    public void setGlobalSlot(String userId, int slot, String questId, String questName, int nodeId, String title) {
        SaveState st = getOrCreate(userId);
        st.setGlobalSlot(slot, questId, questName, nodeId, title);
        log.debug("setGlobalSlot userId={} slot={} questId={} nodeId={}", userId, slot, questId, nodeId);
    }

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
