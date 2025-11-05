package com.javarush.apalinskiy.domain.save;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Represents a global save slot entry for a user's saved quest state.
 *
 * <p>Each {@code GlobalSlot} corresponds to one slot within a user's global
 * {@link SaveState}. It stores information about the quest, current node,
 * and metadata such as title and last update time.</p>
 *
 * <p>This entity uses a composite primary key defined by {@link GlobalSlotId},
 * which combines the user identifier and slot index. It is mapped to the
 * {@code save_state_global_slots} table.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "save_state_global_slots")
public class GlobalSlot {

    /**
     * Composite primary key consisting of user ID and slot index.
     */
    @EmbeddedId
    private GlobalSlotId id;

    /**
     * Parent save state to which this slot belongs.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private SaveState saveState;

    /**
     * Identifier of the quest associated with this save slot.
     */
    @Column(name = "quest_id", length = 36, nullable = false)
    private String questId;

    /**
     * Human-readable name of the quest (for display purposes).
     */
    @Column(name = "quest_name", length = 100)
    private String questName;

    /**
     * ID of the quest node where the player was last located.
     */
    @Column(name = "node_id", nullable = false)
    private int nodeId;

    /**
     * Optional custom title assigned by the player to the save slot.
     */
    @Column(name = "title", length = 200)
    private String title;

    /**
     * Timestamp of the last save or update.
     */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Lifecycle callback — initializes timestamp before persisting a new record.
     */
    @PrePersist
    void prePersist() {
        if (updatedAt == null) updatedAt = Instant.now();
    }

    /**
     * Lifecycle callback — updates timestamp whenever the record is modified.
     */
    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
