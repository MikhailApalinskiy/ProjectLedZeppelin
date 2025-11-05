package com.javarush.apalinskiy.domain.quest.custom;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Represents a newly created custom quest that is pending moderation approval.
 *
 * <p>Each {@code PendingNewRow} entry stores a quest submitted by a user
 * for initial review before it becomes publicly available.
 * Once approved, the data is typically promoted to a live {@code CustomQuest} entity.</p>
 *
 * <p>The entity is mapped to the {@code custom_quests_pending_new} table
 * and contains the full quest structure serialized as JSON.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "custom_quests_pending_new")
public class PendingNewRow {

    /**
     * Unique identifier of the pending quest submission.
     */
    @Id
    @Column(name = "pending_id", length = 50, nullable = false)
    private String pendingId;

    /**
     * Identifier of the user who created and submitted this quest.
     */
    @Column(name = "owner_id", nullable = false, length = 36)
    private String ownerId;

    /**
     * Title of the quest being submitted for moderation.
     */
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    /**
     * ID of the starting node in the quest.
     */
    @Column(name = "start_id", nullable = false)
    private Integer startId;

    /**
     * Quest node data serialized as JSON.
     *
     * <p>Contains the full structure of quest nodes, stored temporarily
     * before moderation approval.</p>
     */
    @Column(name = "nodes_json", nullable = false)
    private String nodesJson;

    /**
     * Optional version note or comment added by the author during submission.
     */
    @Column(name = "version_note", nullable = false)
    private String versionNote;

    /**
     * Timestamp indicating when the quest was submitted for moderation.
     */
    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;
}
