package com.javarush.apalinskiy.repository.quest;

import com.javarush.apalinskiy.domain.quest.QuestNode;

import java.util.List;

public interface QuestDraftStore extends QuestStore {

    void replaceNode(QuestNode node);

    void clearDraft(int newStartId);

    void setStartId(int newStartId);

    void reload(List<QuestNode> nodes, int newStartId, boolean markEdited);

    boolean deleteNode(int id);
}
