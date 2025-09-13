package com.javarush.apalinskiy.domain.quest.index;

import com.javarush.apalinskiy.domain.quest.Option;
import com.javarush.apalinskiy.domain.quest.QuestNode;
import lombok.Getter;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * High-level navigator for traversing a quest graph.
 * <p>
 * Combines an {@link QuestIdIndex} for fast node lookup
 * and a {@link QuestChoiceIndex} for resolving user answers
 * into next nodes.
 * </p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Provides access to nodes by ID ({@link #get(int)}).</li>
 *   <li>Validates and returns the start node ({@link #start()}).</li>
 *   <li>Resolves a player's choice to the next node ({@link #choose(int, String)}).</li>
 *   <li>Exposes all node IDs ({@link #allIds()}).</li>
 * </ul>
 *
 * <h3>Creation modes</h3>
 * <ul>
 *   <li>{@link #from(List, int)} – strict mode: verifies that all option links
 *       point to existing nodes, throwing {@link IllegalStateException} on broken links.</li>
 *   <li>{@link #editingFrom(List, int)} – relaxed mode: allows broken links,
 *       useful when editing an incomplete quest draft.</li>
 * </ul>
 *
 * <p>This class is immutable and thread-safe as long as the underlying nodes are immutable.</p>
 */
@Getter
public final class QuestNavigator {

    /**
     * Index for fast lookup of nodes by ID.
     */
    private final QuestIdIndex idIndex;
    /**
     * Index for resolving choices (answer → next node ID).
     */
    private final QuestChoiceIndex choiceIndex;
    /**
     * ID of the designated start node.
     */
    private final int startId;

    /**
     * Constructs a new {@code QuestNavigator}.
     * <p>
     * Ensures the start node exists in {@code idIndex}.
     * </p>
     *
     * @param idIndex     index of nodes by ID
     * @param choiceIndex index of choices
     * @param startId     ID of the start node
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if the start node is missing
     */
    private QuestNavigator(QuestIdIndex idIndex, QuestChoiceIndex choiceIndex, int startId) {
        this.idIndex = Objects.requireNonNull(idIndex, "idIndex");
        this.choiceIndex = Objects.requireNonNull(choiceIndex, "choiceIndex");
        this.startId = startId;
        idIndex.require(startId);
    }

    /**
     * Builds a strict {@code QuestNavigator}, verifying all option links.
     *
     * @param nodes   list of quest nodes (non-null)
     * @param startId ID of the start node
     * @return a new {@code QuestNavigator} with validation
     * @throws IllegalStateException if a node option refers to a non-existent node
     */
    public static QuestNavigator from(List<QuestNode> nodes, int startId) {
        return build(nodes, startId, true);
    }

    /**
     * Builds a relaxed {@code QuestNavigator}, skipping link validation.
     * <p>
     * Useful when editing a quest draft that may have incomplete links.
     * </p>
     *
     * @param nodes   list of quest nodes (non-null)
     * @param startId ID of the start node
     * @return a new {@code QuestNavigator} without strict validation
     */
    public static QuestNavigator editingFrom(List<QuestNode> nodes, int startId) {
        return build(nodes, startId, false);
    }

    /**
     * Internal builder method with optional strict validation.
     */
    private static QuestNavigator build(List<QuestNode> nodes, int startId, boolean strict) {
        Objects.requireNonNull(nodes, "nodes");
        QuestIdIndex idIdx = QuestIdIndex.from(nodes);
        if (strict) {
            for (QuestNode from : nodes) {
                for (Option o : from.getOptions()) {
                    Integer next = o.next();
                    if (next != null && idIdx.get(next) == null) {
                        throw new IllegalStateException("Broken link: #" + from.getId() + " -> #" + next);
                    }
                }
            }
        }
        QuestChoiceIndex choiceIdx = QuestChoiceIndex.from(nodes);
        return new QuestNavigator(idIdx, choiceIdx, startId);
    }

    /**
     * Returns the node with the given ID, or {@code null} if not found.
     *
     * @param id node ID
     * @return the {@link QuestNode} or {@code null} if absent
     */
    public QuestNode get(int id) {
        return idIndex.get(id);
    }

    /**
     * Returns the start node.
     *
     * @return the start {@link QuestNode}, never {@code null}
     * @throws IllegalArgumentException if the start node does not exist
     */
    public QuestNode start() {
        return idIndex.require(startId);
    }


    /**
     * Resolves a user answer from the given node ID into the next node.
     *
     * @param fromId ID of the current node
     * @param answer raw user input (may be null/blank)
     * @return an {@link Optional} containing the next node, or empty if no match
     */
    public Optional<QuestNode> choose(int fromId, String answer) {
        Integer nextId = choiceIndex.nextId(fromId, answer);
        return (nextId == null) ? Optional.empty() : Optional.ofNullable(idIndex.get(nextId));
    }

    /**
     * Returns an unmodifiable set of all node IDs in this quest.
     *
     * @return all quest node IDs
     */
    public Set<Integer> allIds() {
        return idIndex.allIds();
    }
}
