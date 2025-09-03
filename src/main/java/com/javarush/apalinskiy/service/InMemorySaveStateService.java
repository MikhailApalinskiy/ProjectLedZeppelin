package com.javarush.apalinskiy.service;

import com.javarush.apalinskiy.save.SaveState;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemorySaveStateService implements SaveStateService {

    private final Map<String, SaveState> storage = new ConcurrentHashMap<>();

    @Override
    public SaveState getOrCreate(String userId) {
        return storage.computeIfAbsent(userId, SaveState::new);
    }

    @Override
    public Optional<Integer> getSlot(String userId, int slot) {
        SaveState st = storage.get(userId);
        if (st == null || slot < 0 || slot >= st.getSlots().length) {
            return Optional.empty();
        }
        int val = st.getSlots()[slot];
        return (val < 0) ? Optional.empty() : Optional.of(val);
    }

    @Override
    public void setSlot(String userId, int slot, int nodeId) {
        SaveState st = getOrCreate(userId);
        if (slot < 0 || slot >= st.getSlots().length) {
            throw new IllegalArgumentException("slot out of bounds: " + slot);
        }
        st.getSlots()[slot] = nodeId;
        st.touch();
    }

    @Override
    public void clearSlot(String userId, int slot) {
        SaveState st = storage.get(userId);
        if (st == null || slot < 0 || slot >= st.getSlots().length) {
            return;
        }
        st.getSlots()[slot] = -1;
        st.touch();
    }

    @Override
    public Optional<Integer> getQuestNode(String userId, String questId) {
        SaveState st = storage.get(userId);
        if (st == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(st.getLastNodeByQuest().get(questId));
    }

    @Override
    public void setQuestNode(String userId, String questId, int nodeId) {
        SaveState st = getOrCreate(userId);
        st.getLastNodeByQuest().put(questId, nodeId);
        st.touch();
    }

    @Override
    public void clearQuestNode(String userId, String questId) {
        SaveState st = storage.get(userId);
        if (st == null) {
            return;
        }
        st.getLastNodeByQuest().remove(questId);
        st.touch();
    }
}
