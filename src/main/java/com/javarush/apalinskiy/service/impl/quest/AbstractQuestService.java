package com.javarush.apalinskiy.service.impl.quest;

import com.javarush.apalinskiy.domain.quest.choice.ChoiceError;
import com.javarush.apalinskiy.domain.quest.choice.ChooseResult;
import com.javarush.apalinskiy.domain.quest.QuestNode;
import com.javarush.apalinskiy.service.quest.QuestService;

/**
 * Base implementation of {@link QuestService} that provides
 * common logic for handling user choices in quests.
 * <p>
 * This abstract class encapsulates standard validation and error handling
 * for {@link QuestService#choose(int, String)}, delegating only the
 * resolution of the next node to subclasses.
 * </p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Validates that the source node exists and is not final.</li>
 *   <li>Validates that the user's answer is not {@code null} or blank.</li>
 *   <li>Delegates resolution of the next node to {@link #resolveNext(int, String)}.</li>
 *   <li>Returns a {@link ChooseResult} representing either success (next node found)
 *       or error (with a {@link ChoiceError}).</li>
 * </ul>
 *
 * <h3>Errors</h3>
 * <ul>
 *   <li>{@link ChoiceError#NODE_NOT_FOUND} – if the starting node ID is invalid.</li>
 *   <li>{@link ChoiceError#FINAL_NODE} – if the starting node is final and cannot be answered.</li>
 *   <li>{@link ChoiceError#EMPTY_ANSWER} – if the provided answer is {@code null} or blank.</li>
 *   <li>{@link ChoiceError#NO_SUCH_OPTION} – if no option matches the answer.</li>
 * </ul>
 */
abstract class AbstractQuestService implements QuestService {

    /**
     * Handles a user choice in the quest by performing validation
     * and delegating resolution of the next node.
     *
     * @param fromId ID of the current node
     * @param answer raw user input (not yet normalized)
     * @return {@link ChooseResult} with either the next node or an error
     */
    @Override
    public ChooseResult choose(int fromId, String answer) {
        QuestNode from = getById(fromId);
        if (from == null) {
            return ChooseResult.error(ChoiceError.NODE_NOT_FOUND, "Node #" + fromId + " is not found");
        }
        if (from.isFin()) {
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
     * Resolves the next quest node given a current node ID and user answer.
     * <p>
     * Subclasses must implement this method to perform actual
     * option matching and navigation.
     * </p>
     *
     * @param fromId current node ID
     * @param answer user input (may need normalization)
     * @return the next node if found, otherwise {@code null}
     */
    protected abstract QuestNode resolveNext(int fromId, String answer);
}
