package com.javarush.apalinskiy.web.view;

import lombok.Getter;

import java.io.Serializable;

/**
 * Read-only view model representing a single save slot entry in the UI.
 * <p>
 * Instances are created via factory methods:
 * <ul>
 *   <li>{@link #empty(int, String, String)} — an unoccupied slot.</li>
 *   <li>{@link #filled(int, int, String, String, String, String)} — a slot with an existing save.</li>
 * </ul>
 * The model is intended for presentation (JSP/EL) and does not perform validation.
 * </p>
 *
 * <h3>Fields</h3>
 * <ul>
 *   <li>{@code index} — position of the slot in the user's save list (UI defines 0/1-based semantics).</li>
 *   <li>{@code nodeId} — node identifier where the save points to; {@code null} for an empty slot.</li>
 *   <li>{@code title} — human-readable title/caption for the save; {@code null} for an empty slot.</li>
 *   <li>{@code updatedAtText} — localized/pretty-printed timestamp string; {@code null} for an empty slot.</li>
 *   <li>{@code questId} — identifier of the quest this slot belongs to (never {@code null}).</li>
 *   <li>{@code questName} — display name of the quest (never {@code null}).</li>
 * </ul>
 *
 * <h3>Nullability &amp; invariants</h3>
 * <ul>
 *   <li>Empty slots: {@code nodeId}, {@code title}, {@code updatedAtText} are {@code null}.</li>
 *   <li>Filled slots: {@code nodeId}, {@code title}, {@code updatedAtText} must be non-null.</li>
 *   <li>{@code questId} and {@code questName} should always be provided by the caller.</li>
 * </ul>
 *
 * <p>
 * Note: The class is serializable to support session storage or caching in the web layer.
 * </p>
 */
@Getter
public final class SlotView implements Serializable {

    /**
     * Positional index of the slot in the UI.
     */
    private final int index;
    /**
     * Node id of the saved position; {@code null} for empty slots.
     */
    private final Integer nodeId;
    /**
     * Title/caption of the save; {@code null} for empty slots.
     */
    private final String title;
    /**
     * Localized last-updated text (e.g., "Today 14:32"); {@code null} for empty slots.
     */
    private final String updatedAtText;
    /**
     * Owning quest identifier (never {@code null}).
     */
    private final String questId;
    /**
     * Human-readable quest name (never {@code null}).
     */
    private final String questName;

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
     * Creates an empty (unoccupied) slot.
     *
     * @param index     position of the slot in the UI
     * @param questId   owning quest id (non-null)
     * @param questName owning quest name (non-null)
     * @return empty slot view
     */
    public static SlotView empty(int index, String questId, String questName) {
        return new SlotView(index, null, null, null, questId, questName);
    }

    /**
     * Creates a filled slot with save metadata.
     *
     * @param index         position of the slot in the UI
     * @param nodeId        node id where the save points
     * @param title         human-readable title/caption
     * @param updatedAtText localized/pretty-printed "last updated" text
     * @param questId       owning quest id (non-null)
     * @param questName     owning quest name (non-null)
     * @return filled slot view
     */
    public static SlotView filled(int index, int nodeId, String title, String updatedAtText,
                                  String questId, String questName) {
        return new SlotView(index, nodeId, title, updatedAtText, questId, questName);
    }
}

