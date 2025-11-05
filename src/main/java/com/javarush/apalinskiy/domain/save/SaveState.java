package com.javarush.apalinskiy.domain.save;

import com.javarush.apalinskiy.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.*;

/**
 * Represents the global save state container for a user.
 *
 * <p>Each {@code SaveState} is uniquely associated with a single {@link User}
 * and contains all save slot data through related {@link GlobalSlot} entries.
 * It defines how many save slots a user has, manages versioning for optimistic locking,
 * and tracks the last update timestamp.</p>
 *
 * <p>This entity is mapped to the {@code save_states} table and uses the user's
 * {@code user_id} both as its primary key and as a foreign key to {@link User}.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "save_states")
public class SaveState {

    /**
     * Primary key — same as the associated user's ID.
     */
    @Id
    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    /**
     * Associated user who owns this save state.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * Number of available save slots for the user (default is 10).
     */
    @Column(name = "slot_count", nullable = false)
    private int slotCount = 10;

    /**
     * Timestamp of the last modification of the save state.
     */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    /**
     * Entity version used for optimistic locking.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 3;

    /**
     * Collection of the user's global save slots, ordered by slot index.
     */
    @OneToMany(mappedBy = "saveState", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id.slotIndex ASC")
    private List<GlobalSlot> globalSlots = new ArrayList<>();

    /**
     * Default key used to represent the user's main quest save.
     */
    public static final String MAIN_QUEST_KEY = "main";

    /**
     * Normalizes the quest key, returning {@link #MAIN_QUEST_KEY} if the provided ID is null or blank.
     *
     * <p>This utility helps ensure consistent slot lookup for both global and quest-specific saves.</p>
     *
     * @param questIdOrNull quest identifier (nullable)
     * @return normalized quest key string
     */
    public static String questKey(String questIdOrNull) {
        return (questIdOrNull == null || questIdOrNull.isBlank()) ? MAIN_QUEST_KEY : questIdOrNull;
    }

    /**
     * Lifecycle callback — initializes timestamp before first persistence.
     */
    @PrePersist
    void prePersist() {
        if (updatedAt == null) updatedAt = Instant.now();
    }

    /**
     * Lifecycle callback — updates timestamp before entity update.
     */
    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}