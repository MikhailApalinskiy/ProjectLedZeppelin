package com.javarush.apalinskiy.domain.save;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
public class SaveState {

    public static final String MAIN_QUEST_KEY = "main";

    private final String userId;
    private final int slotCount;

    private final Map<String, int[]> slotsByQuest = new ConcurrentHashMap<>();
    private final Map<String, Integer> lastNodeByQuest = new ConcurrentHashMap<>();

    @Getter
    @Setter
    public static class SaveSlot {
        private String questId;
        private String questName;
        private int nodeId;
        private String title;
        private Instant updatedAt;
    }

    private final SaveSlot[] globalSlots;

    private Instant updatedAt = Instant.now();
    private int version = 3; // bumped

    public SaveState(String userId) {
        this(userId, 10);
    }

    public SaveState(String userId, int slotCount) {
        this.userId = userId;
        this.slotCount = Math.max(1, slotCount);
        this.globalSlots = new SaveSlot[this.slotCount];
    }

    public static String questKey(String questIdOrNull) {
        return (questIdOrNull == null || questIdOrNull.isBlank()) ? MAIN_QUEST_KEY : questIdOrNull;
    }

    public SaveSlot getGlobalSlot(int slot) {
        if (slot < 0 || slot >= globalSlots.length) return null;
        return globalSlots[slot];
    }

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

    public void clearGlobalSlot(int slot) {
        if (slot < 0 || slot >= globalSlots.length) return;
        globalSlots[slot] = null;
        touch();
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }
}
