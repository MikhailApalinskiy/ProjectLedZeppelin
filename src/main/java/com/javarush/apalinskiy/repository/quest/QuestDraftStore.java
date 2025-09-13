package com.javarush.apalinskiy.repository.quest;

import com.javarush.apalinskiy.domain.quest.QuestNode;

import java.util.List;

/**
 * Extension of {@link QuestStore} that supports editing and draft management.
 * <p>
 * Unlike {@link QuestStore}, which is typically read-only (for published/live quests),
 * this interface allows modifications such as adding, replacing, or deleting nodes.
 * </p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Maintain a mutable "draft" version of a quest.</li>
 *   <li>Allow replacing or removing nodes, and changing the start node.</li>
 *   <li>Support complete reloading of the quest structure.</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <p>
 * Implementations are expected to be used in authoring tools,
 * while {@link QuestStore} implementations are used in production runtime.
 * </p>
 */
public interface QuestDraftStore extends QuestStore {

    /**
     * Replaces or adds a node in the draft.
     * If a node with the same ID exists, it is replaced.
     *
     * @param node quest node to add or replace
     */
    void replaceNode(QuestNode node);

    /**
     * Clears the current draft and resets the start node ID.
     *
     * @param newStartId new start node ID to use for the next draft
     */
    void clearDraft(int newStartId);

    /**
     * Sets a new start node ID for the draft.
     *
     * @param newStartId new start node ID
     */
    void setStartId(int newStartId);

    /**
     * Reloads the draft with a new set of nodes.
     *
     * @param nodes      new quest nodes
     * @param newStartId new start node ID
     * @param markEdited whether the draft should be flagged as "edited"
     */
    void reload(List<QuestNode> nodes, int newStartId, boolean markEdited);

    /**
     * Deletes a node from the draft, if it exists.
     *
     * @param id ID of the node to delete
     * @return true if the node was removed, false otherwise
     */
    boolean deleteNode(int id);
}
