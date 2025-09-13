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
 * Service for authoring and managing quests in the editor.
 * <p>
 * Combines {@link QuestDraftStore} for in-progress editing,
 * {@link QuestStore} for published content, and {@link CustomQuestRepository}
 * for catalog persistence and moderation workflow.
 * </p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Manipulate the editor draft (add, delete, replace, clear nodes, set start).</li>
 *   <li>Load quests from the catalog into the editor.</li>
 *   <li>Publish drafts as new quests or update existing ones.</li>
 *   <li>Stage quests for moderation (pending new/edit) and approve or reject them.</li>
 *   <li>Validate current drafts for consistency and integrity.</li>
 *   <li>List catalog entries (all, by owner, pending new/edit).</li>
 *   <li>Provide utilities for computing quest version hashes.</li>
 * </ul>
 *
 * <h3>Draft lifecycle</h3>
 * <ul>
 *   <li>Draft is created and edited in {@link QuestDraftStore}.</li>
 *   <li>Validated with {@link #validateCurrentDraft()} before publishing.</li>
 *   <li>Published directly ({@link #publishNew(String, String)}) or staged for moderation
 *       ({@link #submitNewForModeration(String, String)}).</li>
 *   <li>Updated via {@link #updateExisting(String, boolean)} (admin or staged edit).</li>
 * </ul>
 *
 * <h3>Limitations</h3>
 * <ul>
 *   <li>Persistence depends on the {@link CustomQuestRepository} implementation (in-memory by default).</li>
 *   <li>Editor state is reset after publish or submit.</li>
 *   <li>Version is computed deterministically from node content, start ID, and options.</li>
 * </ul>
 */
@Getter
public class QuestAuthoringService {

    private static final Logger log = LoggerFactory.getLogger(QuestAuthoringService.class);

    /**
     * Draft (editor) store used for interactive editing.
     */
    private final QuestDraftStore editorRepo;
    /**
     * Live/published store (read-only navigation snapshot).
     */
    private final QuestStore prodRepo;
    /**
     * Catalog repository storing published quests and moderation queues.
     */
    private final CustomQuestRepository catalogRepo;

    /**
     * Creates a new authoring service.
     *
     * @param editorRepo  draft store used for editing sessions (must not be {@code null})
     * @param prodRepo    live store for published content (must not be {@code null})
     * @param catalogRepo catalog repository (must not be {@code null})
     * @throws NullPointerException if any argument is {@code null}
     */
    public QuestAuthoringService(QuestDraftStore editorRepo, QuestStore prodRepo, CustomQuestRepository catalogRepo) {
        this.editorRepo = Objects.requireNonNull(editorRepo);
        this.prodRepo = Objects.requireNonNull(prodRepo);
        this.catalogRepo = Objects.requireNonNull(catalogRepo);
    }

    /**
     * Returns a node by ID from the current editor draft.
     *
     * @param id node ID
     * @return node or {@code null} if not found
     */
    public QuestNode get(int id) {
        return editorRepo.get(id);
    }

    /**
     * Returns all nodes of the current editor draft (immutable snapshot).
     *
     * @return list of draft nodes (never {@code null}, may be empty)
     */
    public List<QuestNode> nodes() {
        return editorRepo.nodes();
    }

    /**
     * Deletes a node from the editor draft.
     * <ul>
     *   <li>Returns {@code false} if the node does not exist.</li>
     *   <li>If node #1 is deleted, resets {@code startId} to 0.</li>
     * </ul>
     *
     * @param id node ID to delete
     * @return {@code true} if deleted, {@code false} otherwise
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
     * Saves (inserts or replaces) a node in the editor draft.
     * <ul>
     *   <li>If the node existed — it is replaced; otherwise inserted.</li>
     *   <li>If the node ID is 1, {@code startId} is forced to 1.</li>
     * </ul>
     *
     * @param node node to persist in the draft (must not be {@code null})
     * @throws NullPointerException if {@code node} is {@code null}
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
     * Sets the start node ID for the current editor draft.
     *
     * @param startId new start node ID
     */
    public void setStart(int startId) {
        editorRepo.setStartId(startId);
        log.debug("setStart startId={}", startId);
    }

    /**
     * Clears the editor draft and resets {@code startId} to 0.
     */
    public void clearEditorDraft() {
        editorRepo.clearDraft(0);
        log.debug("clearEditorDraft done");
    }

    /**
     * Loads a quest from the catalog into the editor draft.
     * <p>Replaces current draft contents.</p>
     *
     * @param questId catalog quest ID to load (must not be {@code null})
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
     * Retrieves a quest from the catalog by ID.
     *
     * @param id catalog quest ID
     * @return optional with the quest if found
     */
    public Optional<CustomQuest> getFromCatalog(String id) {
        return catalogRepo.get(id);
    }

    /**
     * Lists all quests in the catalog.
     *
     * @return quests sorted by update time (implementation-dependent)
     */
    public List<CustomQuest> listAllFromCatalog() {
        List<CustomQuest> list = catalogRepo.listAll();
        log.debug("listAllFromCatalog size={}", list.size());
        return list;
    }

    /**
     * Lists all quests in the catalog belonging to the specified owner.
     *
     * @param ownerLogin owner login
     * @return list of quests owned by the user
     */
    public List<CustomQuest> listOwnerFromCatalog(String ownerLogin) {
        List<CustomQuest> list = catalogRepo.listByOwner(ownerLogin);
        log.debug("listOwnerFromCatalog owner={} size={}", ownerLogin, list.size());
        return list;
    }

    /**
     * Publishes the current draft as a new quest (immediately visible).
     * <ul>
     *   <li>Validates draft via {@link #validateCurrentDraft()}.</li>
     *   <li>Uses {@code questName} or falls back to "Untitled Quest".</li>
     *   <li>Clears the editor draft after publishing.</li>
     * </ul>
     *
     * @param ownerLogin owner login (must not be {@code null})
     * @param questName  optional quest name (blank → "Untitled Quest")
     * @throws IllegalStateException if validation fails
     * @throws NullPointerException  if {@code ownerLogin} is {@code null}
     */
    public void publishNew(String ownerLogin, String questName) {
        Objects.requireNonNull(ownerLogin, "ownerLogin");
        String name = (questName == null || questName.isBlank()) ? "Untitled Quest" : questName.trim();
        Draft d = buildDraftOrThrow();
        catalogRepo.create(ownerLogin, name, d.start, d.nodes, true, d.version);
        log.info("Quest published owner={} name='{}' nodes={} startId={} version={}",
                ownerLogin, name, d.nodes.size(), d.start, d.version);
        editorRepo.reload(Collections.emptyList(), 0, false);
    }

    /**
     * Alias for {@link #publishNew(String, String)}.
     *
     * @param ownerLogin owner login
     * @param questName  quest name
     * @throws IllegalStateException if validation fails
     */
    public void publish(String ownerLogin, String questName) {
        publishNew(ownerLogin, questName);
    }

    /**
     * Deletes a quest from the catalog if the caller is the owner.
     *
     * @param questId    quest ID
     * @param ownerLogin expected owner login
     * @return {@code true} if deleted; {@code false} if not found or not the owner
     */
    public boolean deleteFromCatalogIfOwner(String questId, String ownerLogin) {
        boolean ok = catalogRepo.deleteIfOwner(questId, ownerLogin);
        if (ok) {
            log.info("Quest deleted by owner questId={} owner={}", questId, ownerLogin);
        } else {
            log.warn("Delete by owner skipped questId={} owner={} (not found / not owner)", questId, ownerLogin);
        }
        return ok;
    }

    /**
     * Deletes a quest from the catalog as an administrator.
     *
     * @param questId quest ID
     * @return {@code true} if deleted; {@code false} if not found
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
     * Validates the current editor draft and returns a list of error messages.
     * <ul>
     *   <li>Checks that draft is not empty and start node is set.</li>
     *   <li>Requires at least one final node.</li>
     *   <li>Ensures every non-final node has at least one option with a valid {@code next}.</li>
     *   <li>Ensures all links point to existing nodes.</li>
     * </ul>
     *
     * @return list of validation errors; empty list if draft is valid
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
     * Submits the current draft as a <em>new</em> quest for moderation (pending NEW).
     * <ul>
     *   <li>Validates the draft and stages it in the catalog.</li>
     *   <li>Clears the editor draft after staging.</li>
     * </ul>
     *
     * @param ownerLogin owner login (must not be {@code null})
     * @param questName  optional quest name (blank → "Untitled Quest")
     * @throws IllegalStateException if validation fails
     * @throws NullPointerException  if {@code ownerLogin} is {@code null}
     */
    public void submitNewForModeration(String ownerLogin, String questName) {
        Objects.requireNonNull(ownerLogin, "ownerLogin");
        String name = (questName == null || questName.isBlank()) ? "Untitled Quest" : questName.trim();
        Draft d = buildDraftOrThrow();
        catalogRepo.stageCreate(ownerLogin, name, d.start, d.nodes, d.version);
        log.info("Quest submitted for moderation owner={} name='{}' nodes={} startId={} version={}",
                ownerLogin, name, d.nodes.size(), d.start, d.version);
        editorRepo.reload(Collections.emptyList(), 0, false);
    }

    /**
     * Updates an existing quest using the current draft.
     * <ul>
     *   <li>If {@code asAdmin} is {@code true} — updates immediately (published).</li>
     *   <li>Otherwise — stages an EDIT for moderation.</li>
     * </ul>
     *
     * @param questId quest ID to update (must exist)
     * @param asAdmin whether to publish immediately (admin path)
     * @throws IllegalArgumentException if the quest does not exist
     * @throws IllegalStateException    if draft validation fails
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
     * Lists pending NEW submissions awaiting moderation.
     *
     * @return list of pending NEW entries
     */
    public List<CustomQuestRepository.PendingNew> listPendingNew() {
        List<CustomQuestRepository.PendingNew> list = catalogRepo.listPendingNew();
        log.debug("listPendingNew size={}", list.size());
        return list;
    }

    /**
     * Lists pending EDIT submissions awaiting moderation.
     *
     * @return list of pending EDIT entries
     */
    public List<CustomQuestRepository.PendingEdit> listPendingEdits() {
        List<CustomQuestRepository.PendingEdit> list = catalogRepo.listPendingEdits();
        log.debug("listPendingEdits size={}", list.size());
        return list;
    }

    /**
     * Approves a pending NEW submission and creates a new quest.
     *
     * @param pendingId ID of the pending NEW submission
     * @return ID of the newly created quest
     * @throws NoSuchElementException if the pending item is not found
     */
    public String approveCreate(String pendingId) {
        String id = catalogRepo.approveCreate(pendingId);
        log.info("Approved NEW pendingId={} -> questId={}", pendingId, id);
        return id;
    }

    /**
     * Rejects a pending NEW submission.
     *
     * @param pendingId ID of the pending NEW submission
     * @throws NoSuchElementException if the pending item is not found
     */
    public void rejectCreate(String pendingId) {
        catalogRepo.rejectCreate(pendingId);
        log.info("Rejected NEW pendingId={}", pendingId);
    }

    /**
     * Approves a pending EDIT and applies it to the quest.
     *
     * @param questId quest ID
     * @throws NoSuchElementException if the pending edit is not found
     */
    public void approveEdit(String questId) {
        catalogRepo.approveEdit(questId);
        log.info("Approved EDIT questId={}", questId);
    }

    /**
     * Rejects a pending EDIT submission.
     *
     * @param questId quest ID
     * @throws NoSuchElementException if the pending edit is not found
     */
    public void rejectEdit(String questId) {
        catalogRepo.rejectEdit(questId);
        log.info("Rejected EDIT questId={}", questId);
    }

    /**
     * Computes a deterministic content-based version for a quest draft
     * (hash over start ID, nodes, their texts, images, and options).
     *
     * @param nodes nodes to include in the digest
     * @param start start node ID
     * @return version string in the form {@code sha256:<hex>} or {@code sha256:unknown} on failure
     */
    private static String computeVersion(List<QuestNode> nodes, int start) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(("start:" + start + ";").getBytes(StandardCharsets.UTF_8));
            nodes.stream()
                    .sorted(java.util.Comparator.comparingInt(QuestNode::getId))
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
     * Immutable holder for a validated draft snapshot.
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
     * Validates the current draft and builds an immutable {@link Draft} snapshot.
     * <p>
     * Performs strict validation and additionally checks graph consistency
     * by constructing a {@link QuestNavigator}.
     * </p>
     *
     * @return validated draft snapshot
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
