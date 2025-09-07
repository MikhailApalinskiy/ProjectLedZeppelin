package com.javarush.apalinskiy.application.quests;

import com.javarush.apalinskiy.domain.quest.QuestNode;
import com.javarush.apalinskiy.application.dto.ChooseResult;

public interface QuestService {
    QuestNode getStart();

    QuestNode getById(int id);

    ChooseResult choose(int fromId, String answer);

    String version();
}
