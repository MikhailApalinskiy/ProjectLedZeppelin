package com.javarush.apalinskiy.service.quest;

import com.javarush.apalinskiy.domain.quest.QuestNode;
import com.javarush.apalinskiy.domain.quest.choice.ChooseResult;

/**
 * High-level service for navigating quests.
 * <p>
 * Provides access to the start node, random access by ID,
 * navigation between nodes based on player choices,
 * and version information of the underlying quest.
 * </p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Expose the start node of the quest.</li>
 *   <li>Allow lookup of nodes by ID.</li>
 *   <li>Resolve transitions between nodes using a textual answer.</li>
 *   <li>Provide a version identifier of the quest content.</li>
 * </ul>
 */
public interface QuestService {

    /**
     * Returns the start node of the quest.
     *
     * @return the starting {@link QuestNode}, never {@code null}
     */
    QuestNode getStart();

    /**
     * Returns a quest node by its unique ID.
     *
     * @param id node identifier
     * @return the node if found, or {@code null} if absent
     */
    QuestNode getById(int id);

    /**
     * Resolves the next node based on a choice from a given node.
     * <ul>
     *   <li>If the node does not exist or is a final node, an error is returned.</li>
     *   <li>If the answer is invalid or no such option exists, an error is returned.</li>
     *   <li>Otherwise, returns the result containing the next node.</li>
     * </ul>
     *
     * @param fromId ID of the current node
     * @param answer textual choice provided by the user
     * @return a {@link ChooseResult} describing success (with next node) or failure (with error)
     */
    ChooseResult choose(int fromId, String answer);

    /**
     * Returns a version string that identifies the current state of the quest.
     * <p>
     * The format and semantics of the version are implementation-specific,
     * but typically it is a hash or timestamp indicating content changes.
     * </p>
     *
     * @return quest version identifier (never {@code null})
     */
    String version();
}
