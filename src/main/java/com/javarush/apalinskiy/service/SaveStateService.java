package com.javarush.apalinskiy.service;

import com.javarush.apalinskiy.save.SaveState;

import java.util.Optional;

public interface SaveStateService {

    SaveState getOrCreate(String userId);

    Optional<Integer> getSlot(String userId, int slot);

    void setSlot(String userId, int slot, int nodeId);

    void clearSlot(String userId, int slot);

    Optional<Integer> getQuestNode(String userId, String questId);

    void setQuestNode(String userId, String questId, int nodeId);

    void clearQuestNode(String userId, String questId);
}
