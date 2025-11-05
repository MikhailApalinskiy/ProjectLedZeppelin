package com.javarush.apalinskiy.domain.quest.index;

import com.javarush.apalinskiy.domain.quest.QuestNode;

import java.util.*;

/**
 * Immutable index that maps quest node identifiers to their corresponding {@link QuestNode} instances.
 *
 * <p>{@code QuestIdIndex} provides fast lookup and validation of quest nodes by ID.
 * It is typically constructed once from a list of nodes when loading or parsing
 * a quest definition and remains immutable afterward.</p>
 *
 * <p>This class ensures data integrity by preventing duplicate node IDs
 * and offers convenience methods to retrieve, validate, and iterate through nodes.</p>
 *
 * <p>This class is thread-safe and designed for concurrent read access.</p>
 */
public final class QuestIdIndex {

    /**
     * Immutable mapping of node IDs to their corresponding quest nodes.
     */
    private final Map<Integer, QuestNode> byId;

    private QuestIdIndex(Map<Integer, QuestNode> byId) {
        this.byId = Map.copyOf(byId);
    }

    /**
     * Builds a {@code QuestIdIndex} from a list of quest nodes.
     *
     * <p>Ensures that all node IDs are unique and non-null. If any duplicate
     * IDs are found, an {@link IllegalStateException} is thrown.</p>
     *
     * @param nodes list of quest nodes (must not be {@code null})
     * @return a fully initialized, immutable {@code QuestIdIndex}
     * @throws NullPointerException  if {@code nodes} or any node in the list is {@code null}
     * @throws IllegalStateException if duplicate node IDs are detected
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
     * Retrieves a quest node by its unique ID.
     *
     * @param id the ID of the node
     * @return the corresponding {@link QuestNode}, or {@code null} if not found
     */
    public QuestNode get(int id) {
        return byId.get(id);
    }

    /**
     * Retrieves a quest node by ID, throwing an exception if not found.
     *
     * @param id the ID of the node to retrieve
     * @return the corresponding {@link QuestNode}
     * @throws IllegalArgumentException if no node with the given ID exists
     */
    public QuestNode require(int id) {
        QuestNode n = byId.get(id);
        if (n == null) {
            throw new IllegalArgumentException("Node not found: id=" + id);
        }
        return n;
    }

    /**
     * Returns the total number of nodes indexed.
     *
     * @return the count of indexed quest nodes
     */
    public int size() {
        return byId.size();
    }

    /**
     * Returns an unmodifiable view of all node IDs contained in this index.
     *
     * @return immutable set of all node identifiers
     */
    public Set<Integer> allIds() {
        return Collections.unmodifiableSet(byId.keySet());
    }
}
