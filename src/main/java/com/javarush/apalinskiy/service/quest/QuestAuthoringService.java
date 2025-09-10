package com.javarush.apalinskiy.service.quest;

import com.javarush.apalinskiy.repository.quest.QuestDraftStore;
import com.javarush.apalinskiy.repository.quest.QuestStore;
import com.javarush.apalinskiy.domain.quest.Option;
import com.javarush.apalinskiy.domain.quest.index.QuestNavigator;
import com.javarush.apalinskiy.domain.quest.custom.CustomQuest;
import com.javarush.apalinskiy.domain.quest.QuestNode;
import com.javarush.apalinskiy.repository.quest.CustomQuestRepository;
import lombok.Getter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

@Getter
public class QuestAuthoringService {

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
        return editorRepo.deleteNode(id);
    }

    public void saveNode(QuestNode node) {
        editorRepo.replaceNode(node);
    }

    public void setStart(int startId) {
        editorRepo.setStartId(startId);
    }

    public void clearEditorDraft() {
        editorRepo.clearDraft(0);
    }

    public void loadToEditor(String questId) {
        CustomQuest q = catalogRepo.get(Objects.requireNonNull(questId, "questId"))
                .orElseThrow(() -> new IllegalArgumentException("Quest not found: " + questId));
        editorRepo.reload(q.getNodes(), q.getStartId(), false);
    }

    public Optional<CustomQuest> getFromCatalog(String id) {
        return catalogRepo.get(id);
    }

    public List<CustomQuest> listAllFromCatalog() {
        return catalogRepo.listAll();
    }

    public List<CustomQuest> listOwnerFromCatalog(String ownerLogin) {
        return catalogRepo.listByOwner(ownerLogin);
    }

    public void publishNew(String ownerLogin, String questName) {
        Objects.requireNonNull(ownerLogin, "ownerLogin");
        String name = (questName == null || questName.isBlank()) ? "Untitled Quest" : questName.trim();
        Draft d = buildDraftOrThrow();
        catalogRepo.create(ownerLogin, name, d.start, d.nodes, true, d.version);
        editorRepo.reload(Collections.emptyList(), 0, false);
    }

    public void publish(String ownerLogin, String questName) {
        publishNew(ownerLogin, questName);
    }

    public boolean deleteFromCatalogIfOwner(String questId, String ownerLogin) {
        Objects.requireNonNull(questId, "questId");
        Objects.requireNonNull(ownerLogin, "ownerLogin");
        return catalogRepo.deleteIfOwner(questId, ownerLogin);
    }

    public boolean deleteFromCatalogAsAdmin(String questId) {
        Objects.requireNonNull(questId, "questId");
        return catalogRepo.delete(questId);
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
        editorRepo.reload(Collections.emptyList(), 0, false);
    }

    public void updateExisting(String questId, boolean asAdmin) {
        Objects.requireNonNull(questId, "questId");
        catalogRepo.get(questId).orElseThrow(() -> new IllegalArgumentException("Quest not found: " + questId));
        Draft d = buildDraftOrThrow();
        if (asAdmin) {
            catalogRepo.update(questId, d.start, d.nodes, true, d.version);
        } else {
            catalogRepo.stageEdit(questId, d.start, d.nodes, d.version);
        }
    }

    public List<CustomQuestRepository.PendingNew> listPendingNew() {
        return catalogRepo.listPendingNew();
    }

    public List<CustomQuestRepository.PendingEdit> listPendingEdits() {
        return catalogRepo.listPendingEdits();
    }

    public String approveCreate(String pendingId) {
        return catalogRepo.approveCreate(pendingId);
    }

    public void rejectCreate(String pendingId) {
        catalogRepo.rejectCreate(pendingId);
    }

    public void approveEdit(String questId) {
        catalogRepo.approveEdit(questId);
    }

    public void rejectEdit(String questId) {
        catalogRepo.rejectEdit(questId);
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
            throw new IllegalStateException(String.join(" ", errors));
        }
        List<QuestNode> nodes = editorRepo.nodes();
        int start = editorRepo.startId();
        QuestNavigator.from(nodes, start);
        String version = computeVersion(nodes, start);
        return new Draft(nodes, start, version);
    }
}
