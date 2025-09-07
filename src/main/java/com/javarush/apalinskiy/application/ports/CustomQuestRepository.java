package com.javarush.apalinskiy.application.ports;

import com.javarush.apalinskiy.quest.CustomQuest;
import com.javarush.apalinskiy.domain.quest.QuestNode;

import java.util.List;
import java.util.Optional;

public interface CustomQuestRepository {

    void create(String ownerLogin, String name, int startId, List<QuestNode> nodes,
                  boolean published, String versionNote);

    Optional<CustomQuest> get(String id);

    List<CustomQuest> listAll();

    List<CustomQuest> listByOwner(String ownerLogin);

    void update(String id, int startId, List<QuestNode> nodes,
                boolean published, String versionNote);

    boolean delete(String id);

    boolean deleteIfOwner(String id, String ownerLogin);
}
