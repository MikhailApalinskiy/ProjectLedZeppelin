package com.javarush.apalinskiy.web.view;

import lombok.Getter;

import java.io.Serializable;

@Getter
public final class SlotView implements Serializable {
    private final int index;
    private final Integer nodeId;
    private final String title;
    private final String updatedAtText;
    private final String questId;

    private SlotView(int index, Integer nodeId, String title, String updatedAtText, String questId) {
        this.index = index;
        this.nodeId = nodeId;
        this.title = title;
        this.updatedAtText = updatedAtText;
        this.questId = questId;
    }

    public static SlotView empty(int index, String questId) {
        return new SlotView(index, null, null, null, questId);
    }

    public static SlotView filled(int index, int nodeId, String title, String updatedAtText, String questId) {
        return new SlotView(index, nodeId, title, updatedAtText, questId);
    }
}

