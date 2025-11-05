package com.javarush.apalinskiy.domain.quest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.javarush.apalinskiy.domain.quest.choice.ChoiceNormalizer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

/**
 * Represents a single answer option within a {@link QuestNode}.
 *
 * <p>Each {@code Option} defines one possible player choice and the corresponding
 * next node to navigate to. Choices are stored in the {@code options} table and
 * linked to their parent node via {@link #node}.</p>
 *
 * <p>Each option may optionally specify a {@code next} node ID. If {@code next} is {@code null},
 * the option leads to a terminal or undefined branch. Choice text is normalized
 * using {@link ChoiceNormalizer} for consistent player input matching.</p>
 *
 * <p>This entity is also serialized to JSON for quest editing and export.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "options")
public class Option {

    /**
     * Maximum allowed length of a choice text.
     */
    private static final int MAX_CHOICE_LENGTH = 255;

    /**
     * Unique database identifier of the choice.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "choice_id", nullable = false)
    private Long choiceId;

    /**
     * Player-visible text of the choice.
     */
    @JsonProperty("choice")
    @Column(name = "choice", nullable = false)
    private String choice;

    /**
     * ID of the next quest node reached if this choice is selected (nullable).
     */
    @JsonProperty("next")
    @Column(name = "next")
    private Integer next;

    /**
     * Parent quest node that owns this choice.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quest_node_db_id", nullable = false)
    private QuestNode node;

    /**
     * Constructs a new {@code Option} instance with the given text and next node ID.
     *
     * @param choice text of the player choice (non-null, max {@value #MAX_CHOICE_LENGTH} characters)
     * @param next   ID of the next node to navigate to (nullable)
     * @throws NullPointerException     if {@code choice} is {@code null}
     * @throws IllegalArgumentException if {@code choice} exceeds {@value #MAX_CHOICE_LENGTH} characters
     */
    public Option(String choice, Integer next) {
        if (choice.length() > MAX_CHOICE_LENGTH) {
            throw new IllegalArgumentException("Choice is too long, it must be less than 255 symbols.");
        }
        this.choice = Objects.requireNonNull(choice, "choice");
        this.next = next;
    }

    /**
     * Returns a normalized version of the choice text.
     *
     * <p>Normalization includes trimming, lowercasing, and collapsing
     * whitespace sequences using {@link ChoiceNormalizer}.</p>
     *
     * @return normalized lowercase string used for comparison
     */
    public String normalizedChoice() {
        return ChoiceNormalizer.normalize(choice);
    }
}
