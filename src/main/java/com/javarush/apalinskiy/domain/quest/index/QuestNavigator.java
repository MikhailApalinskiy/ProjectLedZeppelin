package com.javarush.apalinskiy.domain.quest.index;

import com.javarush.apalinskiy.domain.quest.Option;
import com.javarush.apalinskiy.domain.quest.QuestNode;
import lombok.Getter;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * High-level navigator that encapsulates quest structure, transitions, and validation.
 *
 * <p>{@code QuestNavigator} combines both {@link QuestIdIndex} and {@link QuestChoiceIndex}
 * to provide fast, consistent navigation between {@link QuestNode} objects based on
 * user input and quest structure.</p>
 *
 * <p>The navigator can operate in two modes:
 * <ul>
 *   <li><b>strict</b> — validates all node links and ensures no broken references;</li>
 *   <li><b>editing</b> — skips strict validation for use in editors or incomplete drafts.</li>
 * </ul>
 * Instances are immutable and thread-safe.</p>
 */
@Getter
public final class QuestNavigator {

    /**
     * Immutable index mapping node IDs to {@link QuestNode} instances.
     */
    private final QuestIdIndex idIndex;

    /**
     * Immutable index mapping node IDs to {@link QuestNode} instances.
     */
    private final QuestChoiceIndex choiceIndex;

    /**
     * Identifier of the starting quest node.
     */
    private final int startId;


    private QuestNavigator(QuestIdIndex idIndex, QuestChoiceIndex choiceIndex, int startId) {
        this.idIndex = Objects.requireNonNull(idIndex, "idIndex");
        this.choiceIndex = Objects.requireNonNull(choiceIndex, "choiceIndex");
        this.startId = startId;
        idIndex.require(startId);
    }

    /**
     * Builds a strict {@code QuestNavigator} instance from the given node list.
     *
     * <p>Performs full structural validation, ensuring all choice links point
     * to existing nodes and the start node is valid.</p>
     *
     * @param nodes   list of quest nodes
     * @param startId ID of the quest’s starting node
     * @return a fully validated {@code QuestNavigator} instance
     * @throws NullPointerException  if {@code nodes} is {@code null}
     * @throws IllegalStateException if any choice points to a missing node
     */
    public static QuestNavigator from(List<QuestNode> nodes, int startId) {
        return build(nodes, startId, true);
    }

    /**
     * Builds a relaxed (non-strict) {@code QuestNavigator} for use in editors.
     *
     * <p>Does not throw an exception for broken links, allowing partial quest
     * structures to be loaded and edited safely.</p>
     *
     * @param nodes   list of quest nodes
     * @param startId ID of the quest’s starting node
     * @return a {@code QuestNavigator} instance without strict validation
     */
    public static QuestNavigator editingFrom(List<QuestNode> nodes, int startId) {
        return build(nodes, startId, false);
    }

    /**
     * Internal builder method used by {@link #from(List, int)} and {@link #editingFrom(List, int)}.
     *
     * @param nodes   quest node list
     * @param startId ID of the starting node
     * @param strict  whether to validate all links
     * @return constructed {@code QuestNavigator}
     */
    private static QuestNavigator build(List<QuestNode> nodes, int startId, boolean strict) {
        Objects.requireNonNull(nodes, "nodes");
        QuestIdIndex idIdx = QuestIdIndex.from(nodes);
        if (strict) {
            for (QuestNode from : nodes) {
                for (Option o : from.getOptions()) {
                    Integer next = o.getNext();
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
     * Retrieves a quest node by ID.
     *
     * @param id quest node ID
     * @return the corresponding {@link QuestNode}, or {@code null} if not found
     */
    public QuestNode get(int id) {
        return idIndex.get(id);
    }

    /**
     * Returns the starting quest node of the quest.
     *
     * @return the {@link QuestNode} corresponding to {@link #startId}
     * @throws IllegalArgumentException if the start node is missing
     */
    public QuestNode start() {
        return idIndex.require(startId);
    }

    /**
     * Resolves the next node based on the player’s current node and answer.
     *
     * <p>The answer text is normalized before lookup. If no valid transition exists,
     * this method returns an empty {@link Optional}.</p>
     *
     * @param fromId ID of the current quest node
     * @param answer user-provided answer text
     * @return an {@link Optional} containing the next node, or empty if no match
     */
    public Optional<QuestNode> choose(int fromId, String answer) {
        Integer nextId = choiceIndex.nextId(fromId, answer);
        return (nextId == null) ? Optional.empty() : Optional.ofNullable(idIndex.get(nextId));
    }

    /**
     * Returns an immutable set of all node IDs available in this quest.
     *
     * @return unmodifiable set of all quest node IDs
     */
    public Set<Integer> allIds() {
        return idIndex.allIds();
    }
}
