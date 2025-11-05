package com.javarush.apalinskiy.domain.quest.custom;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Represents a draft version of a user-created quest.
 *
 * <p>Each {@code DraftRow} stores an intermediate, unpublished version
 * of a custom quest. Drafts are used by the quest editor before publishing
 * or submitting a quest for moderation.</p>
 *
 * <p>The class is mapped to the {@code custom_quest_drafts} table and
 * contains the quest data serialized as JSON ({@link #nodesJson}).</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "custom_quest_drafts")
public class DraftRow {

    /**
     * Unique identifier of the draft (UUID or short string ID).
     */
    @Id
    @Column(name = "draft_id", length = 50, nullable = false)
    private String draftId;

    /**
     * Identifier of the user who owns this draft.
     */
    @Column(name = "owner_id", length = 36, nullable = false)
    private String ownerId;

    /**
     * Optional reference to the target quest that this draft updates (nullable).
     */
    @Column(name = "target_quest_id", length = 36)
    private String targetQuestId;

    /**
     * Name of the quest being drafted.
     */
    @Column(name = "name", length = 50, nullable = false)
    private String name;

    /**
     * Identifier of the starting node within the draft.
     */
    @Column(name = "start_id", nullable = false)
    private Integer startId;

    /**
     * Serialized quest nodes in JSON format.
     *
     * <p>Contains the structure of {@code QuestNode} objects represented as text.
     * This data is not relationally stored to allow efficient editing and previewing.</p>
     */
    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Column(name = "nodes_json", nullable = false, columnDefinition = "text")
    private String nodesJson;

    /**
     * Optional version note or short comment about this draft version.
     */
    @Column(name = "version_note", nullable = false)
    private String versionNote = "";

    /**
     * Timestamp of the last modification.
     */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
