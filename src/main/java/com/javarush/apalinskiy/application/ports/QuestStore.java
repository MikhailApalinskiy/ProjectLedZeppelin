package com.javarush.apalinskiy.application.ports;

import com.javarush.apalinskiy.domain.quest.QuestNode;

import java.util.List;
import java.util.Optional;

public interface QuestStore {

    String version();

    QuestNode start();

    QuestNode get(int id);

    Optional<QuestNode> choose(int fromId, String answer);

    int startId();

    List<QuestNode> nodes();
}
