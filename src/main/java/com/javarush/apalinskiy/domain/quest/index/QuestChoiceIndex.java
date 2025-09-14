package com.javarush.apalinskiy.domain.quest.index;

import com.javarush.apalinskiy.domain.quest.choice.ChoiceNormalizer;
import com.javarush.apalinskiy.domain.quest.Option;
import com.javarush.apalinskiy.domain.quest.QuestNode;

import java.util.*;

/**
 * Index structure for resolving the next node in a quest
 * based on a player's normalized answer.
 * <p>
 * Internally maintains a map of keys in the form
 * {@code "fromNodeId:normalizedAnswer"} → {@code nextNodeId}.
 * <br/>
 * The index is immutable and built from a list of {@link QuestNode}.
 * </p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Builds a lookup map from {@link QuestNode} options using {@link #from(List)}.</li>
 *   <li>Resolves the next node ID from a given node and user input via {@link #nextId(int, String)}.</li>
 *   <li>Ensures no duplicate normalized answers exist within the same node,
 *       otherwise throws {@link IllegalStateException}.</li>
 * </ul>
 *
 * <h3>Normalization</h3>
 * User input is normalized using {@link ChoiceNormalizer#normalize(String)}
 * before lookup, so answers differing only in case/spacing are treated as equal.
 */
public final class QuestChoiceIndex {

    /**
     * Immutable mapping of "fromNodeId:normalizedAnswer" → nextNodeId.
     */
    private final Map<String, Integer> jump;

    /**
     * Constructs a new immutable {@code QuestChoiceIndex}.
     *
     * @param jump prebuilt choice map
     */
    private QuestChoiceIndex(Map<String, Integer> jump) {
        this.jump = Map.copyOf(jump);
    }

    /**
     * Builds a {@code QuestChoiceIndex} from a list of quest nodes.
     * <p>
     * Iterates through all {@link QuestNode#getOptions()} of each node,
     * mapping the normalized choice text to the option's next node ID.
     * </p>
     *
     * @param nodes list of quest nodes (non-null)
     * @return new immutable {@code QuestChoiceIndex}
     * @throws NullPointerException  if {@code nodes} is null
     * @throws IllegalStateException if duplicate normalized answers are found
     *                               within the same node
     */
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
                    throw new IllegalStateException(
                            "Duplicate choice in node #" + from.getId() + " for answer: '" + o.choice() + "'"
                    );
                }
            }
        }
        return new QuestChoiceIndex(m);
    }

    /**
     * Resolves the ID of the next node given a starting node and user input.
     *
     * @param fromId     ID of the current node
     * @param userAnswer raw user input answer (may be null or empty)
     * @return the ID of the next node, or {@code null} if no matching option exists
     */
    public Integer nextId(int fromId, String userAnswer) {
        String norm = ChoiceNormalizer.normalize(userAnswer);
        return jump.get(key(fromId, norm));
    }

    /**
     * Creates a unique key for mapping a choice in a node.
     *
     * @param fromId     ID of the current node
     * @param normAnswer normalized user answer
     * @return concatenated map key
     */
    private static String key(int fromId, String normAnswer) {
        return fromId + ":" + normAnswer;
    }
}
