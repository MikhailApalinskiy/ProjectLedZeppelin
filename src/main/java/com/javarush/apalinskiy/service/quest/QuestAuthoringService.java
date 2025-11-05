package com.javarush.apalinskiy.service.quest;

import com.javarush.apalinskiy.utils.CurrentUserProvider;
import com.javarush.apalinskiy.domain.quest.custom.DraftRow;
import com.javarush.apalinskiy.repository.hibernate.quest.GraphJsonMapper;
import com.javarush.apalinskiy.repository.hibernate.quest.HDraftRepository;
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
 * High-level service responsible for authoring, editing, validating, and publishing custom quests.
 *
 * <p>This service acts as a bridge between:
 * <ul>
 *     <li>The in-memory quest editor ({@link QuestDraftStore})</li>
 *     <li>The live production quest repository ({@link QuestStore})</li>
 *     <li>The persistent catalog of user-created quests ({@link CustomQuestRepository})</li>
 * </ul>
 *
 * <p>Additionally, it provides optional draft management functionality
 * via {@link HDraftRepository}, allowing users to autosave and restore
 * their in-progress quest drafts.</p>
 *
 * <p>Main responsibilities include:
 * <ul>
 *     <li>Editing quest nodes and maintaining the draft state</li>
 *     <li>Validating quest structure and logic before publication</li>
 *     <li>Publishing new or updated quests (instantly or via moderation)</li>
 *     <li>Managing drafts and autosaving user progress</li>
 * </ul></p>
 */
@Getter
public class QuestAuthoringService {

    private static final Logger log = LoggerFactory.getLogger(QuestAuthoringService.class);

    private final QuestDraftStore editorRepo;
    private final QuestStore prodRepo;
    private final CustomQuestRepository catalogRepo;
    private HDraftRepository draftRepo;
    private CurrentUserProvider currentUser;

    private volatile String draftTargetQuestId = null;
    private volatile String draftName = "Untitled Draft";
    private volatile String draftVersionNote = "";

    /**
     * Creates a service without draft support (autosave/restore disabled).
     *
     * @param editorRepo editor repository (must not be {@code null})
     * @param prodRepo live production repository (must not be {@code null})
     * @param catalogRepo catalog repository (must not be {@code null})
     */
    public QuestAuthoringService(QuestDraftStore editorRepo, QuestStore prodRepo, CustomQuestRepository catalogRepo) {
        this.editorRepo = Objects.requireNonNull(editorRepo);
        this.prodRepo = Objects.requireNonNull(prodRepo);
        this.catalogRepo = Objects.requireNonNull(catalogRepo);
        log.debug("QuestAuthoringService: constructed (draftsEnabled={})", false);
    }

    /**
     * Creates a service with draft support enabled.
     *
     * @param editorRepo editor repository
     * @param prodRepo live production repository
     * @param catalogRepo catalog repository
     * @param draftRepo drafts repository to use
     * @param currentUser provider of the current user context
     */
    public QuestAuthoringService(QuestDraftStore editorRepo, QuestStore prodRepo, CustomQuestRepository catalogRepo,
                                 HDraftRepository draftRepo, CurrentUserProvider currentUser) {
        this(editorRepo, prodRepo, catalogRepo);
        this.draftRepo = draftRepo;
        this.currentUser = currentUser;
        log.info("QuestAuthoringService: constructed with drafts support (draftsEnabled={})", draftsEnabled());
    }

    /**
     * Enables drafts after construction.
     *
     * @param draftRepo drafts repository
     * @param currentUser current user provider
     */
    public void enableDrafts(HDraftRepository draftRepo, CurrentUserProvider currentUser) {
        this.draftRepo = draftRepo;
        this.currentUser = currentUser;
        log.info("Drafts enabled (draftsEnabled={})", draftsEnabled());
    }

    /**
     * Returns the quest node with the given identifier from the editor repository.
     *
     * @param id node identifier
     * @return quest node or {@code null} if not found
     */
    public QuestNode get(int id) {
        log.debug("get node id={}", id);
        return editorRepo.get(id);
    }

