package com.javarush.apalinskiy.service.quest;

import com.javarush.apalinskiy.domain.quest.QuestNode;
import com.javarush.apalinskiy.domain.quest.choice.ChooseResult;

/**
 * Core service interface that defines the runtime behavior of a quest instance.
 *
 * <p>Implementations of this interface provide logic for navigating between
 * quest nodes, evaluating player choices, and maintaining quest versioning.
 * The interface is typically used by the gameplay engine during interactive
 * sessions.</p>
 */
public interface QuestService {

    /**
     * Returns the starting node of the quest (entry point).
     *
     * @return {@link QuestNode} representing the first node of the quest
     */
    QuestNode getStart();

    /**
     * Retrieves a quest node by its numeric identifier.
     *
     * @param id node identifier
     * @return {@link QuestNode} with the specified id, or {@code null} if not found
     */
    QuestNode getById(int id);

    /**
     * Processes a player choice from the given node and returns the result.
     *
     * <p>The method determines the next node based on the provided answer,
     * applies any node-specific logic, and encapsulates the outcome in a
     * {@link ChooseResult} object.</p>
     *
     * @param fromId identifier of the current node
     * @param answer player-provided answer or choice text
     * @return result of the choice evaluation, including next node and status
     */
    ChooseResult choose(int fromId, String answer);

    /**
     * Returns a stable version string identifying the quest’s content version.
     *
     * <p>This is typically a hash or revision tag that can be used for
     * consistency checks, caching, or multiplayer synchronization.</p>
     *
     * @return version identifier string (e.g., {@code sha256:...})
     */
    String version();
}
