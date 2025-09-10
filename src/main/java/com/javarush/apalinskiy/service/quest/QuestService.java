package com.javarush.apalinskiy.service.quest;

import com.javarush.apalinskiy.domain.quest.QuestNode;
import com.javarush.apalinskiy.domain.quest.choice.ChooseResult;

public interface QuestService {
    QuestNode getStart();

    QuestNode getById(int id);

    ChooseResult choose(int fromId, String answer);

    String version();
}
