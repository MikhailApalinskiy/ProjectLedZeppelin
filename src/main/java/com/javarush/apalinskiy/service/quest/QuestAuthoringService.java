package com.javarush.apalinskiy.service.quest;

import com.javarush.apalinskiy.repository.quest.QuestDraftStore;
import com.javarush.apalinskiy.repository.quest.QuestStore;
import com.javarush.apalinskiy.domain.quest.Option;
import com.javarush.apalinskiy.domain.quest.index.QuestNavigator;
import com.javarush.apalinskiy.domain.quest.custom.CustomQuest;
import com.javarush.apalinskiy.domain.quest.QuestNode;
import com.javarush.apalinskiy.repository.quest.CustomQuestRepository;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Application service that coordinates editor draft operations, catalog (publication)
 * operations and moderation workflow for custom quests.
 * <p>
 * The service delegates persistence to three repositories:
 * editor draft store, production store and the public catalog repository.
 * </p>
 */
@Getter
public class QuestAuthoringService {

    private static final Logger log = LoggerFactory.getLogger(QuestAuthoringService.class);

    /**
     * In-memory/temporary editor draft storage used while authoring.
     */
    private final QuestDraftStore editorRepo;
    /**
     * Read-only/engine-facing production store (not used directly in publication flow here).
     */
    private final QuestStore prodRepo;
    /**
     * Public catalog with live quests and moderation queues.
     */
    private final CustomQuestRepository catalogRepo;

    /**
     * Creates the service with required repositories.
     *
     * @param editorRepo  draft repository used by the editor
     * @param prodRepo    production repository (engine/runtime)
     * @param catalogRepo public catalog / moderation repository
     */
    public QuestAuthoringService(QuestDraftStore editorRepo, QuestStore prodRepo, CustomQuestRepository catalogRepo) {
        this.editorRepo = Objects.requireNonNull(editorRepo);
        this.prodRepo = Objects.requireNonNull(prodRepo);
        this.catalogRepo = Objects.requireNonNull(catalogRepo);
    }

    /**
     * @return current draft node by id or {@code null} if missing
     */
    public QuestNode get(int id) {
        return editorRepo.get(id);
    }

    /**
     * @return snapshot of current draft nodes
     */
    public List<QuestNode> nodes() {
        return editorRepo.nodes();
    }

    /**
     * Deletes a node from the draft. If node #1 is removed, start id is reset to 0.
     *
     * @param id node id
     * @return {@code true} if the node existed and was removed
     */
    public boolean deleteNode(int id) {
        boolean ok = editorRepo.deleteNode(id);
        log.debug("deleteNode id={} ok={}", id, ok);
        if (!ok) {
            return false;
        }
        if (id == 1) {
            editorRepo.setStartId(0);
            log.debug("startId reset, node #1 deleted");
        }
        return true;
    }

    /**
     * Inserts or replaces a node in the draft. If node id is 1, forces start id to 1.
     *
     * @param node node to persist into the draft
     */
    public void saveNode(QuestNode node) {
        boolean existed = editorRepo.get(node.getId()) != null;
        editorRepo.replaceNode(node);
        if (node.getId() == 1) {
            editorRepo.setStartId(1);
            log.debug("startId forced to node #1");
        }
        log.debug("saveNode id={} existed={}", node.getId(), existed);
    }

    /**
     * Sets the start node id in the draft.
     *
     * @param startId start node id
     */
    public void setStart(int startId) {
        editorRepo.setStartId(startId);
        log.debug("setStart startId={}", startId);
    }

    /**
     * Clears the current draft.
     */
    public void clearEditorDraft() {
        editorRepo.clearDraft(0);
        log.debug("clearEditorDraft done");
    }

    /**
     * Loads a catalog quest into the editor draft (non-published state).
     *
     * @param questId id of the catalog quest to load
     * @throws IllegalArgumentException if the quest is not found
     */
    public void loadToEditor(String questId) {
        CustomQuest q = catalogRepo.get(Objects.requireNonNull(questId, "questId"))
                .orElseThrow(() -> {
                    log.warn("loadToEditor failed: quest not found questId={}", questId);
                    return new IllegalArgumentException("Quest not found: " + questId);
                });
        editorRepo.reload(q.getNodes(), q.getStartId(), false);
        log.info("Editor loaded questId={} name='{}' nodes={} startId={}", questId, q.getName(), q.getNodes().size(), q.getStartId());
    }

    /**
     * Fetches a quest from the catalog.
     *
     * @param id quest id
     * @return optional quest
     */
    public Optional<CustomQuest> getFromCatalog(String id) {
        return catalogRepo.get(id);
    }

    /**
     * Lists all quests from the catalog (typically ordered by {@code updatedAt DESC}).
     *
     * @return list of quests
     */
    public List<CustomQuest> listAllFromCatalog() {
        List<CustomQuest> list = catalogRepo.listAll();
        log.debug("listAllFromCatalog size={}", list.size());
        return list;
    }

