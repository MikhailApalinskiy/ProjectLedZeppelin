package com.javarush.apalinskiy.infra.catalog;

import com.javarush.apalinskiy.application.ports.CustomQuestRepository;
import com.javarush.apalinskiy.quest.CustomQuest;
import com.javarush.apalinskiy.domain.quest.QuestNode;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryCustomQuestRepository implements CustomQuestRepository {

    private final Map<String, CustomQuest> byId = new ConcurrentHashMap<>();

    @Override
    public void create(String ownerLogin, String name, int startId, List<QuestNode> nodes,
                         boolean published, String versionNote) {
        String id = UUID.randomUUID().toString();
        Instant now = Instant.now();
        CustomQuest cq = new CustomQuest(id, ownerLogin, name, startId, nodes, published, versionNote, now, now);
        byId.put(id, cq);
    }

    @Override
    public Optional<CustomQuest> get(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public List<CustomQuest> listAll() {
        return byId.values().stream()
                .sorted(Comparator.comparing(CustomQuest::getUpdatedAt).reversed())
                .toList();
    }

    @Override
    public List<CustomQuest> listByOwner(String ownerLogin) {
        return byId.values().stream()
                .filter(q -> q.getOwnerLogin().equals(ownerLogin))
                .sorted(Comparator.comparing(CustomQuest::getUpdatedAt).reversed())
                .toList();
    }

    @Override
    public void update(String id, int startId, List<QuestNode> nodes, boolean published, String versionNote) {
        byId.computeIfPresent(id, (k, old) -> old.withUpdate(nodes, startId, published, versionNote));
    }

    @Override
    public boolean delete(String id) {
        return byId.remove(id) != null;
    }

    @Override
    public boolean deleteIfOwner(String id, String ownerLogin) {
        CustomQuest q = byId.get(id);
        if (q == null) {
            return false;
        }
        if (!ownerLogin.equals(q.getOwnerLogin())) {
            return false;
        }
        byId.remove(id);
        return true;
    }
}
