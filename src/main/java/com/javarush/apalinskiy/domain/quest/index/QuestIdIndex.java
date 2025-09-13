package com.javarush.apalinskiy.domain.quest.index;

import com.javarush.apalinskiy.domain.quest.QuestNode;

import java.util.*;

/**
 * Immutable index for fast lookup of {@link QuestNode} instances by ID.
 * <p>
 * This index is built once from a list of nodes and provides efficient
 * access by node ID, validation of duplicates, and convenient helpers.
 * </p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Ensures all nodes have unique IDs at construction time.</li>
 *   <li>Provides safe and unsafe lookup methods ({@link #get(int)} vs {@link #require(int)}).</li>
 *   <li>Exposes the total number of nodes and all registered IDs.</li>
 * </ul>
 *
 * <h3>Immutability</h3>
 * The internal map is defensively copied and wrapped in an unmodifiable
 * view; the index cannot be modified after creation.
 */
public final class QuestIdIndex {

    /**
     * Immutable mapping of node ID → node.
     */
    private final Map<Integer, QuestNode> byId;

    /**
     * Constructs a new immutable {@code QuestIdIndex}.
     *
     * @param byId prebuilt mapping of node IDs to nodes
     */
    private QuestIdIndex(Map<Integer, QuestNode> byId) {
        this.byId = Map.copyOf(byId);
    }

    /**
     * Builds a {@code QuestIdIndex} from a list of nodes.
     * <p>
     * Validates that all nodes are non-null and have unique IDs.
     * </p>
     *
     * @param nodes list of quest nodes (non-null, elements non-null)
     * @return new immutable {@code QuestIdIndex}
     * @throws NullPointerException  if {@code nodes} or any element is {@code null}
     * @throws IllegalStateException if duplicate node IDs are found
     */
    public static QuestIdIndex from(List<QuestNode> nodes) {
        Objects.requireNonNull(nodes, "nodes");
        Map<Integer, QuestNode> m = new HashMap<>();
        for (QuestNode n : nodes) {
            Objects.requireNonNull(n, "node");
            QuestNode prev = m.put(n.getId(), n);
            if (prev != null) {
                throw new IllegalStateException("Duplicate node id: " + n.getId());
            }
        }
        return new QuestIdIndex(m);
    }

    /**
     * Returns the quest node with the given ID, or {@code null} if not found.
     *
     * @param id node ID
     * @return the {@link QuestNode}, or {@code null} if not present
     */
    public QuestNode get(int id) {
        return byId.get(id);
    }

    /**
     * Returns the quest node with the given ID, throwing if not found.
     *
     * @param id node ID
     * @return the {@link QuestNode}, never {@code null}
     * @throws IllegalArgumentException if the node ID is not present
     */
    public QuestNode require(int id) {
        QuestNode n = byId.get(id);
        if (n == null) {
            throw new IllegalArgumentException("Node not found: id=" + id);
        }
        return n;
    }

    /**
     * Returns the number of nodes in this index.
     *
     * @return total size of the index
     */
    public int size() {
        return byId.size();
    }

    /**
     * Returns an unmodifiable view of all node IDs.
     *
     * @return unmodifiable set of node IDs
     */
    public Set<Integer> allIds() {
        return Collections.unmodifiableSet(byId.keySet());
    }
}