    /**
     * Lists catalog quests owned by the given user.
     *
     * @param ownerId owner user id
     * @return list of quests
     */
    public List<CustomQuest> listOwnerFromCatalog(String ownerId) {
        List<CustomQuest> list = catalogRepo.listByOwner(ownerId);
        log.debug("listOwnerFromCatalog ownerId={} size={}", ownerId, list.size());
        return list;
    }

    /**
     * Publishes a new quest from the current draft immediately (admin path).
     * Resets the editor draft on success.
     *
     * @param ownerId   owner user id (non-null)
     * @param questName desired quest name; defaults to "Untitled Quest" if blank
     * @throws IllegalStateException if the draft is invalid
     */
    public void publishNew(String ownerId, String questName) {
        Objects.requireNonNull(ownerId, "ownerId");
        String name = (questName == null || questName.isBlank()) ? "Untitled Quest" : questName.trim();
        Draft d = buildDraftOrThrow();
        catalogRepo.create(ownerId, name, d.start, d.nodes, true, d.version);
        log.info("Quest published ownerID={} name='{}' nodes={} startId={} version={}",
                ownerId, name, d.nodes.size(), d.start, d.version);
        editorRepo.reload(Collections.emptyList(), 0, false);
    }

    /**
     * Alias for {@link #publishNew(String, String)}.
     */
    public void publish(String ownerId, String questName) {
        publishNew(ownerId, questName);
    }

    /**
     * Deletes a quest from the catalog if the caller is the owner.
     *
     * @param questId quest id
     * @param ownerID caller user id (must match quest owner)
     * @return {@code true} if removed; {@code false} otherwise
     */
    public boolean deleteFromCatalogIfOwner(String questId, String ownerID) {
        boolean ok = catalogRepo.deleteIfOwner(questId, ownerID);
        if (ok) {
            log.info("Quest deleted by owner questId={} ownerId={}", questId, ownerID);
        } else {
            log.warn("Delete by owner skipped questId={} ownerId={} (not found / not owner)", questId, ownerID);
        }
        return ok;
    }

    /**
     * Deletes a quest from the catalog as an admin.
     *
     * @param questId quest id
     * @return {@code true} if removed; {@code false} otherwise
     */
    public boolean deleteFromCatalogAsAdmin(String questId) {
        boolean ok = catalogRepo.delete(questId);
        if (ok) {
            log.info("Quest deleted by admin questId={}", questId);
        } else {
            log.warn("Delete by admin skipped questId={} (not found)", questId);
        }
        return ok;
    }

    /**
     * Validates the current draft for structural consistency.
     *
     * @return list of human-readable validation errors (empty if valid)
     */
    public List<String> validateCurrentDraft() {
        List<String> errors = new ArrayList<>();
        List<QuestNode> list = editorRepo.nodes();
        int start = editorRepo.startId();
        if (list == null || list.isEmpty()) {
            errors.add("Draft is empty.");
            return errors;
        }
        Map<Integer, QuestNode> byId = list.stream().collect(Collectors.toMap(QuestNode::getId, n -> n, (a, b) -> a, LinkedHashMap::new));
        if (start <= 0 || !byId.containsKey(start)) {
            errors.add("Start node is not set.");
        }
        boolean hasFinal = list.stream().anyMatch(QuestNode::isFin);
        if (!hasFinal) {
            errors.add("At least one final node is required.");
        }
        for (QuestNode n : list) {
            if (!n.isFin()) {
                var opts = n.getOptions();
                boolean ok = !opts.isEmpty() && opts.stream().allMatch(o -> o != null && o.next() != null);
                if (!ok) {
                    errors.add("Node #" + n.getId() + " must have at least one option.");
                }
            }
        }
        for (QuestNode n : list) {
            if (!n.isFin()) {
                for (Option o : n.getOptions()) {
                    if (o == null || o.next() == null) continue;
                    if (!byId.containsKey(o.next())) {
                        errors.add("Node #" + n.getId() + " has a broken link to #" + o.next() + ".");
                    }
                }
            }
        }
        return errors;
    }

    /**
     * Submits a new quest for moderation based on the current draft.
     * Resets the editor draft on success.
     *
     * @param ownerId   owner user id (non-null)
     * @param questName desired quest name; defaults to "Untitled Quest" if blank
     * @throws IllegalStateException if the draft is invalid
     */
    public void submitNewForModeration(String ownerId, String questName) {
        Objects.requireNonNull(ownerId, "ownerId");
        String name = (questName == null || questName.isBlank()) ? "Untitled Quest" : questName.trim();
        Draft d = buildDraftOrThrow();
        catalogRepo.stageCreate(ownerId, name, d.start, d.nodes, d.version);
        log.info("Quest submitted for moderation owner={} name='{}' nodes={} startId={} version={}",
                ownerId, name, d.nodes.size(), d.start, d.version);
        editorRepo.reload(Collections.emptyList(), 0, false);
    }