    /**
     * Returns all quest nodes currently loaded in the editor.
     *
     * @return list of nodes representing the current draft or quest
     */
    public List<QuestNode> nodes() {
        return editorRepo.nodes();
    }

    /**
     * Deletes the node with the specified identifier from the editor draft.
     * <p>If node {@code #1} is deleted, resets the start ID to {@code 0}.</p>
     *
     * @param id node identifier to delete
     * @return {@code true} if the node existed and was removed; otherwise {@code false}
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
        autosaveDraft();
        return true;
    }

    /**
     * Saves or updates a node in the editor draft.
     * <p>If node {@code #1} is added or modified, forces the start ID to 1.</p>
     *
     * @param node quest node to save or replace
     */
    public void saveNode(QuestNode node) {
        boolean existed = editorRepo.get(node.getId()) != null;
        editorRepo.replaceNode(node);
        if (node.getId() == 1) {
            editorRepo.setStartId(1);
            log.debug("startId forced to node #1");
        }
        log.debug("saveNode id={} existed={}", node.getId(), existed);
        autosaveDraft();
    }

    public void setStart(int startId) {
        log.debug("setStart requested startId={}", startId);
        editorRepo.setStartId(1);
        log.debug("setStart ignored requested={}, forced to 1", startId);
    }

    /**
     * Clears the current editor draft, resetting all fields and nodes.
     *
     * <p>If a previously autosaved draft exists for the current user,
     * it will be restored automatically; otherwise, an empty draft is created.</p>
     */
    public void clearEditorDraft() {
        log.info("clearEditorDraft()");
        this.draftTargetQuestId = null;
        this.draftName = "Untitled Draft";
        this.draftVersionNote = "";
        boolean restored = tryRestoreDraft(null);
        if (!restored) {
            editorRepo.clearDraft(0);
            log.debug("clearEditorDraft: no draft found, set empty");
        } else {
            log.debug("clearEditorDraft: restored DRAFT for new quest");
        }
        autosaveDraft();
    }

    /**
     * Loads an existing quest (or its draft) into the editor for editing.
     *
     * <p>If a draft version exists for the current user, restores it instead of
     * loading the published quest from the catalog.</p>
     *
     * @param questId quest identifier to load
     * @throws IllegalArgumentException if quest is not found
     */
    public void loadToEditor(String questId) {
        log.info("loadToEditor questId={}", questId);
        Objects.requireNonNull(questId, "questId");
        this.draftTargetQuestId = questId;
        this.draftName = "Draft of " + questId;
        this.draftVersionNote = "";
        if (!tryRestoreDraft(questId)) {
            CustomQuest q = catalogRepo.get(questId).orElseThrow(() -> {
                log.warn("loadToEditor failed: quest not found questId={}", questId);
                return new IllegalArgumentException("Quest not found: " + questId);
            });
            editorRepo.reload(q.getNodes(), q.getStartId(), false);
            log.info("Editor loaded LIVE questId={} name='{}' nodes={} startId={}",
                    questId, q.getName(), q.getNodes().size(), q.getStartId());
        } else {
            log.info("Editor restored DRAFT for questId={}", questId);
        }
        autosaveDraft();
    }

    /**
     * Retrieves a quest from the catalog by its identifier.
     *
     * @param id quest identifier
     * @return optional quest; empty if not found
     */
    public Optional<CustomQuest> getFromCatalog(String id) {
        log.debug("getFromCatalog id={}", id);
        return catalogRepo.get(id);
    }

    /**
     * Publishes a new quest directly to the catalog (bypassing moderation).
     *
     * <p>Validates the current editor state, computes a SHA-256 version hash,
     * and persists a new {@link CustomQuest} entity as published.</p>
     *
     * @param ownerId   identifier of the quest owner
     * @param questName name of the quest (defaults to "Untitled Quest" if blank)
     * @throws IllegalStateException if validation fails or draft is invalid
     */
    public void publishNew(String ownerId, String questName) {
        log.info("publishNew ownerId={} questName='{}'", ownerId, questName);
        Objects.requireNonNull(ownerId, "ownerId");
        String name = (questName == null || questName.isBlank()) ? "Untitled Quest" : questName.trim();
        Draft d = buildDraftOrThrow();
        catalogRepo.create(ownerId, name, d.start, d.nodes, true, d.version);
        log.info("Quest published ownerID={} name='{}' nodes={} startId={} version={}",
                ownerId, name, d.nodes.size(), d.start, d.version);
        editorRepo.reload(Collections.emptyList(), 0, false);
        if (draftsEnabled()) {
            try {
                draftRepo.findLatest(currentUser.currentUserId(), null)
                        .ifPresent(dr -> draftRepo.delete(dr.getDraftId(), currentUser.currentUserId()));
            } catch (Exception ignore) {
            }
        }
    }

