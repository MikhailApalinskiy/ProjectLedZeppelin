package com.javarush.apalinskiy.repository.quest;

import com.javarush.apalinskiy.domain.quest.QuestNode;

import java.util.List;
import java.util.Optional;

/**
 * Represents a read-only quest data store that provides access
 * to a fully constructed quest graph and its navigation logic.
 *
 * <p>Implementations may represent immutable quest states — such as
 * published quests, in-memory parsed JSON quests, or deserialized
 * database entities. The {@code QuestStore} interface exposes
 * only read and navigation operations, without allowing modification.</p>
 *
 * <p>See {@link com.javarush.apalinskiy.repository.quest.QuestDraftStore}
 * for a mutable variant used in quest editing.</p>
 */
public interface QuestStore {

    /**
     * Returns the current version identifier of this quest store.
     * <p>The version can be a hash, timestamp, or symbolic label that
     * indicates the data’s revision for caching or synchronization purposes.</p>
     *
     * @return version string, never {@code null}
     */
    String version();

    /**
     * Returns the starting node of the quest.
     *
     * @return the start node, or {@code null} if the store is empty
     */
    QuestNode start();

    /**
     * Retrieves a quest node by its unique ID.
     *
     * @param id node identifier
     * @return the corresponding node, or {@code null} if not found
     */
    QuestNode get(int id);

    /**
     * Resolves the next node based on the provided answer from a given node.
     *
     * @param fromId ID of the current node
     * @param answer player's chosen answer text
     * @return optional next node if the answer is valid
     */
    Optional<QuestNode> choose(int fromId, String answer);

    /**
     * Returns the ID of the quest's starting node.
     *
     * @return start node ID
     */
    int startId();

    /**
     * Returns all nodes currently contained in this quest store.
     * <p>The order of nodes is not guaranteed unless specified by implementation.</p>
     *
     * @return list of all quest nodes; never {@code null}
     */
    List<QuestNode> nodes();
}
