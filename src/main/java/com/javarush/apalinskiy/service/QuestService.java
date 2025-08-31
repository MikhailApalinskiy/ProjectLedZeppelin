package com.javarush.apalinskiy.service;

import com.javarush.apalinskiy.quest.model.QuestNode;
import com.javarush.apalinskiy.service.dto.ChooseResult;

public interface QuestService {
    QuestNode getStart();

    QuestNode getById(int id);

    ChooseResult choose(int fromId, String answer);

    String version();
}
