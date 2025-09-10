package com.javarush.apalinskiy.repository.quest;

import com.javarush.apalinskiy.domain.quest.custom.CustomQuest;
import com.javarush.apalinskiy.domain.quest.QuestNode;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CustomQuestRepository {

    void create(String ownerLogin, String name, int startId, List<QuestNode> nodes,
                boolean published, String versionNote);

    Optional<CustomQuest> get(String id);

    List<CustomQuest> listAll();

    List<CustomQuest> listByOwner(String ownerLogin);

    void update(String id, int startId, List<QuestNode> nodes,
                boolean published, String versionNote);

    boolean delete(String id);

    boolean deleteIfOwner(String id, String ownerLogin);

    @Getter
    final class PendingNew {
        private final String pendingId;
        private final String ownerLogin;
        private final String name;
        private final int startId;
        private final List<QuestNode> nodes;
        private final String versionNote;
        private final Instant submittedAt;

        public PendingNew(String pendingId, String ownerLogin, String name,
                          int startId, List<QuestNode> nodes, String versionNote, Instant submittedAt) {
            this.pendingId = pendingId;
            this.ownerLogin = ownerLogin;
            this.name = name;
            this.startId = startId;
            this.nodes = nodes;
            this.versionNote = versionNote;
            this.submittedAt = submittedAt;
        }
    }

    void stageCreate(String ownerLogin, String name, int startId, List<QuestNode> nodes, String versionNote);

    List<PendingNew> listPendingNew();

    String approveCreate(String pendingId);

    void rejectCreate(String pendingId);

    @Getter
    final class PendingEdit {
        private final String questId;
        private final String ownerLogin;
        private final String name;
        private final int startId;
        private final List<QuestNode> nodes;
        private final String versionNote;
        private final Instant submittedAt;

        public PendingEdit(String questId, String ownerLogin, String name,
                           int startId, List<QuestNode> nodes, String versionNote, Instant submittedAt) {
            this.questId = questId;
            this.ownerLogin = ownerLogin;
            this.name = name;
            this.startId = startId;
            this.nodes = nodes;
            this.versionNote = versionNote;
            this.submittedAt = submittedAt;
        }
    }

    void stageEdit(String questId, int startId, List<QuestNode> nodes, String versionNote);

    List<PendingEdit> listPendingEdits();

    void approveEdit(String questId);

    void rejectEdit(String questId);
}
