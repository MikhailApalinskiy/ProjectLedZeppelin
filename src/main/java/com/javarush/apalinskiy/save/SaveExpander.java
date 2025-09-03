package com.javarush.apalinskiy.save;

import com.javarush.apalinskiy.quest.model.QuestNode;
import com.javarush.apalinskiy.service.QuestService;
import com.javarush.apalinskiy.service.SaveStateService;

import java.util.Objects;

public class SaveExpander {

    private final QuestService questService;
    private final SaveStateService saveStateService;

    public SaveExpander(QuestService questService, SaveStateService saveStateService) {
        this.questService = Objects.requireNonNull(questService);
        this.saveStateService = Objects.requireNonNull(saveStateService);
    }

    public ExpandedNode expandFromSlot(String userId, int slot) {
        int nodeId = saveStateService.getSlot(userId, slot).orElseGet(this::startId);
        return expandNodeId(nodeId);
    }

    public ExpandedNode expandFromQuest(String userId, String questId) {
        int nodeId = saveStateService.getQuestNode(userId, questId).orElseGet(this::startId);
        return expandNodeId(nodeId);
    }

    public ExpandedNode expandNodeId(int nodeId) {
        QuestNode node = questService.getById(nodeId);
        if (node == null) {
            int sid = startId();
            QuestNode start = questService.getById(sid);
            if (start == null) {
                throw new IllegalStateException("Start node not found (id=" + sid + ")");
            }
            return new ExpandedNode(sid, start);
        }
        return new ExpandedNode(nodeId, node);
    }

    private int startId() {
        QuestNode s = questService.getStart();
        if (s == null) {
            throw new IllegalStateException("QuestService.getStart() returned null");
        }
        return s.getId();
    }

    public record ExpandedNode(int nodeId, QuestNode node) {
    }
}
