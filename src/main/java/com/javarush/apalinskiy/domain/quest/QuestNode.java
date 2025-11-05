package com.javarush.apalinskiy.domain.quest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.javarush.apalinskiy.domain.quest.custom.CustomQuest;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.*;

/**
 * Represents a single node (step) within a {@link CustomQuest}.
 *
 * <p>Each {@code QuestNode} contains narrative text, a set of {@link Option} choices,
 * and metadata indicating whether it is a terminal (“final”) node.
 * Nodes form the structure of a quest by linking to one another through options.</p>
 *
 * <p>Instances of this class are stored in the {@code quest_nodes} table and
 * serialized to JSON for quest editing and import/export. The class enforces
 * strict validation rules to ensure logical consistency (e.g., no options in final nodes).</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "quest_nodes")
public class QuestNode {

    /**
     * Maximum allowed length for quest node text.
     */
    private static final int MAX_TEXT_LENGTH = 10_000;

    /**
     * Database primary key (auto-generated).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "db_quest_node_id", nullable = false)
    private Long questNodeId;

    /**
     * Logical quest node identifier, unique within a single quest.
     */
    @Column(name = "quest_node_id", nullable = false)
    private Integer id;

    /**
     * Narrative text displayed to the player.
     */
    @Column(name = "text", nullable = false, length = 10_000)
    private String text;

    /**
     * List of available player options originating from this node.
     */
    @OneToMany(
            mappedBy = "node",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<Option> options = new ArrayList<>();

    /**
     * Indicates whether this node is final (no outgoing options).
     */
    @Column(name = "fin", nullable = false)
    private Boolean fin;

    /**
     * Optional image path or name associated with this node.
     */
    @Column(name = "image")
    private String image;

    /**
     * Parent quest that owns this node.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quest_id", nullable = false)
    private CustomQuest quest;

    /**
     * Constructs a validated {@code QuestNode} with given properties.
     *
     * @param id      node identifier within the quest (must be > 0)
     * @param text    main text of the node (non-blank, ≤ {@value #MAX_TEXT_LENGTH})
     * @param options list of available options (empty for final nodes)
     * @param fin     whether the node is final
     * @param image   optional image name or path
     * @throws IllegalArgumentException if text is too long, blank, or inconsistent with {@code fin} flag
     * @throws NullPointerException     if {@code id} or {@code text} is {@code null}
     */
    private QuestNode(Integer id, String text, List<Option> options, Boolean fin, String image) {
        if (text.length() > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException("Text is too long, it must be less than 10000 symbols.");
        }
        this.id = Objects.requireNonNull(id, "id");
        this.text = Objects.requireNonNull(text, "text");
        this.fin = (fin != null && fin);
        this.image = (image == null || image.isBlank()) ? null : image.trim();
        setOptionsSafe(options);
        validate();
    }

    /**
     * Creates a new quest node with provided parameters.
     *
     * @param id      node identifier
     * @param text    node text
     * @param options list of options
     * @param fin     whether the node is final
     * @param image   optional image name or path
     * @return a validated {@code QuestNode} instance
     */
    public static QuestNode of(Integer id, String text, List<Option> options, Boolean fin, String image) {
        return new QuestNode(id, text, options, fin, image);
    }

    /**
     * Creates a final node (no outgoing choices).
     *
     * @param id    node identifier
     * @param text  node text
     * @param image optional image name or path
     * @return a new final {@code QuestNode}
     */
    public static QuestNode fin(Integer id, String text, String image) {
        return new QuestNode(id, text, List.of(), true, image);
    }

    /**
     * Creates a non-final node with available options.
     *
     * @param id      node identifier
     * @param text    node text
     * @param options list of available choices
     * @param image   optional image name or path
     * @return a new non-final {@code QuestNode}
     */
    public static QuestNode nonFin(Integer id, String text, List<Option> options, String image) {
        return new QuestNode(id, text, options, false, image);
    }

    /**
     * JSON factory for deserialization via Jackson.
     *
     * @param id      node ID
     * @param text    node text
     * @param options node options
     * @param fin     whether the node is final
     * @param image   image path
     * @return a new {@code QuestNode} instance
     */
    @JsonCreator
    public static QuestNode json(@JsonProperty("id") Integer id, @JsonProperty("text") String text, @JsonProperty("options") List<Option> options, @JsonProperty("final") Boolean fin, @JsonProperty("image") String image) {
        return new QuestNode(id, text, options, fin, image);
    }

    /**
     * Safely replaces the current list of options, ensuring all have a back-reference to this node.
     *
     * @param opts list of options to assign (may be {@code null})
     */
    public void setOptionsSafe(List<Option> opts) {
        if (this.options == null) {
            this.options = new ArrayList<>();
        }
        this.options.clear();
        if (opts != null) {
            for (Option o : opts) {
                if (o == null) {
                    continue;
                }
                o.setNode(this);
                this.options.add(o);
            }
        }
    }

    /**
     * Validates internal consistency of the node.
     *
     * <p>Checks:
     * <ul>
     *   <li>{@code id > 0}</li>
     *   <li>{@code text} is not blank</li>
     *   <li>final nodes contain no options</li>
     *   <li>no duplicate normalized option texts</li>
     *   <li>non-final node options must have non-null {@code next}</li>
     *   <li>text length ≤ {@value #MAX_TEXT_LENGTH}</li>
     * </ul></p>
     *
     * @throws IllegalArgumentException if validation fails
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
                throw new IllegalArgumentException("duplicate option choice: " + o.getChoice());
            }
            if (o.getNext() == null) {
                throw new IllegalArgumentException("non-final node option must have next: " + o.getChoice());
            }
        }
        if (text.length() > 10_000) {
            throw new IllegalArgumentException("text too long");
        }
    }
}
