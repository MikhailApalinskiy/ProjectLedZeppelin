package com.javarush.apalinskiy.domain.quest.index;

import com.javarush.apalinskiy.domain.quest.choice.ChoiceNormalizer;
import com.javarush.apalinskiy.domain.quest.Option;
import com.javarush.apalinskiy.domain.quest.QuestNode;

import java.util.*;

/**
 * Immutable index that maps normalized player answers to quest node transitions.
 *
 * <p>{@code QuestChoiceIndex} is a precomputed lookup table that associates each
 * normalized player answer (choice text) with the corresponding next node ID.
 * It allows fast and deterministic resolution of quest navigation based on user input.</p>
 *
 * <p>The index is built once from a list of {@link QuestNode} objects and their
 * {@link Option} definitions using {@link #from(List)}.
 * All choice keys are normalized using {@link ChoiceNormalizer} to ensure
 * consistent matching regardless of case or whitespace differences.</p>
 *
 * <p>This class is immutable and thread-safe.</p>
 */
public final class QuestChoiceIndex {

    /**
     * Map of normalized choice keys ("fromId:normalizedAnswer") to target node IDs.
     */
    private final Map<String, Integer> jump;

    private QuestChoiceIndex(Map<String, Integer> jump) {
        this.jump = Map.copyOf(jump);
    }

    /**
     * Builds a new {@code QuestChoiceIndex} from a list of quest nodes.
     *
     * <p>For each node and its {@link Option} list, this method extracts
     * the normalized choice text and associates it with the next node ID.
     * Duplicate normalized choices within the same node cause an {@link IllegalStateException}.</p>
     *
     * @param nodes list of quest nodes to process (must not be {@code null})
     * @return a fully constructed, immutable {@code QuestChoiceIndex}
     * @throws IllegalStateException if duplicate normalized choices are found within a node
     * @throws NullPointerException  if {@code nodes} is {@code null}
     */
    public static QuestChoiceIndex from(List<QuestNode> nodes) {
        Objects.requireNonNull(nodes, "nodes");
        Map<String, Integer> m = new HashMap<>();
        for (QuestNode from : nodes) {
            for (Option o : from.getOptions()) {
                Integer next = o.getNext();
                if (next == null) {
                    continue;
                }
                String key = key(from.getId(), o.normalizedChoice());
                Integer prev = m.put(key, next);
                if (prev != null) {
                    throw new IllegalStateException(
                            "Duplicate choice in node #" + from.getId() + " for answer: '" + o.getChoice() + "'"
                    );
                }
            }
        }
        return new QuestChoiceIndex(m);
    }

    /**
     * Resolves the next node ID based on the player's current node and answer.
     *
     * <p>The input answer is normalized via {@link ChoiceNormalizer#normalize(String)}
     * before lookup. If no matching choice exists, this method returns {@code null}.</p>
     *
     * @param fromId     ID of the current node
     * @param userAnswer raw user input (may be unnormalized)
     * @return the next node ID, or {@code null} if no match is found
     */
    public Integer nextId(int fromId, String userAnswer) {
        String norm = ChoiceNormalizer.normalize(userAnswer);
        return jump.get(key(fromId, norm));
    }

    /**
     * Constructs a unique key for the given node ID and normalized answer.
     *
     * @param fromId     ID of the current node
     * @param normAnswer normalized answer text
     * @return composite key in the format {@code "<fromId>:<normAnswer>"}
     */
    private static String key(int fromId, String normAnswer) {
        return fromId + ":" + normAnswer;
    }
}
