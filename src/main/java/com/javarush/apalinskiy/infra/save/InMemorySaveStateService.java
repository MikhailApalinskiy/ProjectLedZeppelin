package com.javarush.apalinskiy.infra.save;

import com.javarush.apalinskiy.application.save.SaveStateService;
import com.javarush.apalinskiy.domain.save.SaveState;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InMemorySaveStateService implements SaveStateService {

    private final Map<String, SaveState> storage = new ConcurrentHashMap<>();

    @Override
    public SaveState getOrCreate(String userId) {
        return storage.computeIfAbsent(userId, SaveState::new);
    }

    @Override
    public Optional<GlobalSlot> getGlobalSlot(String userId, int slot) {
        SaveState st = storage.get(userId);
        if (st == null) {
            return Optional.empty();
        }
        SaveState.SaveSlot s = st.getGlobalSlot(slot);
        if (s == null) {
            return Optional.empty();
        }
        return Optional.of(new GlobalSlot(
                slot,
                s.getNodeId(),
                s.getTitle(),
                s.getQuestId(),
                s.getQuestName(),
                s.getUpdatedAt()
        ));
    }

    @Override
    public void setGlobalSlot(String userId, int slot, String questId, String questName, int nodeId, String title) {
        SaveState st = getOrCreate(userId);
        st.setGlobalSlot(slot, questId, questName, nodeId, title);
    }

    @Override
    public void clearGlobalSlot(String userId, int slot) {
        SaveState st = storage.get(userId);
        if (st == null) {
            return;
        }
        st.clearGlobalSlot(slot);
    }
}
