package com.javarush.apalinskiy.domain.quest.choice;

import com.javarush.apalinskiy.domain.quest.QuestNode;
import lombok.Getter;

@Getter
public final class ChooseResult {
    private final boolean ok;
    private final QuestNode next;
    private final ChoiceError error;
    private final String message;

    private ChooseResult(boolean ok, QuestNode next, ChoiceError error, String message) {
        this.ok = ok;
        this.next = next;
        this.error = error;
        this.message = message;
    }

    public static ChooseResult ok(QuestNode next) {
        return new ChooseResult(true, next, null, null);
    }

    public static ChooseResult error(ChoiceError err, String msg) {
        return new ChooseResult(false, null, err, msg);
    }
}

