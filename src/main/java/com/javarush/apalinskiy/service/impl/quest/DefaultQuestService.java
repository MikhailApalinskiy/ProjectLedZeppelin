package com.javarush.apalinskiy.service.impl.quest;

import com.javarush.apalinskiy.repository.quest.QuestStore;
import com.javarush.apalinskiy.domain.quest.QuestNode;


import java.util.Objects;

public class DefaultQuestService extends AbstractQuestService {
    private final QuestStore store;

    public DefaultQuestService(QuestStore store) {
        this.store = Objects.requireNonNull(store);
    }

    @Override
    public QuestNode getStart() {
        return store.start();
    }

    @Override
    public QuestNode getById(int id) {
        return store.get(id);
    }

    @Override
    public String version() {
        return store.version();
    }

    @Override
    protected QuestNode resolveNext(int fromId, String answer) {
        return store.choose(fromId, answer).orElse(null);
    }
}
