package com.javarush.apalinskiy.repository.hibernate.quest;

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
 * In-memory implementation of {@link QuestDraftStore} that maintains a loaded quest graph
 * (nodes and navigation) entirely in RAM.
 *
 * <p>Supports both “editing” and “live” modes: in editing mode, graph integrity checks are
 * more permissive to allow intermediate edits. The class holds an atomic {@link Snapshot}
 * containing the current {@link QuestNavigator} and its version identifier.</p>
 *
 * <p>This store is typically used for quest authoring, draft editing, or fast read-only
 * playback without database access.</p>
 *
 * <p>All mutating methods are synchronized to preserve consistency across threads.
 * Lightweight getters use atomic and volatile fields for concurrent reads.</p>
 */
@Getter
public class InMemoryQuestStore implements QuestDraftStore {

    private static final Logger log = LoggerFactory.getLogger(InMemoryQuestStore.class);

    /**
     * Immutable snapshot of the quest state consisting of the {@link QuestNavigator}
     * and its version label.
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

    /** Holds the current snapshot atomically for thread-safe reads. */
    private final AtomicReference<Snapshot> ref = new AtomicReference<>();

    /** Identifier of the quest's start node. */
    private volatile int startId;

    /** Indicates whether the store is operating in editing mode. */
    private final boolean editingMode;


    private InMemoryQuestStore(int startId, Snapshot snapshot, boolean editingMode) {
        this.startId = startId;
        this.editingMode = editingMode;
        this.ref.set(Objects.requireNonNull(snapshot, "snapshot"));
        log.debug("QuestStore created editingMode={} startId={} version={}",
                editingMode, startId, snapshot.version);
    }

    /**
     * Creates an empty, editable quest store with no nodes and version {@code draft:empty}.
     *
     * @param startId starting node ID for future graph initialization
     * @return a new editable quest store
     */
    public static InMemoryQuestStore empty(int startId) {
        log.info("Creating empty QuestStore startId={}", startId);
        return new InMemoryQuestStore(startId, new Snapshot(null, "draft:empty"), true);
    }

    /**
     * Loads a quest graph from a JSON resource on the classpath.
     *
     * @param resourceName resource path (relative to classpath root)
     * @param startId      start node ID
     * @return an initialized quest store in read-only mode
     * @throws IOException if the resource cannot be found or parsed
     */
    public static InMemoryQuestStore fromClasspath(String resourceName, int startId) throws IOException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try (InputStream in = cl.getResourceAsStream(resourceName)) {
            if (in == null) {
                log.error("Resource not found on classpath: {}", resourceName);
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

    /**
     * Builds a quest store from raw JSON bytes, parsing nodes and computing a SHA-256 version hash.
     *
     * @param bytes   UTF-8 encoded JSON bytes
     * @param startId start node ID
     * @return a new non-editing quest store
     * @throws IOException if parsing fails
     */
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
     * Returns the current quest version string (e.g., {@code live:timestamp} or {@code draft:empty}).
     */
    @Override
    public String version() {
        String v = ref.get().version;
        log.debug("version() -> {}", v);
        return v;
    }

    /**
     * Returns the start node of the quest or {@code null} if the store is empty.
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
     * Retrieves a quest node by ID, or {@code null} if missing.
     *
     * @param id logical node ID
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
     * Performs a transition by evaluating a user answer from a given node.
     *
     * @param fromId starting node ID
     * @param answer player's textual choice
     * @return optional next node if valid
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
        int id = startId;
        log.debug("startId() -> {}", id);
        return id;
    }

    /**
     * Deletes a node by ID and rebuilds the internal navigator.
     *
     * <p>Returns {@code false} if the node ID does not exist or the navigator is empty.</p>
     *
     * @param id node ID to delete
     * @return {@code true} if deleted successfully
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
     * Returns an immutable, sorted list of all quest nodes by ID.
     */
    @Override
    public List<QuestNode> nodes() {
        QuestNavigator n = ref.get().getNav();
        if (n == null) {
            log.debug("nodes() -> empty (navigator is null)");
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
     * Replaces or inserts a node into the graph, rebuilding navigation afterward.
     *
     * @param node node to insert or replace
     */
    @Override
    public synchronized void replaceNode(QuestNode node) {
        Objects.requireNonNull(node, "node");
        QuestNavigator cur = ref.get().nav;
        List<QuestNode> newNodes = new ArrayList<>();
        if (cur != null) {
            for (Integer id : cur.allIds()) {
                QuestNode ex = cur.get(id);
                if (ex != null && !Objects.equals(ex.getId(), node.getId())) newNodes.add(ex);
            }
        }
        newNodes.add(node);
        int newStartId = (this.startId == 0) ? node.getId() : this.startId;
        rebuild(newNodes, newStartId);
        log.debug("replaceNode(id={}) done: newSize={} newStartId={}", node.getId(), newNodes.size(), newStartId);
    }

    /**
     * Clears the current draft, resets the start node ID, and marks version as {@code draft:empty}.
     *
     * @param newStartId new start ID to set
     */
    @Override
    public synchronized void clearDraft(int newStartId) {
        ref.set(new Snapshot(null, "draft:empty"));
        this.startId = newStartId;
        log.debug("clearDraft() set startId={} version=draft:empty", newStartId);
    }

    /**
     * Updates the start node ID and rebuilds the navigator if available.
     *
     * @param newStartId new start node ID
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
     * Reloads the quest with a new list of nodes and start node ID.
     *
     * @param nodes      new quest nodes
     * @param newStartId new start node ID
     * @param markEdited whether to mark the version as edited
     */
    @Override
    public synchronized void reload(List<QuestNode> nodes, int newStartId, boolean markEdited) {
        rebuild(nodes, newStartId);
        this.startId = newStartId;
        log.debug("reload() nodes={} startId={} markEdited={}", (nodes == null ? 0 : nodes.size()), newStartId, markEdited);
    }

    /**
     * Rebuilds the internal {@link QuestNavigator} and updates the snapshot version.
     *
     * <p>Handles cases where the start ID is invalid by falling back to node #1 or the smallest ID.</p>
     *
     * @param nodes list of quest nodes
     * @param start desired start node ID
     */
    private void rebuild(List<QuestNode> nodes, int start) {
        List<QuestNode> safe = (nodes == null ? List.of() : nodes);
        final QuestNavigator nav;
        if (safe.isEmpty()) {
            nav = null;
        } else {
            boolean has1 = safe.stream().anyMatch(n -> n.getId() == 1);
            int tmpStart = has1 ? 1 : start;
            int finalTmpStart = tmpStart;
            boolean invalidStart = (tmpStart <= 0) || safe.stream().noneMatch(n -> n.getId() == finalTmpStart);
            if (invalidStart) {
                tmpStart = safe.stream().mapToInt(QuestNode::getId).min().orElseThrow();
            }
            nav = editingMode
                    ? QuestNavigator.editingFrom(safe, tmpStart)
                    : QuestNavigator.from(safe, tmpStart);
            start = tmpStart; // 👈 после всех стримов
        }
        String version = (editingMode ? "draft:" : "live:") + System.currentTimeMillis();
        ref.set(new Snapshot(nav, version));
        this.startId = start;
        log.debug("rebuild() done mode={} nodes={} startId={} version={}",
                (editingMode ? "editing" : "live"), safe.size(), start, version);
    }

    /**
     * Computes the SHA-256 hash of quest JSON bytes and returns it in hex format.
     *
     * @param data input data
     * @return lowercase hex string or {@code "unknown"} if hashing fails
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