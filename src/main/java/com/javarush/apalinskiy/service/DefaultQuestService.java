package com.javarush.apalinskiy.service;

import com.javarush.apalinskiy.quest.model.QuestNode;
import com.javarush.apalinskiy.repositories.QuestRepository;
import com.javarush.apalinskiy.service.dto.ChoiceError;
import com.javarush.apalinskiy.service.dto.ChooseResult;

import java.util.Objects;

public class DefaultQuestService implements QuestService {
    private final QuestRepository repo;

    public DefaultQuestService(QuestRepository repo) {
        this.repo = Objects.requireNonNull(repo);
    }

    @Override
    public QuestNode getStart() {
        return repo.start();
    }

    @Override
    public QuestNode getById(int id) {
        return repo.get(id);
    }

    @Override
    public String version() {
        return repo.version();
    }

    @Override
    public ChooseResult choose(int fromId, String answer) {
        QuestNode from = repo.get(fromId);
        if (from == null) {
            return ChooseResult.error(ChoiceError.NODE_NOT_FOUND, "Узел #" + fromId + " не найден");
        }
        if (from.isFin()) {
            return ChooseResult.error(ChoiceError.FINAL_NODE, "Это финальная ветка");
        }
        if (answer == null || answer.isBlank()) {
            return ChooseResult.error(ChoiceError.EMPTY_ANSWER, "Пустой ответ");
        }
        return repo.choose(fromId, answer)
                .map(ChooseResult::ok)
                .orElseGet(() -> ChooseResult.error(ChoiceError.NO_SUCH_OPTION, "Нет такого варианта ответа"));
    }
}
