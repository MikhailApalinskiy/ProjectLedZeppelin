package com.javarush.apalinskiy.repository.quest;

import com.javarush.apalinskiy.domain.quest.custom.CustomQuest;
import com.javarush.apalinskiy.domain.quest.QuestNode;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing {@link CustomQuest} entities.
 *
 * <p>Defines CRUD and moderation operations for user-created quests,
 * including support for draft staging, moderation approval/rejection,
 * and pagination queries.</p>
 *
 * <p>Implementations (e.g. Hibernate-based) are responsible for handling
 * persistence, versioning, and moderation workflow consistency.</p>
 */
public interface CustomQuestRepository {

    /**
     * Persists a new {@link CustomQuest} entity with its associated graph nodes.
     *
     * @param ownerId     ID of the quest owner
     * @param name        quest title
     * @param startId     starting node ID
     * @param nodes       quest graph nodes
     * @param published   whether the quest should be marked as live immediately
     * @param versionNote optional version or comment label
     */
    void create(String ownerId, String name, int startId, List<QuestNode> nodes,
                boolean published, String versionNote);

    /**
     * Retrieves a quest by its unique identifier.
     *
     * @param id quest ID
     * @return optional quest if found
     */
    Optional<CustomQuest> get(String id);

    /**
     * Counts all live (published) quests, optionally filtered by name substring.
     *
     * @param q optional name filter (case-insensitive)
     * @return total count of matching quests
     */
    int countAllLive(String q);

    /**
     * Retrieves a paginated list of live (published) quests, optionally filtered by name.
     *
     * @param page page number (1-based)
     * @param size page size
     * @param q    optional name filter (case-insensitive)
     * @return list of published quests
     */
    List<CustomQuest> findAllLivePaged(int page, int size, String q);

    /**
     * Updates an existing quest and replaces its graph nodes.
     *
     * @param id          quest ID
     * @param startId     starting node ID
     * @param nodes       updated graph nodes
     * @param published   whether the quest should remain or become live
     * @param versionNote version or comment note
     */
    void update(String id, int startId, List<QuestNode> nodes,
                boolean published, String versionNote);

    /**
     * Deletes a quest by its ID.
     *
     * @param id quest ID
     * @return {@code true} if deleted, {@code false} if not found
     */
    boolean delete(String id);

    /**
     * Deletes a quest only if it belongs to the specified owner.
     *
     * @param id      quest ID
     * @param ownerId owner user ID
     * @return {@code true} if deletion succeeded, {@code false} otherwise
     */
    boolean deleteIfOwner(String id, String ownerId);

    /**
     * Retrieves a paginated list of quests created by a specific owner.
     *
     * @param ownerId  owner user ID
     * @param page     page number (1-based)
     * @param size     page size
     * @param q        optional name filter
     * @param onlyLive if true, returns only published quests
     * @return list of owner’s quests
     */
    List<CustomQuest> findByOwnerPaged(String ownerId, int page, int size, String q, boolean onlyLive);

    /**
     * Counts all quests created by a given owner, optionally filtered by name.
     *
     * @param ownerId  owner user ID
     * @param q        optional name filter
     * @param onlyLive if true, counts only published quests
     * @return total count of matching quests
     */
    int countByOwner(String ownerId, String q, boolean onlyLive);

    /**
     * Represents a pending (unapproved) new quest submission.
     */
    @Getter
    final class PendingNew {
        private final String pendingId;
        private final String ownerId;
        private final String name;
        private final int startId;
        private final List<QuestNode> nodes;
        private final String versionNote;
        private final Instant submittedAt;

        public PendingNew(String pendingId, String ownerId, String name,
                          int startId, List<QuestNode> nodes, String versionNote, Instant submittedAt) {
            this.pendingId = pendingId;
            this.ownerId = ownerId;
            this.name = name;
            this.startId = startId;
            this.nodes = nodes;
            this.versionNote = versionNote;
            this.submittedAt = submittedAt;
        }
    }

    /**
     * Saves a new quest submission for later moderation approval.
     *
     * @param ownerId     ID of the quest owner
     * @param name        quest title
     * @param startId     starting node ID
     * @param nodes       quest nodes
     * @param versionNote optional version note
     */
    void stageCreate(String ownerId, String name, int startId, List<QuestNode> nodes, String versionNote);

    /**
     * Retrieves all pending new quest submissions.
     *
     * @return list of unapproved new quests
     */
    List<PendingNew> listPendingNew();

    /**
     * Approves a previously staged new quest and persists it as a live quest.
     *
     * @param pendingId pending submission ID
     * @return ID of the newly created quest
     */
    String approveCreate(String pendingId);

    /**
     * Rejects and removes a pending new quest submission.
     *
     * @param pendingId pending submission ID
     */
    void rejectCreate(String pendingId);

    /**
     * Represents a pending (unapproved) quest edit submission.
     */
    @Getter
    final class PendingEdit {
        private final String questId;
        private final String ownerId;
        private final String name;
        private final int startId;
        private final List<QuestNode> nodes;
        private final String versionNote;
        private final Instant submittedAt;

        public PendingEdit(String questId, String ownerId, String name,
                           int startId, List<QuestNode> nodes, String versionNote, Instant submittedAt) {
            this.questId = questId;
            this.ownerId = ownerId;
            this.name = name;
            this.startId = startId;
            this.nodes = nodes;
            this.versionNote = versionNote;
            this.submittedAt = submittedAt;
        }
    }

    /**
     * Saves an edit submission of an existing quest for moderation.
     *
     * @param questId     quest ID
     * @param startId     new starting node ID
     * @param nodes       updated graph nodes
     * @param versionNote version note or comment
     */
    void stageEdit(String questId, int startId, List<QuestNode> nodes, String versionNote);

    /**
     * Retrieves all pending quest edit submissions.
     *
     * @return list of unapproved quest edits
     */
    List<PendingEdit> listPendingEdits();

    /**
     * Approves a staged quest edit and updates the quest to the new version.
     *
     * @param questId quest ID
     */
    void approveEdit(String questId);

    /**
     * Rejects and removes a pending quest edit, reverting to the last live version.
     *
     * @param questId quest ID
     */
    void rejectEdit(String questId);
}
