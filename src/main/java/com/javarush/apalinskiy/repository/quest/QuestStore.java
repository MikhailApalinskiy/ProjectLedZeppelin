package com.javarush.apalinskiy.repository.quest;

import com.javarush.apalinskiy.domain.quest.QuestNode;
import com.javarush.apalinskiy.domain.quest.choice.ChoiceNormalizer;

import java.util.List;
import java.util.Optional;

/**
 * Read-only view of a quest repository.
 * <p>
 * Provides access to the quest structure (nodes, start node, navigation),
 * but does not allow modification. Implementations may represent either:
 * <ul>
 *   <li>a published quest (immutable, used in production runtime), or</li>
 *   <li>a snapshot of a draft (read-only representation).</li>
 * </ul>
 * </p>
 *
 * <h3>Contract</h3>
 * <ul>
 *   <li>All node IDs must be unique within a store.</li>
 *   <li>{@link #start()} must return a valid, non-null start node
 *       if the store is initialized, otherwise may return {@code null}.</li>
 *   <li>{@link #choose(int, String)} must respect {@link ChoiceNormalizer} rules
 *       when matching user answers to options.</li>
 * </ul>
 */
public interface QuestStore {

    /**
     * Returns the version identifier of the current quest snapshot.
     * <p>
     * Implementations may use semantic versioning, timestamps, or
     * hash-based identifiers (e.g., SHA-256).
     * </p>
     *
     * @return version string (never {@code null})
     */
    String version();

    /**
     * Returns the start node of the quest.
     *
     * @return start node, or {@code null} if the store is empty
     */
    QuestNode start();

    /**
     * Returns the node with the given ID, or {@code null} if not found.
     *
     * @param id quest node ID
     * @return quest node, or {@code null} if not found
     */
    QuestNode get(int id);

    /**
     * Chooses the next node based on the current node ID and user answer.
     *
     * @param fromId current node ID
     * @param answer raw user input
     * @return optional containing the next node, or empty if no match found
     */
    Optional<QuestNode> choose(int fromId, String answer);

    /**
     * Returns the ID of the start node.
     *
     * @return start node ID
     */
    int startId();

    /**
     * Returns an immutable list of all quest nodes in this store.
     *
     * @return list of nodes (never {@code null})
     */
    List<QuestNode> nodes();
}
