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
 * In-memory implementation of {@link CustomQuestRepository}.
 * <p>
 * Stores authored quests and moderation state (pending new and pending edits)
 * in thread-safe maps. Data is lost when the JVM stops and is intended
 * for testing, prototyping, or non-persistent environments.
 * </p>
 *
 * <h3>Storage</h3>
 * <ul>
 *   <li>{@link #byId} – live approved {@link CustomQuest} instances.</li>
 *   <li>{@link #stagedNew} – quests submitted by users for creation, awaiting moderation.</li>
 *   <li>{@link #stagedEdit} – edits submitted for existing quests, awaiting moderation.</li>
 * </ul>
 *
 * <h3>Lifecycle</h3>
 * <ul>
 *   <li>Authors can directly {@link #create(String, String, int, List, boolean, String)} a quest
 *       (e.g. self-published) or submit it via {@link #stageCreate(String, String, int, List, String)}
 *       for moderation.</li>
 *   <li>Moderators can {@link #approveCreate(String)} or {@link #rejectCreate(String)}.</li>
 *   <li>Edits are staged with {@link #stageEdit(String, int, List, String)} and moderated with
 *       {@link #approveEdit(String)} or {@link #rejectEdit(String)}.</li>
 *   <li>Approved quests are stored in {@link #byId} and can be updated or deleted.</li>
 * </ul>
 *
 * <h3>Logging</h3>
 * <ul>
 *   <li>DEBUG: normal lifecycle operations (create, update, approve, reject).</li>
 *   <li>WARN: attempted operations on missing or invalid quests.</li>
 * </ul>
 */
public class InMemoryCustomQuestRepository implements CustomQuestRepository {

    private static final Logger log = LoggerFactory.getLogger(InMemoryCustomQuestRepository.class);

    /**
     * Active quests stored by ID.
     */
    private final Map<String, CustomQuest> byId = new ConcurrentHashMap<>();
    /**
     * Pending new quests awaiting moderation.
     */
    private final Map<String, PendingNew> stagedNew = new ConcurrentHashMap<>();
    /**
     * Pending edits awaiting moderation.
     */
    private final Map<String, PendingEdit> stagedEdit = new ConcurrentHashMap<>();

    /**
     * Immediately creates and stores a new custom quest (no moderation).
     *
     * @param ownerLogin  quest owner login
     * @param name        quest name
     * @param startId     start node ID
     * @param nodes       quest nodes
     * @param published   whether quest should be published
     * @param versionNote version label/note
     */
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

    /**
     * Retrieves a quest by ID.
     *
     * @param id quest ID
     * @return optional containing quest if found
     */
    @Override
    public Optional<CustomQuest> get(String id) {
        Optional<CustomQuest> res = Optional.ofNullable(byId.get(id));
        log.debug("Get quest id={} found={}", id, res.isPresent());
        return res;
    }

    /**
     * Lists all quests sorted by {@link CustomQuest#getUpdatedAt} (descending).
     *
     * @return list of all quests
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
     * Lists all quests by a given owner, sorted by {@link CustomQuest#getUpdatedAt} (descending).
     *
     * @param ownerLogin quest owner login
     * @return list of quests belonging to the owner
     */
    @Override
    public List<CustomQuest> listByOwner(String ownerLogin) {
        List<CustomQuest> list = byId.values().stream()
                .filter(q -> q.getOwnerLogin().equals(ownerLogin))
                .sorted(Comparator.comparing(CustomQuest::getUpdatedAt).reversed())
                .toList();
        log.debug("List quests by owner={} size={}", ownerLogin, list.size());
        return list;
    }

    /**
     * Updates an existing quest if it exists.
     *
     * @param id          quest ID
     * @param startId     new start node ID
     * @param nodes       new nodes
     * @param published   publication flag
     * @param versionNote version note
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
     * Deletes a quest by ID.
     *
     * @param id quest ID
     * @return true if deleted, false if not found
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
     * Deletes a quest only if it belongs to the given owner.
     *
     * @param id         quest ID
     * @param ownerLogin expected owner login
     * @return true if deleted, false otherwise
     */
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

    /**
     * Stages a new quest for moderation.
     *
     * @param ownerLogin  quest owner login
     * @param name        quest name
     * @param startId     start node ID
     * @param nodes       quest nodes
     * @param versionNote version note
     */
    @Override
    public void stageCreate(String ownerLogin, String name, int startId, List<QuestNode> nodes, String versionNote) {
        String pendingId = "new-" + UUID.randomUUID();
        stagedNew.put(pendingId, new PendingNew(
                pendingId, ownerLogin, name, startId, List.copyOf(nodes), versionNote, Instant.now()
        ));
        log.debug("Staged CREATE pendingId={} owner={} name='{}' nodes={} startId={}",
                pendingId, ownerLogin, name, nodes.size(), startId);
    }

    /**
     * Lists all staged new quests awaiting moderation, sorted by submission time (descending).
     *
     * @return list of pending new quests
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
     * Approves a staged new quest, moves it into live storage,
     * and returns the new quest ID.
     *
     * @param pendingId pending ID
     * @return new quest ID
     * @throws NoSuchElementException if pending ID not found
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
                newId, pn.getOwnerLogin(), pn.getName(), pn.getStartId(),
                pn.getNodes(), true, pn.getVersionNote(), now, now
        ));
        log.debug("Approved CREATE pendingId={} -> newId={} owner={} name='{}'",
                pendingId, newId, pn.getOwnerLogin(), pn.getName());
        return newId;
    }

    /**
     * Rejects a staged new quest.
     *
     * @param pendingId pending ID
     * @throws NoSuchElementException if pending ID not found
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
     * Stages an edit for an existing quest.
     *
     * @param questId     quest ID
     * @param startId     new start node ID
     * @param nodes       new nodes
     * @param versionNote version note
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
                questId, live.getOwnerLogin(), live.getName(),
                startId, List.copyOf(nodes), versionNote, Instant.now()
        ));
        log.debug("Staged EDIT questId={} owner={} name='{}' nodes={} startId={}",
                questId, live.getOwnerLogin(), live.getName(), nodes.size(), startId);
    }

    /**
     * Lists all staged edits awaiting moderation, sorted by submission time (descending).
     *
     * @return list of pending edits
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
     * Approves a staged edit, updating the live quest.
     *
     * @param questId quest ID
     * @throws NoSuchElementException if pending edit not found
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
     * Rejects a staged edit.
     *
     * @param questId quest ID
     * @throws NoSuchElementException if pending edit not found
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