    /**
     * Publishes a new quest (alias of {@link #publishNew(String, String)}).
     */
    public void publish(String ownerId, String questName) {
        log.debug("publish (alias publishNew) ownerId={} questName='{}'", ownerId, questName);
        publishNew(ownerId, questName);
    }

    /**
     * Deletes a quest from the catalog if the current user is its owner.
     *
     * @param questId quest identifier
     * @param ownerID current user identifier
     * @return {@code true} if deleted, {@code false} otherwise
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
     * Deletes a quest from the catalog with administrative privileges.
     *
     * @param questId quest identifier
     * @return {@code true} if deleted, {@code false} otherwise
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
     * Validates the current editor draft for structural and logical consistency.
     *
     * <p>Checks include:
     * <ul>
     *     <li>Presence of start node (#1)</li>
     *     <li>Presence of at least one final node</li>
     *     <li>Integrity of option links</li>
     *     <li>Non-empty option lists for non-final nodes</li>
     * </ul></p>
     *
     * @return list of validation errors (empty if valid)
     */
    public List<String> validateCurrentDraft() {
        log.debug("validateCurrentDraft()");
        List<String> errors = new ArrayList<>();
        List<QuestNode> list = editorRepo.nodes();
        int start = editorRepo.startId();
        if (list == null || list.isEmpty()) {
            errors.add("Draft is empty.");
            log.debug("validateCurrentDraft -> errors={}", errors);
            return errors;
        }
        Map<Integer, QuestNode> byId = list.stream()
                .collect(Collectors.toMap(QuestNode::getId, n -> n, (a, b) -> a, LinkedHashMap::new));
        if (!byId.containsKey(1)) {
            errors.add("Start node #1 is required.");
        }
        if (start != 1) {
            errors.add("Start node must be #1.");
        }
        boolean hasFinal = list.stream().anyMatch(QuestNode::getFin);
        if (!hasFinal) {
            errors.add("At least one final node is required.");
        }
        for (QuestNode n : list) {
            if (!n.getFin()) {
                var opts = n.getOptions();
                boolean ok = !opts.isEmpty() && opts.stream().allMatch(o -> o != null && o.getNext() != null);
                if (!ok) {
                    errors.add("Node #" + n.getId() + " must have at least one option.");
                }
            }
        }
        for (QuestNode n : list) {
            if (!n.getFin()) {
                for (Option o : n.getOptions()) {
                    if (o == null || o.getNext() == null) {
                        continue;
                    }
                    if (!byId.containsKey(o.getNext())) {
                        errors.add("Node #" + n.getId() + " has a broken link to #" + o.getNext() + ".");
                    }
                }
            }
        }
        log.debug("validateCurrentDraft -> errorsCount={}", errors.size());
        return errors;
    }

    /**
     * Submits the current draft as a new quest for moderation approval.
     *
     * <p>The quest will be stored in the staging area until reviewed
     * and approved or rejected by an administrator.</p>
     *
     * @param ownerId   quest owner identifier
     * @param questName quest name (defaults to "Untitled Quest" if blank)
     */
    public void submitNewForModeration(String ownerId, String questName) {
        log.info("submitNewForModeration ownerId={} questName='{}'", ownerId, questName);
        Objects.requireNonNull(ownerId, "ownerId");
        String name = (questName == null || questName.isBlank()) ? "Untitled Quest" : questName.trim();
        Draft d = buildDraftOrThrow();
        catalogRepo.stageCreate(ownerId, name, d.start, d.nodes, d.version);
        log.info("Quest submitted for moderation owner={} name='{}' nodes={} startId={} version={}",
                ownerId, name, d.nodes.size(), d.start, d.version);
        editorRepo.reload(Collections.emptyList(), 0, false);
        if (draftsEnabled()) {
            try {
                draftRepo.findLatest(currentUser.currentUserId(), null)
                        .ifPresent(dr -> draftRepo.delete(dr.getDraftId(), currentUser.currentUserId()));
                log.debug("submitNewForModeration: cleanup temp draft done");
            } catch (Exception ignore) {
            }
        }
    }

