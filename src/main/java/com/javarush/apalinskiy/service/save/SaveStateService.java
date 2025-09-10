package com.javarush.apalinskiy.service.save;

import com.javarush.apalinskiy.domain.save.SaveState;

import java.time.Instant;
import java.util.Optional;

public interface SaveStateService {

    SaveState getOrCreate(String userId);

    record GlobalSlot(int index, Integer nodeId, String title, String questId, String questName, Instant updatedAt) {
    }

    Optional<GlobalSlot> getGlobalSlot(String userId, int slot);

    void setGlobalSlot(String userId, int slot, String questId, String questName, int nodeId, String title);

    void clearGlobalSlot(String userId, int slot);
}
