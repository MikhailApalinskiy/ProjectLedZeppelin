package com.javarush.apalinskiy.domain.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents accumulated gameplay statistics for a {@link User}.
 *
 * <p>The {@code UserStats} entity tracks a player’s overall progress and activity
 * on the TextQuest platform, including quests created, completed, and unique
 * endings unlocked. It is linked one-to-one with the {@link User} entity.</p>
 *
 * <p>This entity is persisted in the {@code user_stats} table and also includes
 * a secondary table {@code user_main_endings} to store distinct “main” endings
 * the user has discovered.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_stats")
public class UserStats {

    /**
     * Unique identifier of the user statistics entry (auto-generated).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_stats_id", nullable = false)
    private Long userStatsId;

    /**
     * Total number of quests created by the user.
     */
    @Column(name = "quests_created", nullable = false)
    private Long questsCreated;

    /**
     * Total number of quests completed by the user.
     */
    @Column(name = "quests_completed", nullable = false)
    private Long questsCompleted;

    /**
     * Total number of unique endings unlocked by the user.
     */
    @Column(name = "endings_unlocked", nullable = false)
    private Long endingsUnlocked;

    /**
     * Associated user that owns this statistics record.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false, unique = true)
    private User user;

    /**
     * Collection of unique “main” endings (represented by node IDs)
     * that the user has discovered across all quests.
     *
     * <p>Stored in the {@code user_main_endings} table as a simple element collection.
     * Each combination of {@code user_id} and {@code final_node_id} is unique.</p>
     */
    @ElementCollection
    @CollectionTable(
            name = "user_main_endings",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "user_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "final_node_id"})
    )
    @Column(name = "final_node_id", nullable = false)
    private Set<Integer> mainFinals = new HashSet<>();

    /**
     * Constructs a {@code UserStats} instance with specified counters.
     *
     * @param questsCreated   number of quests created
     * @param questsCompleted number of quests completed
     * @param endingsUnlocked number of endings unlocked
     */
    public UserStats(Long questsCreated, Long questsCompleted, Long endingsUnlocked) {
        this.questsCreated = questsCreated;
        this.questsCompleted = questsCompleted;
        this.endingsUnlocked = endingsUnlocked;
    }
}
