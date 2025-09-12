package com.javarush.apalinskiy.repository.inmemory.quest;

import com.javarush.apalinskiy.repository.quest.CustomQuestRepository;
import com.javarush.apalinskiy.domain.quest.custom.CustomQuest;
import com.javarush.apalinskiy.domain.quest.QuestNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryCustomQuestRepository implements CustomQuestRepository {

    private static final Logger log = LoggerFactory.getLogger(InMemoryCustomQuestRepository.class);

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
        log.debug("Created custom quest id={} owner={} name='{}' nodes={} published={}",
                id, ownerLogin, name, nodes.size(), published);
    }

    @Override
    public Optional<CustomQuest> get(String id) {
        Optional<CustomQuest> res = Optional.ofNullable(byId.get(id));
        log.debug("Get quest id={} found={}", id, res.isPresent());
        return res;
    }

    @Override
    public List<CustomQuest> listAll() {
        List<CustomQuest> list = byId.values().stream()
                .sorted(Comparator.comparing(CustomQuest::getUpdatedAt).reversed())
                .toList();
        log.debug("List all quests size={}", list.size());
        return list;
    }

    @Override
    public List<CustomQuest> listByOwner(String ownerLogin) {
        List<CustomQuest> list = byId.values().stream()
                .filter(q -> q.getOwnerLogin().equals(ownerLogin))
                .sorted(Comparator.comparing(CustomQuest::getUpdatedAt).reversed())
                .toList();
        log.debug("List quests by owner={} size={}", ownerLogin, list.size());
        return list;
    }

    @Override
    public void update(String id, int startId, List<QuestNode> nodes, boolean published, String versionNote) {
        boolean existed = byId.containsKey(id);
        byId.computeIfPresent(id, (k, old) -> old.withUpdate(nodes, startId, published, versionNote));
        if (existed) {
            log.debug("Updated quest id={} nodes={} startId={} published={}", id, nodes.size(), startId, published);
        } else {
            log.warn("Update skipped: quest not found id={}", id);
        }
    }

    @Override
    public boolean delete(String id) {
        boolean removed = byId.remove(id) != null;
        if (removed) {
            log.debug("Deleted quest id={}", id);
        } else {
            log.warn("Delete skipped: quest not found id={}", id);
        }
        return removed;
    }

    @Override
    public boolean deleteIfOwner(String id, String ownerLogin) {
        CustomQuest q = byId.get(id);
        if (q == null) {
            log.warn("Delete-if-owner skipped: quest not found id={}", id);
            return false;
        }
        if (!ownerLogin.equals(q.getOwnerLogin())) {
            log.warn("Delete-if-owner denied: owner mismatch id={} owner={}", id, ownerLogin);
            return false;
        }
        byId.remove(id);
        log.debug("Deleted quest by owner id={} owner={}", id, ownerLogin);
        return true;
    }

    @Override
    public void stageCreate(String ownerLogin, String name, int startId, List<QuestNode> nodes, String versionNote) {
        String pendingId = "new-" + UUID.randomUUID();
        stagedNew.put(pendingId, new PendingNew(
                pendingId, ownerLogin, name, startId, List.copyOf(nodes), versionNote, Instant.now()
        ));
        log.debug("Staged CREATE pendingId={} owner={} name='{}' nodes={} startId={}",
                pendingId, ownerLogin, name, nodes.size(), startId);
    }

    @Override
    public List<PendingNew> listPendingNew() {
        List<PendingNew> list = stagedNew.values().stream()
                .sorted(Comparator.comparing(PendingNew::getSubmittedAt).reversed())
                .toList();
        log.debug("List pending NEW size={}", list.size());
        return list;
    }

    @Override
    public String approveCreate(String pendingId) {
        PendingNew pn = stagedNew.remove(pendingId);
        if (pn == null) {
            log.warn("Approve CREATE failed: pending not found pendingId={}", pendingId);
            throw new NoSuchElementException("Pending new quest not found: " + pendingId);
        }
        String newId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        byId.put(newId, new CustomQuest(
                newId, pn.getOwnerLogin(), pn.getName(), pn.getStartId(),
                pn.getNodes(), true, pn.getVersionNote(), now, now
        ));
        log.debug("Approved CREATE pendingId={} -> newId={} owner={} name='{}'",
                pendingId, newId, pn.getOwnerLogin(), pn.getName());
        return newId;
    }

    @Override
    public void rejectCreate(String pendingId) {
        if (stagedNew.remove(pendingId) == null) {
            log.warn("Reject CREATE failed: pending not found pendingId={}", pendingId);
            throw new NoSuchElementException("Pending new quest not found: " + pendingId);
        }
        log.debug("Rejected CREATE pendingId={}", pendingId);
    }

    @Override
    public void stageEdit(String questId, int startId, List<QuestNode> nodes, String versionNote) {
        CustomQuest live = byId.get(questId);
        if (live == null) {
            log.warn("Stage EDIT failed: quest not found id={}", questId);
            throw new NoSuchElementException("Quest not found: " + questId);
        }
        stagedEdit.put(questId, new PendingEdit(
                questId, live.getOwnerLogin(), live.getName(),
                startId, List.copyOf(nodes), versionNote, Instant.now()
        ));
        log.debug("Staged EDIT questId={} owner={} name='{}' nodes={} startId={}",
                questId, live.getOwnerLogin(), live.getName(), nodes.size(), startId);
    }

    @Override
    public List<PendingEdit> listPendingEdits() {
        List<PendingEdit> list = stagedEdit.values().stream()
                .sorted(Comparator.comparing(PendingEdit::getSubmittedAt).reversed())
                .toList();
        log.debug("List pending EDIT size={}", list.size());
        return list;
    }

    @Override
    public void approveEdit(String questId) {
        PendingEdit pe = stagedEdit.remove(questId);
        if (pe == null) {
            log.warn("Approve EDIT failed: pending not found questId={}", questId);
            throw new NoSuchElementException("Pending edit not found: " + questId);
        }
        byId.computeIfPresent(questId, (k, old) ->
                old.withUpdate(pe.getNodes(), pe.getStartId(), old.isPublished(), pe.getVersionNote())
        );
        log.debug("Approved EDIT questId={} nodes={} startId={}", questId, pe.getNodes().size(), pe.getStartId());
    }

    @Override
    public void rejectEdit(String questId) {
        if (stagedEdit.remove(questId) == null) {
            log.warn("Reject EDIT failed: pending not found questId={}", questId);
            throw new NoSuchElementException("Pending edit not found: " + questId);
        }
        log.debug("Rejected EDIT questId={}", questId);
    }
}
