package com.javarush.apalinskiy.repository.quest;

import com.javarush.apalinskiy.domain.quest.custom.CustomQuest;
import com.javarush.apalinskiy.domain.quest.QuestNode;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Repository interface for managing user-created (custom) quests.
 * <p>
 * Provides CRUD operations for {@link CustomQuest} as well as
 * staging mechanisms for moderation workflows (pending create/edit).
 * </p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Store and retrieve {@link CustomQuest} entities.</li>
 *   <li>Support listing all quests or only those by a specific owner.</li>
 *   <li>Enforce uniqueness of quest IDs.</li>
 *   <li>Allow staging new quests and edits for moderation before publishing.</li>
 * </ul>
 *
 * <h3>Moderation Workflow</h3>
 * <p>
 * When a user submits a new quest or edits an existing one,
 * the change is stored in a {@link PendingNew} or {@link PendingEdit}
 * entry until approved or rejected by an administrator.
 * </p>
 */
public interface CustomQuestRepository {

    /**
     * Creates and persists a new quest immediately (without staging).
     *
     * @param ownerLogin  quest owner's login
     * @param name        quest name
     * @param startId     ID of the start node
     * @param nodes       list of quest nodes
     * @param published   whether quest is published immediately
     * @param versionNote version or changelog note
     */
    void create(String ownerLogin, String name, int startId, List<QuestNode> nodes,
                boolean published, String versionNote);

    /**
     * Retrieves a quest by its ID.
     *
     * @param id quest ID
     * @return optional containing the quest if found
     */
    Optional<CustomQuest> get(String id);

    /**
     * Lists all quests, usually sorted by update time (newest first).
     *
     * @return list of all quests
     */
    List<CustomQuest> listAll();

    /**
     * Lists quests owned by the specified user.
     *
     * @param ownerLogin owner login
     * @return list of quests owned by the user
     */
    List<CustomQuest> listByOwner(String ownerLogin);

    /**
     * Updates an existing quest.
     *
     * @param id          quest ID
     * @param startId     new start node ID
     * @param nodes       updated list of nodes
     * @param published   whether quest is published
     * @param versionNote version or changelog note
     */
    void update(String id, int startId, List<QuestNode> nodes,
                boolean published, String versionNote);

    /**
     * Deletes a quest by its ID.
     *
     * @param id quest ID
     * @return true if the quest was removed, false if not found
     */
    boolean delete(String id);

    /**
     * Deletes a quest only if the given user is its owner.
     *
     * @param id         quest ID
     * @param ownerLogin expected owner login
     * @return true if deleted, false otherwise
     */
    boolean deleteIfOwner(String id, String ownerLogin);

    /**
     * Represents a new quest submitted for moderation.
     */
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

    /**
     * Stages a new quest for moderation (not visible until approved).
     *
     * @param ownerLogin  quest owner's login
     * @param name        quest name
     * @param startId     start node ID
     * @param nodes       quest nodes
     * @param versionNote version note or changelog
     */
    void stageCreate(String ownerLogin, String name, int startId, List<QuestNode> nodes, String versionNote);

    /**
     * Lists all staged new quests awaiting moderation.
     *
     * @return list of pending new quests
     */
    List<PendingNew> listPendingNew();

    /**
     * Approves a staged quest creation and persists it as a new quest.
     *
     * @param pendingId ID of the staged quest
     * @return ID of the newly created quest
     * @throws NoSuchElementException if pending item is not found
     */
    String approveCreate(String pendingId);

    /**
     * Rejects a staged quest creation (removes it from staging).
     *
     * @param pendingId ID of the staged quest
     * @throws NoSuchElementException if pending item is not found
     */
    void rejectCreate(String pendingId);

    /**
     * Represents an edit to an existing quest submitted for moderation.
     */
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

    /**
     * Stages an edit to an existing quest for moderation.
     *
     * @param questId     quest being edited
     * @param startId     new start node ID
     * @param nodes       updated nodes
     * @param versionNote version or changelog note
     * @throws NoSuchElementException if the quest does not exist
     */
    void stageEdit(String questId, int startId, List<QuestNode> nodes, String versionNote);

    /**
     * Lists all staged edits awaiting moderation.
     *
     * @return list of pending edits
     */
    List<PendingEdit> listPendingEdits();

    /**
     * Approves a staged edit and applies it to the quest.
     *
     * @param questId quest being edited
     * @throws NoSuchElementException if pending edit is not found
     */
    void approveEdit(String questId);

    /**
     * Rejects a staged edit (removes it from staging).
     *
     * @param questId quest being edited
     * @throws NoSuchElementException if pending edit is not found
     */
    void rejectEdit(String questId);
}
