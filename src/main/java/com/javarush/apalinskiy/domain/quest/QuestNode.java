package com.javarush.apalinskiy.domain.quest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.*;

/**
 * Represents a single node in a quest graph.
 * <p>
 * A {@code QuestNode} contains the narrative text, available {@link Option}s,
 * a flag indicating whether it is final, and an optional image reference.
 * </p>
 *
 * <h3>JSON mapping</h3>
 * <ul>
 *   <li>{@code id} – node identifier</li>
 *   <li>{@code text} – narrative text</li>
 *   <li>{@code options} – list of possible choices (empty for final nodes)</li>
 *   <li>{@code final} – boolean flag whether the node is terminal</li>
 *   <li>{@code image} – optional image path or name</li>
 * </ul>
 * Unknown JSON properties are ignored; null values are not serialized.
 *
 * <h3>Validation rules</h3>
 * Enforced at construction time via {@link #validate()}:
 * <ul>
 *   <li>{@code id} must be greater than zero.</li>
 *   <li>{@code text} must not be blank and may not exceed 10,000 characters.</li>
 *   <li>If {@code fin == true}, then {@code options} must be empty.</li>
 *   <li>If {@code fin == false}, each option must have a non-null {@code next}.</li>
 *   <li>Option choices must be unique after normalization.</li>
 * </ul>
 *
 * <p>This class is immutable and uses Lombok {@code @Getter} to expose fields.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public final class QuestNode {

    /**
     * Unique identifier of this node (must be > 0).
     */
    private final int id;
    /**
     * Narrative text displayed to the player.
     */
    private final String text;
    /**
     * List of answer options (empty for final nodes).
     */
    private final List<Option> options;
    /**
     * Flag indicating whether this node is a final/terminal node.
     */
    private final boolean fin;
    /**
     * Optional image reference (trimmed, or {@code null} if blank).
     */
    private final String image;

    /**
     * Creates a new quest node with validation.
     *
     * @param id      unique node identifier (must be > 0)
     * @param text    narrative text (non-blank, max 10,000 chars)
     * @param options list of options (defensively copied, empty if {@code null})
     * @param fin     whether this node is final
     * @param image   optional image reference (trimmed, {@code null} if blank)
     * @throws IllegalArgumentException if validation fails
     */
    private QuestNode(int id, String text, List<Option> options, boolean fin, String image) {
        this.id = id;
        this.text = Objects.requireNonNull(text, "text");
        this.options = List.copyOf(Objects.requireNonNullElseGet(options, List::of));
        this.fin = fin;
        this.image = (image == null || image.isBlank()) ? null : image.trim();
        validate();
    }

    /**
     * Factory method for a general-purpose quest node.
     *
     * @param id      node ID
     * @param text    narrative text
     * @param options list of options
     * @param fin     whether this node is final
     * @param image   optional image reference
     * @return new {@code QuestNode}
     */
    public static QuestNode of(int id, String text, List<Option> options, boolean fin, String image) {
        return new QuestNode(id, text, options, fin, image);
    }

    /**
     * Factory method for a final node (no options).
     *
     * @param id    node ID
     * @param text  narrative text
     * @param image optional image reference
     * @return new final {@code QuestNode}
     */
    public static QuestNode fin(int id, String text, String image) {
        return new QuestNode(id, text, List.of(), true, image);
    }

    /**
     * Factory method for a non-final node with options.
     *
     * @param id      node ID
     * @param text    narrative text
     * @param options list of options
     * @param image   optional image reference
     * @return new non-final {@code QuestNode}
     */
    public static QuestNode nonFin(int id, String text, List<Option> options, String image) {
        return new QuestNode(id, text, options, false, image);
    }

    /**
     * Factory method for JSON deserialization.
     *
     * @param id      node ID
     * @param text    narrative text
     * @param options list of options
     * @param fin     whether this node is final
     * @param image   optional image reference
     * @return new {@code QuestNode} from JSON
     */
    @JsonCreator
    public static QuestNode json(@JsonProperty("id") int id, @JsonProperty("text") String text, @JsonProperty("options") List<Option> options, @JsonProperty("final") boolean fin, @JsonProperty("image") String image) {
        return new QuestNode(id, text, options, fin, image);
    }

    /**
     * Validates invariants of this node.
     *
     * @throws IllegalArgumentException if any constraint is violated
     */
    private void validate() {
        if (id <= 0) {
            throw new IllegalArgumentException("id must be > 0");
        }
        if (text.isBlank()) {
            throw new IllegalArgumentException("text must not be blank");
        }
        if (fin && !options.isEmpty()) {
            throw new IllegalArgumentException("final node should not have options");
        }
        HashSet<String> dupCheck = new HashSet<>();
        for (Option o : options) {
            String n = o.normalizedChoice();
            if (!dupCheck.add(n)) {
                throw new IllegalArgumentException("duplicate option choice: " + o.choice());
            }
            if (o.next() == null) {
                throw new IllegalArgumentException("non-final node option must have next: " + o.choice());
            }
        }
        if (text.length() > 10_000) {
            throw new IllegalArgumentException("text too long");
        }
    }
}