    /**
     * Updates an existing quest from the current draft.
     * <ul>
     *   <li>If {@code asAdmin} is true, changes are applied immediately and the quest stays published.</li>
     *   <li>Otherwise an edit is staged for moderation.</li>
     * </ul>
     *
     * @param questId target quest id (non-null; must exist)
     * @param asAdmin whether to apply immediately
     * @throws IllegalArgumentException if the quest does not exist
     * @throws IllegalStateException    if the draft is invalid
     */
    public void updateExisting(String questId, boolean asAdmin) {
        Objects.requireNonNull(questId, "questId");
        catalogRepo.get(questId).orElseThrow(() -> {
            log.warn("updateExisting failed: quest not found questId={}", questId);
            return new IllegalArgumentException("Quest not found: " + questId);
        });
        Draft d = buildDraftOrThrow();
        if (asAdmin) {
            catalogRepo.update(questId, d.start, d.nodes, true, d.version);
            log.info("Quest updated by admin questId={} nodes={} startId={} version={}",
                    questId, d.nodes.size(), d.start, d.version);
        } else {
            catalogRepo.stageEdit(questId, d.start, d.nodes, d.version);
            log.info("Quest edit staged questId={} nodes={} startId={} version={}",
                    questId, d.nodes.size(), d.start, d.version);
        }
    }

    /**
     * @return pending NEW submissions (most recent first)
     */
    public List<CustomQuestRepository.PendingNew> listPendingNew() {
        List<CustomQuestRepository.PendingNew> list = catalogRepo.listPendingNew();
        log.debug("listPendingNew size={}", list.size());
        return list;
    }

    /**
     * @return pending EDIT submissions (most recent first)
     */
    public List<CustomQuestRepository.PendingEdit> listPendingEdits() {
        List<CustomQuestRepository.PendingEdit> list = catalogRepo.listPendingEdits();
        log.debug("listPendingEdits size={}", list.size());
        return list;
    }

    /**
     * Approves a pending NEW submission and returns the created quest id.
     *
     * @param pendingId moderation id
     * @return new quest id
     */
    public String approveCreate(String pendingId) {
        String id = catalogRepo.approveCreate(pendingId);
        log.info("Approved NEW pendingId={} -> questId={}", pendingId, id);
        return id;
    }

    /**
     * Rejects a pending NEW submission.
     *
     * @param pendingId moderation id
     */
    public void rejectCreate(String pendingId) {
        catalogRepo.rejectCreate(pendingId);
        log.info("Rejected NEW pendingId={}", pendingId);
    }

    /**
     * Approves a pending EDIT for the given quest id.
     *
     * @param questId quest id
     */
    public void approveEdit(String questId) {
        catalogRepo.approveEdit(questId);
        log.info("Approved EDIT questId={}", questId);
    }

    /**
     * Rejects a pending EDIT for the given quest id.
     *
     * @param questId quest id
     */
    public void rejectEdit(String questId) {
        catalogRepo.rejectEdit(questId);
        log.info("Rejected EDIT questId={}", questId);
    }

    /**
     * Computes a stable content hash for the draft graph and start node.
     *
     * @param nodes nodes to hash
     * @param start start node id
     * @return version string in the form {@code sha256:<hex>}
     */
    private static String computeVersion(List<QuestNode> nodes, int start) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(("start:" + start + ";").getBytes(StandardCharsets.UTF_8));
            nodes.stream()
                    .sorted(Comparator.comparingInt(QuestNode::getId))
                    .forEach(n -> {
                        md.update(("id:" + n.getId() + ";fin:" + n.isFin() + ";").getBytes(StandardCharsets.UTF_8));
                        md.update(("img:" + (n.getImage() == null ? "" : n.getImage()) + ";").getBytes(StandardCharsets.UTF_8));
                        md.update(("text:" + n.getText() + ";").getBytes(StandardCharsets.UTF_8));
                        for (Option o : n.getOptions()) {
                            String seg = "[" + o.choice() + "->" + (o.next() == null ? "" : o.next()) + "]";
                            md.update(seg.getBytes(StandardCharsets.UTF_8));
                        }
                    });
            return "sha256:" + HexFormat.of().formatHex(md.digest());
        } catch (Exception e) {
            log.warn("computeVersion failed, returning 'sha256:unknown'", e);
            return "sha256:unknown";
        }
    }

    /**
     * Immutable snapshot of a validated draft.
     */
    @Getter
    private static final class Draft {
        final List<QuestNode> nodes;
        final int start;
        final String version;

        Draft(List<QuestNode> nodes, int start, String version) {
            this.nodes = nodes;
            this.start = start;
            this.version = version;
        }
    }

    /**
     * Builds a validated draft snapshot and computes its version.
     *
     * @return draft descriptor
     * @throws IllegalStateException if validation fails
     */
    private Draft buildDraftOrThrow() {
        List<String> errors = validateCurrentDraft();
        if (!errors.isEmpty()) {
            log.warn("Draft validation failed errors={}", String.join(" ", errors));
            throw new IllegalStateException(String.join(" ", errors));
        }
        List<QuestNode> nodes = editorRepo.nodes();
        int start = editorRepo.startId();
        QuestNavigator.from(nodes, start);
        String version = computeVersion(nodes, start);
        log.debug("Draft built nodes={} startId={} version={}", nodes.size(), start, version);
        return new Draft(nodes, start, version);
    }
}
