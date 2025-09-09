package com.javarush.apalinskiy.infra.catalog;

import com.javarush.apalinskiy.application.ports.CustomQuestRepository;
import com.javarush.apalinskiy.quest.CustomQuest;
import com.javarush.apalinskiy.domain.quest.QuestNode;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryCustomQuestRepository implements CustomQuestRepository {

    private final Map<String, CustomQuest> byId = new ConcurrentHashMap<>();
    private final Map<String, PendingNew> stagedNew = new ConcurrentHashMap<>();
    private final Map<String, PendingEdit> stagedEdit = new ConcurrentHashMap<>();

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

    @Override
    public void stageCreate(String ownerLogin, String name, int startId, List<QuestNode> nodes, String versionNote) {
        String pendingId = "new-" + UUID.randomUUID();
        stagedNew.put(pendingId, new PendingNew(
                pendingId, ownerLogin, name, startId, List.copyOf(nodes), versionNote, Instant.now()
        ));
    }

    @Override
    public List<PendingNew> listPendingNew() {
        return stagedNew.values().stream()
                .sorted(Comparator.comparing(PendingNew::getSubmittedAt).reversed())
                .toList();
    }

    @Override
    public String approveCreate(String pendingId) {
        PendingNew pn = stagedNew.remove(pendingId);
        if (pn == null) {
            throw new NoSuchElementException("Pending new quest not found: " + pendingId);
        }
        String newId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        byId.put(newId, new CustomQuest(
                newId, pn.getOwnerLogin(), pn.getName(), pn.getStartId(),
                pn.getNodes(), true, pn.getVersionNote(), now, now
        ));
        return newId;
    }

    @Override
    public void rejectCreate(String pendingId) {
        if (stagedNew.remove(pendingId) == null) {
            throw new NoSuchElementException("Pending new quest not found: " + pendingId);
        }
    }

    @Override
    public void stageEdit(String questId, int startId, List<QuestNode> nodes, String versionNote) {
        CustomQuest live = byId.get(questId);
        if (live == null) {
            throw new NoSuchElementException("Quest not found: " + questId);
        }
        stagedEdit.put(questId, new PendingEdit(
                questId, live.getOwnerLogin(), live.getName(),
                startId, List.copyOf(nodes), versionNote, Instant.now()
        ));
    }

    @Override
    public List<PendingEdit> listPendingEdits() {
        return stagedEdit.values().stream()
                .sorted(Comparator.comparing(PendingEdit::getSubmittedAt).reversed())
                .toList();
    }

    @Override
    public boolean hasPendingEdit(String questId) {
        return stagedEdit.containsKey(questId);
    }

    @Override
    public void approveEdit(String questId) {
        PendingEdit pe = stagedEdit.remove(questId);
        if (pe == null) {
            throw new NoSuchElementException("Pending edit not found: " + questId);
        }
        byId.computeIfPresent(questId, (k, old) ->
                old.withUpdate(pe.getNodes(), pe.getStartId(), old.isPublished(), pe.getVersionNote())
        );
    }

    @Override
    public void rejectEdit(String questId) {
        if (stagedEdit.remove(questId) == null) {
            throw new NoSuchElementException("Pending edit not found: " + questId);
        }
    }
}
