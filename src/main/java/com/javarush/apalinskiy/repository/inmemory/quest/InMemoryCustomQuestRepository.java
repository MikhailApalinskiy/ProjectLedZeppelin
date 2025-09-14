package com.javarush.apalinskiy.repository.inmemory.quest;

import com.javarush.apalinskiy.repository.quest.CustomQuestRepository;
import com.javarush.apalinskiy.domain.quest.custom.CustomQuest;
import com.javarush.apalinskiy.domain.quest.QuestNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory, thread-safe implementation of {@link CustomQuestRepository}.
 * <p>
 * Stores quests and moderation staging data in {@link ConcurrentHashMap}s.
 * Ordering in {@code listAll}/{@code listByOwner} is by {@code updatedAt} DESC.
 * This repository is non-persistent and intended for tests/dev.
 * </p>
 *
 * @implNote All mutations are atomic per key via map operations; no cross-key transactions.
 */
public class InMemoryCustomQuestRepository implements CustomQuestRepository {

    private static final Logger log = LoggerFactory.getLogger(InMemoryCustomQuestRepository.class);

    private final Map<String, CustomQuest> byId = new ConcurrentHashMap<>();
    private final Map<String, PendingNew> stagedNew = new ConcurrentHashMap<>();
    private final Map<String, PendingEdit> stagedEdit = new ConcurrentHashMap<>();

    /**
     * Creates and stores a new quest with a generated id and {@code createdAt}/{@code updatedAt}=now.
     *
     * @param ownerId     owner user id
     * @param name        quest name
     * @param startId     start node id
     * @param nodes       quest nodes
     * @param published   initial published flag
     * @param versionNote version/comment label
     */
    @Override
    public void create(String ownerId, String name, int startId, List<QuestNode> nodes,
                       boolean published, String versionNote) {
        String id = UUID.randomUUID().toString();
        Instant now = Instant.now();
        CustomQuest cq = new CustomQuest(id, ownerId, name, startId, nodes, published, versionNote, now, now);
        byId.put(id, cq);
        log.debug("Created custom quest id={} ownerId={} name='{}' nodes={} published={}",
                id, ownerId, name, nodes.size(), published);
    }

    /**
     * Gets a quest by id.
     *
     * @param id quest id
     * @return optional quest, empty if not found
     */
    @Override
    public Optional<CustomQuest> get(String id) {
        Optional<CustomQuest> res = Optional.ofNullable(byId.get(id));
        log.debug("Get quest id={} found={}", id, res.isPresent());
        return res;
    }

    /**
     * Lists all quests sorted by {@code updatedAt} descending.
     *
     * @return immutable list of quests
     */
    @Override
    public List<CustomQuest> listAll() {
        List<CustomQuest> list = byId.values().stream()
                .sorted(Comparator.comparing(CustomQuest::getUpdatedAt).reversed())
                .toList();
        log.debug("List all quests size={}", list.size());
        return list;
    }

    /**
     * Lists quests of a specific owner sorted by {@code updatedAt} descending.
     *
     * @param ownerId owner user id
     * @return immutable list of quests for the owner
     */
    @Override
    public List<CustomQuest> listByOwner(String ownerId) {
        List<CustomQuest> list = byId.values().stream()
                .filter(q -> q.getOwnerId().equals(ownerId))
                .sorted(Comparator.comparing(CustomQuest::getUpdatedAt).reversed())
                .toList();
        log.debug("List quests by ownerId={} size={}", ownerId, list.size());
        return list;
    }

    /**
     * Updates an existing quest with new graph/start/publish/version and bumps {@code updatedAt}.
     * No-ops (with a warning) if the quest does not exist.
     *
     * @param id          quest id
     * @param startId     new start node id
     * @param nodes       replacement nodes
     * @param published   new published flag
     * @param versionNote new version/comment label
     */
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

    /**
     * Deletes a quest by id.
     *
     * @param id quest id
     * @return {@code true} if a quest was removed, {@code false} otherwise
     */
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

    /**
     * Deletes a quest only if {@code ownerId} matches the quest owner.
     *
     * @param id      quest id
     * @param ownerId expected owner id
     * @return {@code true} if removed, {@code false} if missing or owner mismatch
     */
    @Override
    public boolean deleteIfOwner(String id, String ownerId) {
        CustomQuest q = byId.get(id);
        if (q == null) {
            log.warn("Delete-if-owner skipped: quest not found id={}", id);
            return false;
        }
        if (!ownerId.equals(q.getOwnerId())) {
            log.warn("Delete-if-owner denied: owner mismatch id={} ownerId={}", id, ownerId);
            return false;
        }
        byId.remove(id);
        log.debug("Deleted quest by owner id={} ownerId={}", id, ownerId);
        return true;
    }

