package com.javarush.apalinskiy.quest.core;

import com.javarush.apalinskiy.quest.model.Option;
import com.javarush.apalinskiy.quest.model.QuestNode;

import java.util.*;
import java.util.regex.Pattern;

public final class QuestChoiceIndex {
    private static final Pattern WS = Pattern.compile("\\s+");
    private final Map<String, Integer> jump;

    private QuestChoiceIndex(Map<String, Integer> jump) {
        this.jump = Map.copyOf(jump);
    }

    public static QuestChoiceIndex from(List<QuestNode> nodes) {
        Objects.requireNonNull(nodes, "nodes");
        Map<String, Integer> m = new HashMap<>();
        for (QuestNode from : nodes) {
            for (Option o : from.getOptions()) {
                Integer next = o.next();
                if (next == null) {
                    continue;
                }
                String key = key(from.getId(), o.normalizedChoice());
                Integer prev = m.put(key, next);
                if (prev != null) {
                    throw new IllegalStateException("Duplicate choice in node #" + from.getId()
                            + " for answer: '" + o.choice() + "'");
                }
            }
        }
        return new QuestChoiceIndex(m);
    }

    public Integer nextId(int fromId, String userAnswer) {
        String norm = normalize(userAnswer);
        return jump.get(key(fromId, norm));
    }

    private static String key(int fromId, String normAnswer) {
        return fromId + ":" + normAnswer;
    }

    private static String normalize(String s) {
        if (s == null) return "";
        return WS.matcher(s).replaceAll(" ").trim().toLowerCase(java.util.Locale.ROOT);
    }
}
