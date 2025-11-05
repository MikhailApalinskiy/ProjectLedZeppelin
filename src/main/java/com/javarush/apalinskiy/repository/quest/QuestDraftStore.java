package com.javarush.apalinskiy.repository.quest;

import com.javarush.apalinskiy.domain.quest.QuestNode;

import java.util.List;

/**
 * Extended interface of {@link QuestStore} that provides mutation operations
 * for quest editing and draft management.
 *
 * <p>Unlike immutable quest stores used for live gameplay, a {@code QuestDraftStore}
 * supports node replacement, deletion, start node reassignment, and full reload
 * operations for in-progress quest editing sessions.</p>
 *
 * <p>Implementations may store quest data in memory, on disk, or within
 * a persistent database. Thread-safety guarantees depend on the concrete implementation.</p>
 */
public interface QuestDraftStore extends QuestStore {

    /**
     * Replaces or inserts a single quest node within the current draft.
     * If a node with the same ID exists, it is replaced.
     *
     * @param node the node to insert or replace; must not be {@code null}
     */
    void replaceNode(QuestNode node);

    /**
     * Clears all nodes in the draft and resets the quest state.
     *
     * @param newStartId the new starting node ID (usually 0 or placeholder)
     */
    void clearDraft(int newStartId);

    /**
     * Changes the starting node of the draft.
     *
     * @param newStartId new starting node ID
     */
    void setStartId(int newStartId);

    /**
     * Reloads the entire quest draft with a new list of nodes.
     *
     * @param nodes      new quest nodes to load
     * @param newStartId new starting node ID
     * @param markEdited whether the new state should be marked as edited
     */
    void reload(List<QuestNode> nodes, int newStartId, boolean markEdited);

    /**
     * Deletes a specific quest node from the current draft.
     *
     * @param id ID of the node to delete
     * @return {@code true} if the node was found and deleted; {@code false} otherwise
     */
    boolean deleteNode(int id);
}
