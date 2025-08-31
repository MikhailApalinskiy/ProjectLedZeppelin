package com.javarush.apalinskiy.quest.core;

import com.javarush.apalinskiy.quest.model.Option;
import com.javarush.apalinskiy.quest.model.QuestNode;
import lombok.Getter;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class QuestNavigator {
    private final QuestIdIndex idIndex;
    private final QuestChoiceIndex choiceIndex;
    @Getter
    private final int startId;

    private QuestNavigator(QuestIdIndex idIndex, QuestChoiceIndex choiceIndex, int startId) {
        this.idIndex = Objects.requireNonNull(idIndex);
        this.choiceIndex = Objects.requireNonNull(choiceIndex);
        this.startId = startId;
        idIndex.require(startId);
    }

    public static QuestNavigator from(List<QuestNode> nodes, int startId) {
        QuestIdIndex idIdx = QuestIdIndex.from(nodes);
        for (QuestNode from : nodes) {
            for (Option o : from.getOptions()) {
                Integer next = o.next();
                if (next != null && idIdx.get(next) == null) {
                    throw new IllegalStateException("Broken link: #" + from.getId() + " -> #" + next);
                }
            }
        }
        QuestChoiceIndex choiceIdx = QuestChoiceIndex.from(nodes);
        return new QuestNavigator(idIdx, choiceIdx, startId);
    }

    public QuestNode get(int id) {
        return idIndex.get(id);
    }

    public QuestNode start() {
        return idIndex.require(startId);
    }

    public Optional<QuestNode> choose(int fromId, String answer) {
        Integer nextId = choiceIndex.nextId(fromId, answer);
        return (nextId == null) ? Optional.empty() : Optional.ofNullable(idIndex.get(nextId));
    }
}
