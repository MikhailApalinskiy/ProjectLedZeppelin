package com.javarush.apalinskiy.repository.inmemory.quest;

import com.javarush.apalinskiy.repository.quest.QuestDraftStore;
import com.javarush.apalinskiy.domain.quest.index.QuestNavigator;
import com.javarush.apalinskiy.repository.json.QuestJsonReader;
import com.javarush.apalinskiy.domain.quest.QuestNode;
import lombok.Getter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Getter
public class InMemoryQuestStore implements QuestDraftStore {

    @Getter
    private static final class Snapshot {
        private final QuestNavigator nav;
        private final String version;

        private Snapshot(QuestNavigator nav, String version) {
            this.nav = nav;
            this.version = version;
        }
    }

    private final AtomicReference<Snapshot> ref = new AtomicReference<>();
    private volatile int startId;
    private final boolean editingMode;

    private InMemoryQuestStore(int startId, Snapshot snapshot, boolean editingMode) {
        this.startId = startId;
        this.editingMode = editingMode;
        this.ref.set(Objects.requireNonNull(snapshot, "snapshot"));
    }

    public static InMemoryQuestStore empty(int startId) {
        return new InMemoryQuestStore(startId, new Snapshot(null, "draft:empty"), true);
    }

    public static InMemoryQuestStore fromClasspath(String resourceName, int startId) throws IOException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try (InputStream in = cl.getResourceAsStream(resourceName)) {
            if (in == null) {
                throw new IOException("Resource not found on classpath: " + resourceName);
            }
            byte[] bytes = in.readAllBytes();
            return fromBytes(bytes, startId);
        }
    }

    private static InMemoryQuestStore fromBytes(byte[] bytes, int startId) throws IOException {
        QuestJsonReader reader = new QuestJsonReader();
        List<QuestNode> nodes = reader.read(new StringReader(new String(bytes, StandardCharsets.UTF_8)));
        QuestNavigator nav = QuestNavigator.from(nodes, startId);
        String version = "sha256:" + sha256(bytes);
        return new InMemoryQuestStore(startId, new Snapshot(nav, version), false);
    }

    @Override
    public String version() {
        return ref.get().version;
    }

    @Override
    public QuestNode start() {
        QuestNavigator nav = ref.get().nav;
        return (nav == null) ? null : nav.start();
    }

    @Override
    public QuestNode get(int id) {
        QuestNavigator n = ref.get().getNav();
        return (n == null) ? null : n.get(id);
    }

    @Override
    public Optional<QuestNode> choose(int fromId, String answer) {
        QuestNavigator n = ref.get().getNav();
        return (n == null) ? Optional.empty() : n.choose(fromId, answer);
    }

    @Override
    public int startId() {
        return startId;
    }

    @Override
    public synchronized boolean deleteNode(int id) {
        QuestNavigator cur = ref.get().getNav();
        if (cur == null) {
            return false;
        }
        Set<Integer> ids = cur.allIds();
        if (!ids.contains(id)) {
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
        return true;
    }

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
        int newStartId = (this.startId == 0) ? node.getId() : this.startId; // <-- ключевая строка
        rebuild(newNodes, newStartId);
    }

    @Override
    public synchronized void clearDraft(int newStartId) {
        ref.set(new Snapshot(null, "draft:empty"));
        this.startId = newStartId;
    }

    @Override
    public synchronized void setStartId(int newStartId) {
        this.startId = newStartId;
        if (ref.get().nav != null) {
            rebuild(this.nodes(), newStartId);
        }
    }

    @Override
    public synchronized void reload(List<QuestNode> nodes, int newStartId, boolean markEdited) {
        rebuild(nodes, newStartId);
        this.startId = newStartId;
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
            nav = editingMode
                    ? QuestNavigator.editingFrom(safe, effectiveStart)
                    : QuestNavigator.from(safe, start);
        }
        String version = (editingMode ? "draft:" : "live:") + System.currentTimeMillis();
        ref.set(new Snapshot(nav, version));
        this.startId = start;
    }

    private static String sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(md.digest(data));
        } catch (Exception e) {
            return "unknown";
        }
    }
}