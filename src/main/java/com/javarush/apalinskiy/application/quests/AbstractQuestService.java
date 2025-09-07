package com.javarush.apalinskiy.application.quests;

import com.javarush.apalinskiy.application.dto.ChoiceError;
import com.javarush.apalinskiy.application.dto.ChooseResult;
import com.javarush.apalinskiy.domain.quest.QuestNode;

public abstract class AbstractQuestService implements QuestService {

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

    protected abstract QuestNode resolveNext(int fromId, String answer);
}
