package com.javarush.apalinskiy.web.view;

import lombok.Getter;

import java.io.Serializable;

/**
 * Represents a single save slot view in the user’s save-state interface.
 * <p>
 * A {@code SlotView} holds minimal display data about a saved quest position,
 * including the quest ID, node ID, title, and timestamp text.
 * Instances are immutable and serializable.
 */
@Getter
public final class SlotView implements Serializable {

    /**
     * Zero-based slot index within the user’s save state.
     */
    private final int index;

    /**
     * Current quest node ID, or {@code null} if the slot is empty.
     */
    private final Integer nodeId;

    /**
     * Optional node title or caption.
     */
    private final String title;

    /**
     * Human-readable “last updated” timestamp text.
     */
    private final String updatedAtText;

    /**
     * Associated quest identifier.
     */
    private final String questId;

    /**
     * Associated quest display name.
     */
    private final String questName;

    /**
     * Constructs a new immutable slot view.
     *
     * @param index         slot index
     * @param nodeId        node ID, may be {@code null} for empty slot
     * @param title         optional node title
     * @param updatedAtText formatted update timestamp
     * @param questId       quest identifier
     * @param questName     quest display name
     */
    private SlotView(int index,
                     Integer nodeId,
                     String title,
                     String updatedAtText,
                     String questId,
                     String questName) {
        this.index = index;
        this.nodeId = nodeId;
        this.title = title;
        this.updatedAtText = updatedAtText;
        this.questId = questId;
        this.questName = questName;
    }

    /**
     * Creates an empty slot representation with no saved node.
     *
     * @param index     slot index
     * @param questId   quest identifier
     * @param questName quest display name
     * @return an empty {@code SlotView} instance
     */
    public static SlotView empty(int index, String questId, String questName) {
        return new SlotView(index, null, null, null, questId, questName);
    }

    /**
     * Creates a filled slot representation with stored node data.
     *
     * @param index         slot index
     * @param nodeId        saved quest node ID
     * @param title         node title
     * @param updatedAtText formatted update timestamp
     * @param questId       quest identifier
     * @param questName     quest display name
     * @return a filled {@code SlotView} instance
     */
    public static SlotView filled(int index, int nodeId, String title, String updatedAtText,
                                  String questId, String questName) {
        return new SlotView(index, nodeId, title, updatedAtText, questId, questName);
    }
}

