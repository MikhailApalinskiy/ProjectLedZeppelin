package com.javarush.apalinskiy.repository.hibernate.quest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.javarush.apalinskiy.domain.quest.Option;
import com.javarush.apalinskiy.domain.quest.QuestNode;
import com.javarush.apalinskiy.domain.quest.custom.CustomQuest;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class responsible for serializing and deserializing quest graphs
 * (composed of {@link QuestNode} and {@link Option}) to and from JSON format.
 *
 * <p>The {@code GraphJsonMapper} is used primarily when saving or loading
 * quest structures in the editor, draft repository, or moderation workflows.
 * It converts a graph of interconnected nodes into a simplified JSON structure
 * that can be safely persisted or transmitted.</p>
 *
 * <p>This class uses Jackson’s {@link ObjectMapper} with relaxed settings
 * (ignoring unknown properties and disabling {@link SerializationFeature#FAIL_ON_EMPTY_BEANS}).</p>
 *
 * <h2>Example JSON structure</h2>
 * <pre>{@code
 * [
 *   {
 *     "id": 1,
 *     "text": "You wake up in a dark room.",
 *     "fin": false,
 *     "image": "dark_room.png",
 *     "options": [
 *       { "choice": "Light a candle", "next": 2 },
 *       { "choice": "Stay still", "next": 3 }
 *     ]
 *   },
 *   {
 *     "id": 2,
 *     "text": "You see an exit.",
 *     "fin": true
 *   }
 * ]
 * }</pre>
 */
public final class GraphJsonMapper {

    /**
     * Shared JSON mapper instance configured for relaxed serialization.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

    private GraphJsonMapper() {
    }

    /**
     * Lightweight JSON representation of a quest option.
     *
     * <p>Used internally for serialization and deserialization of {@link Option} objects.
     * Fields are public to simplify Jackson mapping.</p>
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class JsOption {

        /**
         * Option text shown to the player.
         */
        public String choice;

        /**
         * Identifier of the next node reached after this choice.
         */
        public Integer next;
    }

    /**
     * Lightweight JSON representation of a quest node.
     *
     * <p>Used internally for serialization and deserialization of {@link QuestNode} objects.
     * Contains basic text and choice data but omits parent quest references.</p>
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class JsNode {

        /**
         * Logical ID of the quest node.
         */
        public Integer id;

        /**
         * Text content of the node.
         */
        public String text;

        /**
         * Whether the node is final (no options).
         */
        public Boolean fin;

        /**
         * Optional image associated with the node.
         */
        public String image;

        /**
         * Available options from this node.
         */
        public List<JsOption> options = new ArrayList<>();
    }

    /**
     * Serializes a list of {@link QuestNode} entities into JSON.
     *
     * @param nodes list of quest nodes to serialize
     * @return JSON string representing the quest graph
     * @throws IllegalStateException if serialization fails
     */
    public static String toJson(List<QuestNode> nodes) {
        try {
            List<JsNode> out = new ArrayList<>(nodes.size());
            for (QuestNode n : nodes) {
                JsNode jn = new JsNode();
                jn.id = n.getId();
                jn.text = n.getText();
                jn.fin = n.getFin();
                jn.image = n.getImage();
                jn.options = new ArrayList<>();
                if (n.getOptions() != null) {
                    for (Option o : n.getOptions()) {
                        if (o == null) {
                            continue;
                        }
                        JsOption jo = new JsOption();
                        jo.choice = o.getChoice();
                        jo.next = o.getNext();
                        jn.options.add(jo);
                    }
                }
                out.add(jn);
            }
            return MAPPER.writeValueAsString(out);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize nodes to JSON", e);
        }
    }

    /**
     * Deserializes a JSON string into a list of {@link QuestNode} objects.
     *
     * @param json JSON representation of quest nodes
     * @return deserialized list of {@link QuestNode} instances
     * @throws IllegalStateException if deserialization fails
     */
    public static List<QuestNode> fromJson(String json) {
        try {
            JsNode[] src = MAPPER.readValue(json, JsNode[].class);
            List<QuestNode> out = new ArrayList<>(src.length);
            for (JsNode jn : src) {
                List<Option> opts = new ArrayList<>();
                if (jn.options != null) {
                    for (JsOption jo : jn.options) {
                        if (jo == null) {
                            continue;
                        }
                        opts.add(new Option(jo.choice, jo.next));
                    }
                }
                QuestNode n = QuestNode.of(jn.id, jn.text, opts, jn.fin, jn.image);
                out.add(n);
            }
            return out;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize nodes from JSON", e);
        }
    }

    /**
     * Attaches deserialized quest nodes to a given {@link CustomQuest} instance.
     *
     * <p>This method resets database IDs of nodes and options
     * and updates the quest’s internal graph state to match the provided nodes.</p>
     *
     * @param quest target quest entity
     * @param nodes deserialized list of quest nodes
     */
    public static void attachGraphToQuest(CustomQuest quest, List<QuestNode> nodes) {
        for (QuestNode n : nodes) {
            n.setQuest(quest);
            n.setQuestNodeId(null);
            if (n.getOptions() != null) {
                for (Option o : n.getOptions()) {
                    o.setNode(n);
                    o.setChoiceId(null);
                }
            }
        }
        quest.applyUpdate(nodes, quest.getStartId(), quest.getModerationStatus(), quest.getVersion());
    }
}
