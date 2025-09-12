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

@Getter
public class QuestAuthoringService {

    private static final Logger log = LoggerFactory.getLogger(QuestAuthoringService.class);

    private final QuestDraftStore editorRepo;
    private final QuestStore prodRepo;
    private final CustomQuestRepository catalogRepo;

    public QuestAuthoringService(QuestDraftStore editorRepo, QuestStore prodRepo, CustomQuestRepository catalogRepo) {
        this.editorRepo = Objects.requireNonNull(editorRepo);
        this.prodRepo = Objects.requireNonNull(prodRepo);
        this.catalogRepo = Objects.requireNonNull(catalogRepo);
    }

    public QuestNode get(int id) {
        return editorRepo.get(id);
    }

    public List<QuestNode> nodes() {
        return editorRepo.nodes();
    }

    public boolean deleteNode(int id) {
        boolean ok = editorRepo.deleteNode(id);
        log.debug("deleteNode id={} ok={}", id, ok);
        if (!ok) {
            return false;
        }
        List<QuestNode> all = editorRepo.nodes();
        int currentStart = editorRepo.startId();
        if (all.isEmpty()) {
            if (currentStart != 0) {
                editorRepo.setStartId(0);
                log.debug("startId reset to 0 (draft empty)");
            }
            return true;
        }
        if (all.size() == 1) {
            int onlyId = all.getFirst().getId();
            if (currentStart != onlyId) {
                editorRepo.setStartId(onlyId);
                log.debug("startId set to the only remaining node {}", onlyId);
            }
            return true;
        }
        if (currentStart == id) {
            Integer newStart = all.stream()
                    .map(QuestNode::getId).min((a, b) -> {
                        if (a == 1 && b != 1) return -1;
                        if (b == 1 && a != 1) return 1;
                        return Integer.compare(a, b);
                    })
                    .orElse(currentStart);
            if (newStart != currentStart) {
                editorRepo.setStartId(newStart);
                log.debug("startId switched after delete to {}", newStart);
            }
        }
        return true;
    }

    public void saveNode(QuestNode node) {
        boolean existed = editorRepo.get(node.getId()) != null;
        editorRepo.replaceNode(node);
        int currentStart = editorRepo.startId();
        List<QuestNode> all = editorRepo.nodes();
        int count = (all == null ? 0 : all.size());
        if (count == 1) {
            if (currentStart != node.getId()) {
                editorRepo.setStartId(node.getId());
                log.debug("startId auto-set to the only node {}", node.getId());
            }
        } else if (node.getId() == 1 && currentStart != 1) {
            editorRepo.setStartId(1);
            log.debug("startId switched to node #1 by rule");
        } else if (currentStart <= 0) {
            assert all != null;
            int minId = all.stream().mapToInt(QuestNode::getId).min().orElse(node.getId());
            editorRepo.setStartId(minId);
            log.debug("startId was unset, picked min id {}", minId);
        }

        log.debug("saveNode id={} existed={} total={}", node.getId(), existed, count);
    }

    public void setStart(int startId) {
        editorRepo.setStartId(startId);
        log.debug("setStart startId={}", startId);
    }

    public void clearEditorDraft() {
        editorRepo.clearDraft(0);
        log.debug("clearEditorDraft done");
    }

    public void loadToEditor(String questId) {
        CustomQuest q = catalogRepo.get(Objects.requireNonNull(questId, "questId"))
                .orElseThrow(() -> {
                    log.warn("loadToEditor failed: quest not found questId={}", questId);
                    return new IllegalArgumentException("Quest not found: " + questId);
                });
        editorRepo.reload(q.getNodes(), q.getStartId(), false);
        log.info("Editor loaded questId={} name='{}' nodes={} startId={}", questId, q.getName(), q.getNodes().size(), q.getStartId());
    }

    public Optional<CustomQuest> getFromCatalog(String id) {
        return catalogRepo.get(id);
    }

    public List<CustomQuest> listAllFromCatalog() {
        List<CustomQuest> list = catalogRepo.listAll();
        log.debug("listAllFromCatalog size={}", list.size());
        return list;
    }

    public List<CustomQuest> listOwnerFromCatalog(String ownerLogin) {
        List<CustomQuest> list = catalogRepo.listByOwner(ownerLogin);
        log.debug("listOwnerFromCatalog owner={} size={}", ownerLogin, list.size());
        return list;
    }

    public void publishNew(String ownerLogin, String questName) {
        Objects.requireNonNull(ownerLogin, "ownerLogin");
        String name = (questName == null || questName.isBlank()) ? "Untitled Quest" : questName.trim();
        Draft d = buildDraftOrThrow();
        catalogRepo.create(ownerLogin, name, d.start, d.nodes, true, d.version);
        log.info("Quest published owner={} name='{}' nodes={} startId={} version={}",
                ownerLogin, name, d.nodes.size(), d.start, d.version);
        editorRepo.reload(Collections.emptyList(), 0, false);
    }

    public void publish(String ownerLogin, String questName) {
        publishNew(ownerLogin, questName);
    }

    public boolean deleteFromCatalogIfOwner(String questId, String ownerLogin) {
        boolean ok = catalogRepo.deleteIfOwner(questId, ownerLogin);
        if (ok) {
            log.info("Quest deleted by owner questId={} owner={}", questId, ownerLogin);
        } else {
            log.warn("Delete by owner skipped questId={} owner={} (not found / not owner)", questId, ownerLogin);
        }
        return ok;
    }

    public boolean deleteFromCatalogAsAdmin(String questId) {
        boolean ok = catalogRepo.delete(questId);
        if (ok) {
            log.info("Quest deleted by admin questId={}", questId);
        } else {
            log.warn("Delete by admin skipped questId={} (not found)", questId);
        }
        return ok;
    }

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

    public void submitNewForModeration(String ownerLogin, String questName) {
        Objects.requireNonNull(ownerLogin, "ownerLogin");
        String name = (questName == null || questName.isBlank()) ? "Untitled Quest" : questName.trim();
        Draft d = buildDraftOrThrow();
        catalogRepo.stageCreate(ownerLogin, name, d.start, d.nodes, d.version);
        log.info("Quest submitted for moderation owner={} name='{}' nodes={} startId={} version={}",
                ownerLogin, name, d.nodes.size(), d.start, d.version);
        editorRepo.reload(Collections.emptyList(), 0, false);
    }

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

    public List<CustomQuestRepository.PendingNew> listPendingNew() {
        List<CustomQuestRepository.PendingNew> list = catalogRepo.listPendingNew();
        log.debug("listPendingNew size={}", list.size());
        return list;
    }

    public List<CustomQuestRepository.PendingEdit> listPendingEdits() {
        List<CustomQuestRepository.PendingEdit> list = catalogRepo.listPendingEdits();
        log.debug("listPendingEdits size={}", list.size());
        return list;
    }

    public String approveCreate(String pendingId) {
        String id = catalogRepo.approveCreate(pendingId);
        log.info("Approved NEW pendingId={} -> questId={}", pendingId, id);
        return id;
    }

    public void rejectCreate(String pendingId) {
        catalogRepo.rejectCreate(pendingId);
        log.info("Rejected NEW pendingId={}", pendingId);
    }

    public void approveEdit(String questId) {
        catalogRepo.approveEdit(questId);
        log.info("Approved EDIT questId={}", questId);
    }

    public void rejectEdit(String questId) {
        catalogRepo.rejectEdit(questId);
        log.info("Rejected EDIT questId={}", questId);
    }

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
