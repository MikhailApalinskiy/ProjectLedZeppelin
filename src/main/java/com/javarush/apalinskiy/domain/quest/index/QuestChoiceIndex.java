package com.javarush.apalinskiy.domain.quest.index;

import com.javarush.apalinskiy.domain.quest.choice.ChoiceNormalizer;
import com.javarush.apalinskiy.domain.quest.Option;
import com.javarush.apalinskiy.domain.quest.QuestNode;

import java.util.*;

public final class QuestChoiceIndex {
    private final Map<String, Integer> jump;

    private QuestChoiceIndex(Map<String, Integer> jump) {
        this.jump = Map.copyOf(jump);
    }

    public static QuestChoiceIndex from(List<QuestNode> nodes) {
        java.util.Objects.requireNonNull(nodes, "nodes");
        Map<String, Integer> m = new java.util.HashMap<>();
        for (QuestNode from : nodes) {
            for (Option o : from.getOptions()) {
                Integer next = o.next();
                if (next == null) {
                    continue;
                }
                String key = key(from.getId(), o.normalizedChoice());
                Integer prev = m.put(key, next);
                if (prev != null) {
                    throw new IllegalStateException(
                            "Duplicate choice in node #" + from.getId() + " for answer: '" + o.choice() + "'"
                    );
                }
            }
        }
        return new QuestChoiceIndex(m);
    }

    public Integer nextId(int fromId, String userAnswer) {
        String norm = ChoiceNormalizer.normalize(userAnswer);
        return jump.get(key(fromId, norm));
    }

    private static String key(int fromId, String normAnswer) {
        return fromId + ":" + normAnswer;
    }
}
