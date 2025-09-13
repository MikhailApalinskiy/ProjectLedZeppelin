package com.javarush.apalinskiy.repository.inmemory.quest;

import com.javarush.apalinskiy.repository.quest.QuestDraftStore;
import com.javarush.apalinskiy.domain.quest.index.QuestNavigator;
import com.javarush.apalinskiy.repository.json.QuestJsonReader;
import com.javarush.apalinskiy.domain.quest.QuestNode;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * In-memory implementation of {@link QuestDraftStore}.
 * <p>
 * Maintains a quest graph in memory and provides operations for reading,
 * modifying, and resetting quest nodes. Designed for both "editing mode"
 * (drafts) and "live mode" (published quests).
 * </p>
 *
 * <h3>Characteristics</h3>
 * <ul>
 *   <li>Thread-safe: modifications are synchronized; current state is held
 *       in an {@link AtomicReference} to an immutable {@link Snapshot}.</li>
 *   <li>Supports loading quest data from classpath resources or raw JSON bytes.</li>
 *   <li>Keeps track of the current {@code startId} (entry node).</li>
 *   <li>Provides versioning: either a hash of the loaded data
 *       (live mode) or a timestamp/draft marker (editing mode).</li>
 * </ul>
 *
 * <h3>Modes</h3>
 * <ul>
 *   <li><b>Editing mode</b>: created via {@link #empty(int)}, allows
 *       incremental modifications, tolerates invalid start IDs.</li>
 *   <li><b>Live mode</b>: created via {@link #fromClasspath(String, int)},
 *       requires consistent data, validates links strictly.</li>
 * </ul>
 *
 * <h3>Operations</h3>
 * <ul>
 *   <li>{@link #start()}, {@link #get(int)}, {@link #choose(int, String)} – navigation API.</li>
 *   <li>{@link #deleteNode(int)}, {@link #replaceNode(QuestNode)}, {@link #reload(List, int, boolean)} – mutation API.</li>
 *   <li>{@link #clearDraft(int)}, {@link #setStartId(int)} – reset and configuration.</li>
 *   <li>{@link #nodes()} – snapshot of all nodes sorted by ID.</li>
 * </ul>
 */
@Getter
public class InMemoryQuestStore implements QuestDraftStore {

    private static final Logger log = LoggerFactory.getLogger(InMemoryQuestStore.class);

    /**
     * Immutable snapshot of the quest store state.
     * Holds the {@link QuestNavigator} and version string.
     */
    @Getter
    private static final class Snapshot {
        private final QuestNavigator nav;
        private final String version;

        private Snapshot(QuestNavigator nav, String version) {
            this.nav = nav;
            this.version = version;
        }
    }

    /**
     * Current snapshot reference.
     */
    private final AtomicReference<Snapshot> ref = new AtomicReference<>();
    /**
     * Current start node ID.
     */
    private volatile int startId;
    /**
     * Whether this store is in editing (draft) mode.
     */
    private final boolean editingMode;

    /**
     * Creates a new quest store.
     *
     * @param startId     start node ID
     * @param snapshot    initial snapshot
     * @param editingMode true for editing mode, false for live mode
     */
    private InMemoryQuestStore(int startId, Snapshot snapshot, boolean editingMode) {
        this.startId = startId;
        this.editingMode = editingMode;
        this.ref.set(Objects.requireNonNull(snapshot, "snapshot"));
        log.debug("QuestStore created editingMode={} startId={} version={}",
                editingMode, startId, snapshot.version);
    }

    /**
     * Creates an empty store in editing mode with no nodes.
     *
     * @param startId initial start ID
     * @return new empty quest store
     */
    public static InMemoryQuestStore empty(int startId) {
        return new InMemoryQuestStore(startId, new Snapshot(null, "draft:empty"), true);
    }

    /**
     * Loads quest data from a classpath resource and creates a store in live mode.
     *
     * @param resourceName resource name (e.g. {@code quest.json})
     * @param startId      start node ID
     * @return populated quest store
     * @throws IOException if resource not found or cannot be parsed
     */
    public static InMemoryQuestStore fromClasspath(String resourceName, int startId) throws IOException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try (InputStream in = cl.getResourceAsStream(resourceName)) {
            if (in == null) {
                throw new IOException("Resource not found on classpath: " + resourceName);
            }
            byte[] bytes = in.readAllBytes();
            log.info("Loaded quest resource resource={} bytes={}", resourceName, bytes.length);
            return fromBytes(bytes, startId);
        } catch (IOException e) {
            log.error("Failed to load quest resource resource={} startId={}", resourceName, startId, e);
            throw e;
        }
    }

    private static InMemoryQuestStore fromBytes(byte[] bytes, int startId) throws IOException {
        long t0 = System.nanoTime();
        QuestJsonReader reader = new QuestJsonReader();
        List<QuestNode> nodes = reader.read(new StringReader(new String(bytes, StandardCharsets.UTF_8)));
        QuestNavigator nav = QuestNavigator.from(nodes, startId);
        String version = "sha256:" + sha256(bytes);
        long ms = (System.nanoTime() - t0) / 1_000_000;
        log.info("Quest parsed nodes={} startId={} version={} parseMs={}", nodes.size(), startId, version, ms);
        return new InMemoryQuestStore(startId, new Snapshot(nav, version), false);
    }

    /**
     * Returns current version string of the store.
     * <p>
     * In live mode this is a SHA-256 hash of the resource contents;
     * in editing mode it is a draft marker with a timestamp.
     * </p>
     */
    @Override
    public String version() {
        return ref.get().version;
    }

    /**
     * Returns the start node, or {@code null} if store is empty.
     */
    @Override
    public QuestNode start() {
        QuestNavigator nav = ref.get().nav;
        if (nav == null) {
            log.warn("start() called but navigator is null (empty store), startId={}", startId);
            return null;
        }
        return nav.start();
    }

    /**
     * Returns a node by ID, or {@code null} if not found.
     */
    @Override
    public QuestNode get(int id) {
        QuestNavigator n = ref.get().getNav();
        if (n == null) {
            log.warn("get({}) called but navigator is null", id);
            return null;
        }
        return n.get(id);
    }

    /**
     * Resolves a choice made by a player into the next node.
     *
     * @param fromId current node ID
     * @param answer user answer text
     * @return optional containing next node if found
     */
    @Override
    public Optional<QuestNode> choose(int fromId, String answer) {
        QuestNavigator n = ref.get().getNav();
        if (n == null) {
            log.warn("choose({}, '{}') called but navigator is null", fromId, answer);
            return Optional.empty();
        }
        return n.choose(fromId, answer);
    }

    /**
     * Returns the current start node ID.
     */
    @Override
    public int startId() {
        return startId;
    }

    /**
     * Deletes a node by ID and rebuilds the store.
     *
     * @param id node ID to delete
     * @return true if deleted, false if not found
     */
    @Override
    public synchronized boolean deleteNode(int id) {
        QuestNavigator cur = ref.get().getNav();
        if (cur == null) {
            log.warn("deleteNode({}) skipped: navigator is null", id);
            return false;
        }
        Set<Integer> ids = cur.allIds();
        if (!ids.contains(id)) {
            log.warn("deleteNode({}) skipped: id not found", id);
            return false;
        }
        List<QuestNode> newNodes = new ArrayList<>(Math.max(0, ids.size() - 1));
        for (Integer nid : ids) {
            if (nid == null || nid == id) {
                continue;
            }
            QuestNode q = cur.get(nid);
            if (q != null) {
                newNodes.add(q);
            }
        }
        int newStart = (this.startId == id) ? 0 : this.startId;
        rebuild(newNodes, newStart);
        log.debug("deleteNode({}) done: newSize={} newStartId={}", id, newNodes.size(), newStart);
        return true;
    }

    /**
     * Returns a sorted unmodifiable list of all nodes.
     */
    @Override
    public List<QuestNode> nodes() {
        QuestNavigator n = ref.get().getNav();
        if (n == null) {
            return Collections.emptyList();
        }
        List<QuestNode> res = new ArrayList<>(n.allIds().size());
        for (Integer id : n.allIds()) {
            QuestNode q = n.get(id);
            if (q != null) {
                res.add(q);
            }
        }
        res.sort(Comparator.comparingInt(QuestNode::getId));
        return Collections.unmodifiableList(res);
    }

    /**
     * Replaces or inserts a node, rebuilding the store.
     *
     * @param node node to replace
     */
    @Override
    public synchronized void replaceNode(QuestNode node) {
        Objects.requireNonNull(node, "node");
        QuestNavigator cur = ref.get().nav;
        List<QuestNode> newNodes = new ArrayList<>();
        if (cur != null) {
            for (Integer id : cur.allIds()) {
                QuestNode ex = cur.get(id);
                if (ex != null && ex.getId() != node.getId()) newNodes.add(ex);
            }
        }
        newNodes.add(node);
        int newStartId = (this.startId == 0) ? node.getId() : this.startId;
        rebuild(newNodes, newStartId);
        log.debug("replaceNode(id={}) done: newSize={} newStartId={}", node.getId(), newNodes.size(), newStartId);
    }

    /**
     * Clears all draft data and resets to an empty state.
     *
     * @param newStartId new start node ID
     */
    @Override
    public synchronized void clearDraft(int newStartId) {
        ref.set(new Snapshot(null, "draft:empty"));
        this.startId = newStartId;
        log.debug("clearDraft() set startId={} version=draft:empty", newStartId);
    }

    /**
     * Sets the start node ID. If navigator is available, rebuilds with the new start.
     */
    @Override
    public synchronized void setStartId(int newStartId) {
        this.startId = newStartId;
        if (ref.get().nav != null) {
            rebuild(this.nodes(), newStartId);
            log.debug("setStartId({}) rebuilt navigator", newStartId);
        } else {
            log.warn("setStartId({}) applied but navigator is null", newStartId);
        }
    }

    /**
     * Reloads the store with new nodes and start ID.
     *
     * @param nodes      new nodes
     * @param newStartId new start node ID
     * @param markEdited whether to mark this reload as an edit
     */
    @Override
    public synchronized void reload(List<QuestNode> nodes, int newStartId, boolean markEdited) {
        rebuild(nodes, newStartId);
        this.startId = newStartId;
        log.debug("reload() nodes={} startId={} markEdited={}", (nodes == null ? 0 : nodes.size()), newStartId, markEdited);
    }

    private void rebuild(List<QuestNode> nodes, int start) {
        List<QuestNode> safe = (nodes == null ? List.of() : nodes);
        final QuestNavigator nav;
        if (safe.isEmpty()) {
            nav = null;
        } else {
            boolean invalidStart = (start <= 0) || safe.stream().noneMatch(n -> n.getId() == start);
            int effectiveStart = (editingMode && invalidStart)
                    ? safe.stream().mapToInt(QuestNode::getId).min().orElseThrow()
                    : start;
            if (editingMode && invalidStart) {
                log.warn("rebuild() invalid startId={}, using effectiveStart={} (editingMode)", start, effectiveStart);
            }
            nav = editingMode
                    ? QuestNavigator.editingFrom(safe, effectiveStart)
                    : QuestNavigator.from(safe, start);
        }
        String version = (editingMode ? "draft:" : "live:") + System.currentTimeMillis();
        ref.set(new Snapshot(nav, version));
        this.startId = start;
        log.debug("rebuild() done mode={} nodes={} startId={} version={}",
                (editingMode ? "editing" : "live"), safe.size(), start, version);
    }

    /**
     * Computes a SHA-256 hash of quest bytes.
     *
     * @param data quest data
     * @return hash string, or "unknown" if calculation fails
     */
    private static String sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(data));
        } catch (Exception e) {
            log.warn("sha256 calculation failed, returning 'unknown'", e);
            return "unknown";
        }
    }
}