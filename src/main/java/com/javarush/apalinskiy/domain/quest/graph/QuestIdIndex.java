package com.javarush.apalinskiy.domain.quest.graph;

import com.javarush.apalinskiy.domain.quest.QuestNode;

import java.util.*;

public final class QuestIdIndex {
    private final Map<Integer, QuestNode> byId;

    private QuestIdIndex(Map<Integer, QuestNode> byId) {
        this.byId = Map.copyOf(byId);
    }

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

    public QuestNode get(int id) {
        return byId.get(id);
    }

    public QuestNode require(int id) {
        QuestNode n = byId.get(id);
        if (n == null) {
            throw new IllegalArgumentException("Node not found: id=" + id);
        }
        return n;
    }

    public int size() {
        return byId.size();
    }

    public Set<Integer> allIds() {
        return Collections.unmodifiableSet(byId.keySet());
    }
}
