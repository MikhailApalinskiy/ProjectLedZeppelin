package com.javarush.apalinskiy.domain.quest.custom;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Represents a pending quest edit awaiting moderation or approval.
 *
 * <p>Each {@code PendingEditRow} entry corresponds to a user-submitted update
 * for an existing {@code CustomQuest}. When a player edits a published quest,
 * the changes are stored here until a moderator reviews and approves them.</p>
 *
 * <p>The entity is mapped to the {@code custom_quests_pending_edit} table
 * and contains quest structure data serialized as JSON.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "custom_quests_pending_edit")
public class PendingEditRow {

    /**
     * Identifier of the quest being edited.
     */
    @Id
    @Column(name = "quest_id", length = 36, nullable = false)
    private String questId;

    /**
     * Identifier of the user who submitted the edit.
     */
    @Column(name = "owner_id", nullable = false, length = 36)
    private String ownerId;

    /**
     * Updated quest name proposed in this edit.
     */
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    /**
     * Updated start node ID for the quest.
     */
    @Column(name = "start_id", nullable = false)
    private Integer startId;

    /**
     * Quest content in JSON format, representing all nodes of the edited version.
     *
     * <p>Used for temporary storage before changes are merged into the main quest entity.</p>
     */
    @Column(name = "nodes_json", nullable = false)
    private String nodesJson;

    /**
     * Optional comment or version note explaining the submitted changes.
     */
    @Column(name = "version_note", nullable = false)
    private String versionNote;

    /**
     * Timestamp of when the edit was submitted for moderation.
     */
    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;
}
