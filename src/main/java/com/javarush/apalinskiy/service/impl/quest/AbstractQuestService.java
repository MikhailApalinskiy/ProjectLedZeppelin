package com.javarush.apalinskiy.service.impl.quest;

import com.javarush.apalinskiy.domain.quest.choice.ChoiceError;
import com.javarush.apalinskiy.domain.quest.choice.ChooseResult;
import com.javarush.apalinskiy.domain.quest.QuestNode;
import com.javarush.apalinskiy.service.quest.QuestService;

/**
 * Abstract base class for quest navigation services implementing {@link QuestService}.
 *
 * <p>This class provides common logic for processing player choices within a quest,
 * including validation of node existence, final-state checks, and empty input handling.</p>
 *
 * <p>Concrete subclasses are responsible for implementing the {@link #resolveNext(int, String)}
 * method, which determines the next quest node based on the current node and user’s answer.</p>
 *
 * <p>All choice results are wrapped in {@link ChooseResult} objects,
 * which encapsulate either a successful transition or a {@link ChoiceError} describing the failure.</p>
 */
abstract class AbstractQuestService implements QuestService {

    /**
     * Handles a player's choice from a given quest node.
     *
     * <p>The method performs validation in the following order:</p>
     * <ol>
     *     <li>Ensures the node exists ({@link ChoiceError#NODE_NOT_FOUND})</li>
     *     <li>Prevents selection from a final node ({@link ChoiceError#FINAL_NODE})</li>
     *     <li>Validates that the answer is not blank ({@link ChoiceError#EMPTY_ANSWER})</li>
     *     <li>Resolves the next node using {@link #resolveNext(int, String)}</li>
     * </ol>
     *
     * <p>If a valid next node is found, a {@code ChooseResult.ok(next)} is returned.
     * Otherwise, an error result with {@link ChoiceError#NO_SUCH_OPTION} is produced.</p>
     *
     * @param fromId ID of the current quest node
     * @param answer user’s selected answer (case-sensitive depending on implementation)
     * @return a {@link ChooseResult} representing success or a specific {@link ChoiceError}
     */
    @Override
    public ChooseResult choose(int fromId, String answer) {
        QuestNode from = getById(fromId);
        if (from == null) {
            return ChooseResult.error(ChoiceError.NODE_NOT_FOUND, "Node #" + fromId + " is not found");
        }
        if (from.getFin()) {
            return ChooseResult.error(ChoiceError.FINAL_NODE, "It's a final node");
        }
        if (answer == null || answer.isBlank()) {
            return ChooseResult.error(ChoiceError.EMPTY_ANSWER, "Empty answer");
        }

        QuestNode next = resolveNext(fromId, answer);
        return (next != null)
                ? ChooseResult.ok(next)
                : ChooseResult.error(ChoiceError.NO_SUCH_OPTION, "No such option");
    }

    /**
     * Resolves the next quest node based on the current node and player's answer.
     *
     * <p>Implementations typically look up the next node via internal navigation maps,
     * indexes, or persistence layers. Returning {@code null} indicates that
     * no matching option was found.</p>
     *
     * @param fromId ID of the current quest node
     * @param answer player's input or selected option
     * @return the resolved {@link QuestNode}, or {@code null} if no valid transition exists
     */
    protected abstract QuestNode resolveNext(int fromId, String answer);
}
