package com.javarush.apalinskiy.domain.quest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.javarush.apalinskiy.domain.quest.choice.ChoiceNormalizer;
import lombok.Getter;

import java.util.Locale;
import java.util.Objects;

/**
 * Represents a single answer option available in a {@link QuestNode}.
 * <p>
 * An {@code Option} contains the raw choice text shown to the player
 * and the ID of the next node to jump to if selected.
 * </p>
 *
 * <h3>JSON mapping</h3>
 * <ul>
 *   <li>{@code choice} → user-facing answer text</li>
 *   <li>{@code next} → ID of the next node (may be {@code null} for final nodes)</li>
 * </ul>
 * Unknown JSON properties are ignored during deserialization.
 *
 * <h3>Normalization</h3>
 * The {@link #normalizedChoice()} method provides a lowercased,
 * trimmed, and whitespace-collapsed version of {@link #choice},
 * making user input comparison reliable.
 *
 * @param choice raw answer text (non-null)
 * @param next   ID of the next node, or {@code null} if this option does not lead anywhere
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Option(@Getter @JsonProperty("choice") String choice,
                     @Getter @JsonProperty("next") Integer next) {

    /**
     * Compact constructor that validates the {@code choice} is non-null.
     *
     * @param choice raw answer text
     * @param next   ID of the next node (nullable)
     * @throws NullPointerException if {@code choice} is null
     */
    public Option {
        choice = Objects.requireNonNull(choice, "choice");
    }

    /**
     * Returns the normalized version of {@link #choice}.
     * <p>
     * Normalization rules:
     * <ul>
     *   <li>Collapse consecutive whitespace into a single space.</li>
     *   <li>Trim leading/trailing whitespace.</li>
     *   <li>Convert to lowercase using {@link Locale#ROOT}.</li>
     * </ul>
     *
     * @return normalized choice text, never {@code null}
     */
    public String normalizedChoice() {
        return ChoiceNormalizer.normalize(choice);
    }
}
