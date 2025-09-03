package com.javarush.apalinskiy.save;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
public class SaveState {
    private final String userId;
    private final int[] slots = new int[10];
    private final Map<String, Integer> lastNodeByQuest = new ConcurrentHashMap<>();
    private Instant updatedAt = Instant.now();
    private int version = 1;

    public SaveState(String userId) {
        this.userId = userId;
        Arrays.fill(slots, -1);
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }
}