    /**
     * Updates an existing quest with the data from the current editor draft.
     *
     * <p>If called with {@code asAdmin = true}, the update is published immediately;
     * otherwise, the new version is staged for moderation.</p>
     *
     * @param questId quest identifier
     * @param asAdmin whether to publish immediately as an admin
     * @throws IllegalArgumentException if the quest does not exist
     */
    public void updateExisting(String questId, boolean asAdmin) {
        log.info("updateExisting questId={} asAdmin={}", questId, asAdmin);
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
        if (draftsEnabled()) {
            try {
                draftRepo.findLatest(currentUser.currentUserId(), questId)
                        .ifPresent(dr -> draftRepo.delete(dr.getDraftId(), currentUser.currentUserId()));
                log.debug("updateExisting: cleanup draft for questId={} done", questId);
            } catch (Exception ignore) {
            }
        }
    }

    /**
     * Returns the list of quests pending creation approval.
     *
     * @return list of pending new quests awaiting moderation
     */
    public List<CustomQuestRepository.PendingNew> listPendingNew() {
        List<CustomQuestRepository.PendingNew> list = catalogRepo.listPendingNew();
        log.debug("listPendingNew size={}", list.size());
        return list;
    }

    /**
     * Returns the list of quests pending edit approval.
     *
     * @return list of pending edits awaiting moderation
     */
    public List<CustomQuestRepository.PendingEdit> listPendingEdits() {
        List<CustomQuestRepository.PendingEdit> list = catalogRepo.listPendingEdits();
        log.debug("listPendingEdits size={}", list.size());
        return list;
    }

    /**
     * Approves a new quest creation request, making it publicly available.
     *
     * @param pendingId pending request identifier
     * @return new quest ID assigned upon approval
     */
    public String approveCreate(String pendingId) {
        String id = catalogRepo.approveCreate(pendingId);
        log.info("Approved NEW pendingId={} -> questId={}", pendingId, id);
        return id;
    }

    /**
     * Rejects a pending quest creation request.
     *
     * @param pendingId pending request identifier
     */
    public void rejectCreate(String pendingId) {
        catalogRepo.rejectCreate(pendingId);
        log.info("Rejected NEW pendingId={}", pendingId);
    }

    /**
     * Approves a pending quest edit and applies the changes.
     *
     * @param questId quest identifier
     */
    public void approveEdit(String questId) {
        catalogRepo.approveEdit(questId);
        log.info("Approved EDIT questId={}", questId);
    }

    /**
     * Rejects a pending quest edit request.
     *
     * @param questId quest identifier
     */
    public void rejectEdit(String questId) {
        catalogRepo.rejectEdit(questId);
        log.info("Rejected EDIT questId={}", questId);
    }

