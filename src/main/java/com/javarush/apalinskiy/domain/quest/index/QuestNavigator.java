package com.javarush.apalinskiy.domain.quest.index;

import com.javarush.apalinskiy.domain.quest.Option;
import com.javarush.apalinskiy.domain.quest.QuestNode;
import lombok.Getter;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Getter
public final class QuestNavigator {
    private final QuestIdIndex idIndex;
    private final QuestChoiceIndex choiceIndex;
    private final int startId;

    private QuestNavigator(QuestIdIndex idIndex, QuestChoiceIndex choiceIndex, int startId) {
        this.idIndex = Objects.requireNonNull(idIndex, "idIndex");
        this.choiceIndex = Objects.requireNonNull(choiceIndex, "choiceIndex");
        this.startId = startId;
        idIndex.require(startId);
    }

    public static QuestNavigator from(List<QuestNode> nodes, int startId) {
        return build(nodes, startId, true);
    }

    public static QuestNavigator editingFrom(List<QuestNode> nodes, int startId) {
        return build(nodes, startId, false);
    }

    private static QuestNavigator build(List<QuestNode> nodes, int startId, boolean strict) {
        Objects.requireNonNull(nodes, "nodes");
        QuestIdIndex idIdx = QuestIdIndex.from(nodes);
        if (strict) {
            for (QuestNode from : nodes) {
                for (Option o : from.getOptions()) {
                    Integer next = o.next();
                    if (next != null && idIdx.get(next) == null) {
                        throw new IllegalStateException("Broken link: #" + from.getId() + " -> #" + next);
                    }
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

    public Set<Integer> allIds() {
        return idIndex.allIds();
    }
}
