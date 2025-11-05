package com.javarush.apalinskiy.domain.quest.custom;

import com.javarush.apalinskiy.domain.quest.QuestNode;
import com.javarush.apalinskiy.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a user-created custom quest within the TextQuest platform.
 *
 * <p>Each {@code CustomQuest} is an authored, persistent quest entity owned by a specific {@link User}.
 * It contains a list of {@link QuestNode} objects defining the quest structure,
 * metadata such as version and timestamps, and a moderation status
 * indicating whether the quest is public, pending review, or archived.</p>
 *
 * <p>This entity is stored in the {@code custom_quests} table and is managed by Hibernate ORM.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "custom_quests")
public class CustomQuest {

    /**
     * Maximum allowed length for quest names.
     */
    private static final int MAX_QUESTS_NAME = 50;

    /**
     * Moderation and publication status for a custom quest.
     * <ul>
     *   <li>{@link #LIVE} — visible and playable by users</li>
     *   <li>{@link #PENDING_NEW} — awaiting initial moderation approval</li>
     *   <li>{@link #PENDING_EDIT} — updated and awaiting re-approval</li>
     *   <li>{@link #REJECTED} — rejected by moderation</li>
     *   <li>{@link #ARCHIVED} — archived and hidden from public listings</li>
     * </ul>
     */
    public enum ModerationStatus {LIVE, PENDING_NEW, PENDING_EDIT, REJECTED, ARCHIVED}

    /**
     * Unique quest identifier (UUID as string).
     */
    @Id
    @Column(name = "quest_id", nullable = false, length = 36)
    private String id;

    /**
     * Cached owner identifier (non-persistent, derived from {@link #user}).
     */
    @Transient
    private String ownerId;

    /**
     * Quest title displayed in listings and UI.
     */
    @Column(name = "name", nullable = false, length = MAX_QUESTS_NAME)
    private String name;

    /**
     * Identifier of the starting quest node.
     */
    @Column(name = "start_id", nullable = false)
    private Integer startId;

    /**
     * List of quest nodes that compose this quest’s storyline.
     */
    @OneToMany(
            mappedBy = "quest",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<QuestNode> nodes = new ArrayList<>();

    /**
     * Current moderation or publication state of the quest.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false, length = 20)
    private ModerationStatus moderationStatus = ModerationStatus.LIVE;

    /**
     * Internal version label or comment for this quest iteration.
     */
    @Column(name = "version", nullable = false)
    private String version;

    /**
     * Creation timestamp.
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * Last update timestamp.
     */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Quest owner (author).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Constructs a fully initialized {@code CustomQuest} instance.
     *
     * @param id        unique quest ID
     * @param user      quest owner
     * @param name      quest title
     * @param startId   ID of the starting node
     * @param nodes     list of quest nodes
     * @param status    moderation status (defaults to {@link ModerationStatus#LIVE} if {@code null})
     * @param version   quest version label
     * @param createdAt creation timestamp (defaults to {@code Instant.now()} if {@code null})
     * @param updatedAt last update timestamp (defaults to {@code createdAt} if {@code null})
     * @throws IllegalArgumentException if {@code name} is blank or exceeds {@value #MAX_QUESTS_NAME} characters
     * @throws NullPointerException     if required parameters are {@code null}
     */
    public CustomQuest(String id, User user, String name, Integer startId, List<QuestNode> nodes,
                       ModerationStatus status, String version, Instant createdAt, Instant updatedAt) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("Name is required");
        }
        if (name.length() > MAX_QUESTS_NAME) {
            throw new IllegalArgumentException("Name is too long, maximum length is 50 characters");
        }
        this.id = Objects.requireNonNull(id, "id");
        this.user = Objects.requireNonNull(user, "user");
        this.name = name.trim();
        this.startId = Objects.requireNonNull(startId, "startId");
        this.moderationStatus = (status == null ? ModerationStatus.LIVE : status);
        this.version = Objects.requireNonNullElse(version, "");
        this.createdAt = (createdAt == null ? Instant.now() : createdAt);
        this.updatedAt = (updatedAt == null ? this.createdAt : updatedAt);
        setNodesSafe(nodes);
    }

    /**
     * Creates a new {@code CustomQuest} with auto-generated ID and timestamps.
     *
     * @param user               quest owner
     * @param name               quest title
     * @param startId            ID of the starting node
     * @param nodes              quest node list
     * @param version            quest version label
     * @param publishImmediately whether the quest should be published immediately
     * @return a new {@code CustomQuest} instance
     */
    public static CustomQuest create(User user, String name, Integer startId,
                                     List<QuestNode> nodes, String version, Boolean publishImmediately) {
        return new CustomQuest(
                UUID.randomUUID().toString(),
                user,
                name,
                startId,
                nodes,
                publishImmediately ? ModerationStatus.LIVE : ModerationStatus.PENDING_NEW,
                version,
                Instant.now(),
                Instant.now()
        );
    }

    /**
     * Applies updates to quest nodes and metadata.
     *
     * @param newNodes   new quest node list
     * @param newStartId updated start node ID
     * @param newStatus  updated moderation status (optional)
     * @param newVersion new version label (optional)
     */
    public void applyUpdate(List<QuestNode> newNodes, Integer newStartId,
                            ModerationStatus newStatus, String newVersion) {
        setNodesSafe(newNodes);
        this.startId = Objects.requireNonNull(newStartId, "startId");
        if (newStatus != null) {
            this.moderationStatus = newStatus;
        }
        if (newVersion != null) {
            this.version = newVersion;
        }
        this.updatedAt = Instant.now();
    }

    /**
     * Replaces quest nodes safely, updating the {@code quest} reference in each node.
     *
     * @param newNodes new quest node list (nullable)
     */
    private void setNodesSafe(List<QuestNode> newNodes) {
        this.nodes.clear();
        if (newNodes != null) {
            for (QuestNode n : newNodes) {
                if (n == null) {
                    continue;
                }
                n.setQuest(this);
                this.nodes.add(n);
            }
        }
    }

    /**
     * Lifecycle callback: initializes timestamps and defaults before insertion.
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
        if (moderationStatus == null) {
            moderationStatus = ModerationStatus.LIVE;
        }
        if (version == null) {
            version = "";
        }
    }

    /**
     * Lifecycle callback: updates the modification timestamp before update.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Lifecycle callback: syncs {@link #ownerId} after entity load.
     */
    @PostLoad
    private void syncOwnerIdAfterLoad() {
        this.ownerId = (user != null ? user.getUserId() : null);
    }
}