    /**
     * Computes a stable content-based version hash for a quest graph.
     *
     * <p>The hash includes the start id, ordered nodes, node fields (id, fin, image, text),
     * and each option's choice and next link. Changes in any of these elements produce
     * a different version string.</p>
     *
     * @param nodes quest nodes
     * @param start start node id
     * @return version string in the form {@code sha256:HEX}
     */
    private static String computeVersion(List<QuestNode> nodes, int start) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(("start:" + start + ";").getBytes(StandardCharsets.UTF_8));
            nodes.stream()
                    .sorted(Comparator.comparingInt(QuestNode::getId))
                    .forEach(n -> {
                        md.update(("id:" + n.getId() + ";fin:" + n.getFin() + ";").getBytes(StandardCharsets.UTF_8));
                        md.update(("img:" + (n.getImage() == null ? "" : n.getImage()) + ";").getBytes(StandardCharsets.UTF_8));
                        md.update(("text:" + n.getText() + ";").getBytes(StandardCharsets.UTF_8));
                        for (Option o : n.getOptions()) {
                            String seg = "[" + o.getChoice() + "->" + (o.getNext() == null ? "" : o.getNext()) + "]";
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
     * Immutable draft snapshot prepared for persistence/publication.
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
     * Validates the current editor state and builds a {@link Draft} or throws.
     *
     * @return prepared draft snapshot
     * @throws IllegalStateException if the editor state is invalid
     */
    private Draft buildDraftOrThrow() {
        log.debug("buildDraftOrThrow()");
        List<String> errors = validateCurrentDraft();
        if (!errors.isEmpty()) {
            log.warn("Draft validation failed errors={}", String.join(" ", errors));
            throw new IllegalStateException(String.join(" ", errors));
        }
        List<QuestNode> nodes = editorRepo.nodes();
        int start = 1;
        boolean has1 = nodes.stream().anyMatch(n -> n.getId() == 1);
        if (!has1) {
            throw new IllegalStateException("Start node #1 is required.");
        }
        QuestNavigator.from(nodes, start);
        String version = computeVersion(nodes, start);
        log.debug("Draft built nodes={} startId={} version={}", nodes.size(), start, version);
        return new Draft(nodes, start, version);
    }

    /**
     * @return {@code true} if drafts functionality is available (both {@link #draftRepo} and {@link #currentUser} set)
     */
    private boolean draftsEnabled() {
        return draftRepo != null && currentUser != null;
    }

    /**
     * Attempts to autosave the current editor state for the current user. No-op if drafts are disabled.
     */
    private void autosaveDraft() {
        if (!draftsEnabled()) {
            return;
        }
        try {
            String owner = currentUser.currentUserId();
            log.trace("autosaveDraft owner={} targetQuestId={} name='{}'", owner, draftTargetQuestId, draftName);
            draftRepo.upsertDraft(
                    owner,
                    draftTargetQuestId,
                    draftName,
                    editorRepo.startId(),
                    editorRepo.nodes(),
                    draftVersionNote
            );
            log.debug("autosaveDraft: ok");
        } catch (RuntimeException e) {
            log.warn("Draft autosave failed (non-fatal): {}", e.toString());
        }
    }

    /**
     * Tries to restore the last autosaved draft for the current user.
     *
     * @param targetQuestId target quest id to filter by; {@code null} restores a draft for a new quest
     * @return {@code true} if a draft was restored; {@code false} otherwise
     */
    private boolean tryRestoreDraft(String targetQuestId) {
        if (!draftsEnabled()) {
            log.trace("tryRestoreDraft skipped: drafts disabled");
            return false;
        }
        String owner = currentUser.currentUserId();
        log.debug("tryRestoreDraft owner={} targetQuestId={}", owner, targetQuestId);
        return draftRepo.findLatest(owner, targetQuestId).map(row -> {
            List<QuestNode> nodes = GraphJsonMapper.fromJson(row.getNodesJson());
            editorRepo.reload(nodes, row.getStartId(), true);
            this.draftName = row.getName();
            this.draftVersionNote = row.getVersionNote();
            return true;
        }).orElse(false);
    }

    /**
     * Loads a specific saved draft into the editor.
     *
     * @param draftId identifier of the draft to load
     * @throws IllegalStateException if drafts are disabled or user is not the owner
     */
    public void loadDraftIntoEditor(String draftId) {
        log.info("loadDraftIntoEditor draftId={}", draftId);
        if (!draftsEnabled()) {
            throw new IllegalStateException("Drafts not enabled");
        }
        String owner = currentUser.currentUserId();
        DraftRow row = draftRepo.findById(draftId)
                .orElseThrow(() -> new IllegalArgumentException("Draft not found: " + draftId));
        if (!owner.equals(row.getOwnerId())) {
            log.warn("loadDraftIntoEditor denied: not owner draftId={} owner={} current={}", draftId, row.getOwnerId(), owner);
            throw new IllegalStateException("Not your draft");
        }
        List<QuestNode> nodes = GraphJsonMapper.fromJson(row.getNodesJson());
        this.draftTargetQuestId = row.getTargetQuestId();
        this.draftName = row.getName();
        this.draftVersionNote = row.getVersionNote();
        editorRepo.reload(nodes, row.getStartId(), true);
    }

    /**
     * Creates a new empty draft and clears the editor.
     *
     * @param name name of the new draft (defaults to "Untitled Draft" if blank)
     * @throws IllegalStateException if drafts are disabled
     */
    public void newEmptyDraft(String name) {
        log.info("newEmptyDraft name='{}'", name);
        if (!draftsEnabled()) {
            throw new IllegalStateException("Drafts not enabled");
        }
        String owner = currentUser.currentUserId();
        draftRepo.createEmpty(owner, null, name);
        this.draftTargetQuestId = null;
        this.draftName = (name == null || name.isBlank()) ? "Untitled Draft" : name.trim();
        this.draftVersionNote = "";
        editorRepo.clearDraft(0);
        log.debug("newEmptyDraft: created and editor cleared owner={} name='{}'", owner, this.draftName);
    }

    /**
     * Lightweight immutable pagination container.
     *
     * @param <T> item type
     */
    @Getter
    public static final class Paged<T> {
        private final List<T> items;
        private final int total;
        private final int page;
        private final int pages;
        private final int size;

        public Paged(List<T> items, int total, int page, int size) {
            this.items = List.copyOf(items);
            this.total = total;
            this.size = size;
            this.page = Math.max(1, page);
            this.pages = Math.max(1, (int) Math.ceil(total / (double) size));
        }
    }

    /**
     * Returns a paginated list of all published quests in the catalog.
     *
     * @param q    optional search query (by name or description)
     * @param page current page number (1-based)
     * @param size page size
     * @return {@link Paged} result containing the quests and pagination info
     */
    public Paged<CustomQuest> listAllFromCatalogPaged(String q, int page, int size) {
        log.debug("listAllFromCatalogPaged q='{}' page={} size={}", q, page, size);
        final int safeSize = Math.max(1, size);
        final int total = catalogRepo.countAllLive(q);
        final int pages = Math.max(1, (int) Math.ceil(total / (double) safeSize));
        final int safePage = Math.min(Math.max(1, page), pages);
        final List<CustomQuest> items = (total == 0)
                ? List.of()
                : catalogRepo.findAllLivePaged(safePage, safeSize, q);
        log.debug("listAllFromCatalogPaged -> total={} pages={} items={}", total, pages, items.size());
        return new Paged<>(items, total, safePage, safeSize);
    }

    /**
     * Returns a paginated list of quests created by a specific owner.
     *
     * <p>Non-owners or non-admins will see only published quests.</p>
     *
     * @param ownerId              quest owner identifier
     * @param q                    optional search query
     * @param page                 current page number
     * @param size                 page size
     * @param viewerIsOwnerOrAdmin whether the viewer has full access
     * @return {@link Paged} result containing the owner’s quests
     */
    public Paged<CustomQuest> listOwnerFromCatalogPaged(
            String ownerId, String q, int page, int size, boolean viewerIsOwnerOrAdmin) {
        log.debug("listOwnerFromCatalogPaged ownerId={} q='{}' page={} size={} viewerIsOwnerOrAdmin={}",
                ownerId, q, page, size, viewerIsOwnerOrAdmin);
        final boolean onlyLive = !viewerIsOwnerOrAdmin;
        final int safeSize = Math.max(1, size);
        final int total = catalogRepo.countByOwner(ownerId, q, onlyLive);
        final int pages = Math.max(1, (int) Math.ceil(total / (double) safeSize));
        final int safePage = Math.min(Math.max(1, page), pages);
        final List<CustomQuest> items = (total == 0)
                ? List.of()
                : catalogRepo.findByOwnerPaged(ownerId, safePage, safeSize, q, onlyLive);
        log.debug("listOwnerFromCatalogPaged -> total={} pages={} items={}", total, pages, items.size());
        return new Paged<>(items, total, safePage, safeSize);
    }
}