    /**
     * Stages a new quest for moderation (CREATE). Generates a {@code pendingId}.
     *
     * @param ownerId     owner user id
     * @param name        quest name
     * @param startId     start node id
     * @param nodes       nodes to store
     * @param versionNote version/comment label
     */
    @Override
    public void stageCreate(String ownerId, String name, int startId, List<QuestNode> nodes, String versionNote) {
        String pendingId = "new-" + UUID.randomUUID();
        stagedNew.put(pendingId, new PendingNew(
                pendingId, ownerId, name, startId, List.copyOf(nodes), versionNote, Instant.now()
        ));
        log.debug("Staged CREATE pendingId={} ownerId={} name='{}' nodes={} startId={}",
                pendingId, ownerId, name, nodes.size(), startId);
    }

    /**
     * Lists pending CREATE items sorted by submission time DESC.
     *
     * @return immutable list of pending new items
     */
    @Override
    public List<PendingNew> listPendingNew() {
        List<PendingNew> list = stagedNew.values().stream()
                .sorted(Comparator.comparing(PendingNew::getSubmittedAt).reversed())
                .toList();
        log.debug("List pending NEW size={}", list.size());
        return list;
    }

    /**
     * Approves a pending CREATE and materializes the quest as published.
     *
     * @param pendingId moderation id
     * @return generated quest id
     * @throws NoSuchElementException if {@code pendingId} not found
     */
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
                newId, pn.getOwnerId(), pn.getName(), pn.getStartId(),
                pn.getNodes(), true, pn.getVersionNote(), now, now
        ));
        log.debug("Approved CREATE pendingId={} -> newId={} ownerId={} name='{}'",
                pendingId, newId, pn.getOwnerId(), pn.getName());
        return newId;
    }

    /**
     * Rejects a pending CREATE.
     *
     * @param pendingId moderation id
     * @throws NoSuchElementException if {@code pendingId} not found
     */
    @Override
    public void rejectCreate(String pendingId) {
        if (stagedNew.remove(pendingId) == null) {
            log.warn("Reject CREATE failed: pending not found pendingId={}", pendingId);
            throw new NoSuchElementException("Pending new quest not found: " + pendingId);
        }
        log.debug("Rejected CREATE pendingId={}", pendingId);
    }

    /**
     * Stages an edit for moderation (EDIT) for an existing quest.
     *
     * @param questId     target quest id
     * @param startId     new start node id
     * @param nodes       replacement nodes
     * @param versionNote version/comment label
     * @throws NoSuchElementException if quest not found
     */
    @Override
    public void stageEdit(String questId, int startId, List<QuestNode> nodes, String versionNote) {
        CustomQuest live = byId.get(questId);
        if (live == null) {
            log.warn("Stage EDIT failed: quest not found id={}", questId);
            throw new NoSuchElementException("Quest not found: " + questId);
        }
        stagedEdit.put(questId, new PendingEdit(
                questId, live.getOwnerId(), live.getName(),
                startId, List.copyOf(nodes), versionNote, Instant.now()
        ));
        log.debug("Staged EDIT questId={} ownerId={} name='{}' nodes={} startId={}",
                questId, live.getOwnerId(), live.getName(), nodes.size(), startId);
    }

    /**
     * Lists pending EDIT items sorted by submission time DESC.
     *
     * @return immutable list of pending edit items
     */
    @Override
    public List<PendingEdit> listPendingEdits() {
        List<PendingEdit> list = stagedEdit.values().stream()
                .sorted(Comparator.comparing(PendingEdit::getSubmittedAt).reversed())
                .toList();
        log.debug("List pending EDIT size={}", list.size());
        return list;
    }

    /**
     * Applies a pending EDIT to the live quest (preserving publish flag) and updates {@code updatedAt}.
     *
     * @param questId quest id that has a pending edit
     * @throws NoSuchElementException if no pending edit exists for the id
     */
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

    /**
     * Rejects a pending EDIT.
     *
     * @param questId quest id that has a pending edit
     * @throws NoSuchElementException if no pending edit exists for the id
     */
    @Override
    public void rejectEdit(String questId) {
        if (stagedEdit.remove(questId) == null) {
            log.warn("Reject EDIT failed: pending not found questId={}", questId);
            throw new NoSuchElementException("Pending edit not found: " + questId);
        }
        log.debug("Rejected EDIT questId={}", questId);
    }
}
