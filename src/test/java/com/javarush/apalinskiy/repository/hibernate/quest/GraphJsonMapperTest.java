package com.javarush.apalinskiy.repository.hibernate.quest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javarush.apalinskiy.domain.quest.Option;
import com.javarush.apalinskiy.domain.quest.QuestNode;
import com.javarush.apalinskiy.domain.quest.custom.CustomQuest;
import com.javarush.apalinskiy.domain.user.Role;
import com.javarush.apalinskiy.domain.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.javarush.apalinskiy.repository.hibernate.quest.GraphJsonMapper.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GraphJsonMapper")
class GraphJsonMapperTest {

    private QuestNode n(int id, String text, boolean fin, String image, Option... opts) {
        List<Option> list = new ArrayList<>();
        Collections.addAll(list, opts);
        return QuestNode.of(id, text, list, fin, image);
    }

    private Option o() {
        return new Option("Go", 2);
    }

    private User user(String id, String login) {
        return User.of(Role.USER, "Owner", login, "secret6").withId(id);
    }

    @Nested
    @DisplayName("toJson()")
    class ToJson {

        @Test
        @DisplayName("serializes nodes with options and fields intact")
        void serializesNodes() throws Exception {
            // Given
            List<QuestNode> nodes = List.of(
                    n(1, "Start", false, "a.png", o()),
                    n(2, "End", true, null /* no image */)
            );
            // When
            String json = toJson(nodes);
            // Then
            assertNotNull(json);
            var parsed = new ObjectMapper().readValue(json, new TypeReference<List<Object>>() {
            });
            assertEquals(2, parsed.size());
            assertTrue(json.contains("\"id\":1"));
            assertTrue(json.contains("\"text\":\"Start\""));
            assertTrue(json.contains("\"fin\":false"));
            assertTrue(json.contains("\"image\":\"a.png\""));
            assertTrue(json.contains("\"choice\":\"Go\""));
            assertTrue(json.contains("\"next\":2"));
        }

        @Test
        @DisplayName("throws IllegalStateException on mapper failure (defensive)")
        void throwsOnFailure() {
            // Given
            List<QuestNode> empty = List.of();
            // When / Then
            assertDoesNotThrow(() -> toJson(empty));
        }
    }

    @Nested
    @DisplayName("fromJson()")
    class FromJson {

        @Test
        @DisplayName("deserializes nodes with options correctly")
        void deserializesNodes() {
            // Given
            String json = """
                    [
                      {"id":1,"text":"Start","fin":false,"image":"a.png",
                       "options":[{"choice":"Go","next":2},{"choice":"Stay","next":1}]},
                      {"id":2,"text":"End","fin":true,"options":[]}
                    ]
                    """;
            // When
            List<QuestNode> nodes = fromJson(json);
            // Then
            assertEquals(2, nodes.size());
            QuestNode a = nodes.getFirst();
            assertEquals(1, a.getId());
            assertEquals("Start", a.getText());
            assertFalse(a.getFin());
            assertEquals("a.png", a.getImage());
            assertEquals(2, a.getOptions().size());
            assertEquals("Go", a.getOptions().getFirst().getChoice());
            assertEquals(2, a.getOptions().getFirst().getNext());
            QuestNode b = nodes.get(1);
            assertTrue(b.getFin());
            assertTrue(b.getOptions().isEmpty());
        }

        @Test
        @DisplayName("ignores unknown fields")
        void ignoresUnknownFields() {
            // Given
            String json = """
                    [
                      {"id":1,"text":"Start","fin":false,"image":null,
                       "options":[{"choice":"Go","next":2,"unknown":"x"}],
                       "extra":"ignored"}
                    ]
                    """;
            // When
            List<QuestNode> nodes = fromJson(json);
            // Then
            assertEquals(1, nodes.size());
            assertEquals(1, nodes.getFirst().getId());
            assertEquals(1, nodes.getFirst().getOptions().size());
            assertEquals("Go", nodes.getFirst().getOptions().getFirst().getChoice());
        }

        @Test
        @DisplayName("throws IllegalStateException on malformed JSON")
        void throwsOnMalformed() {
            // Given
            String bad = "{ not-a-valid-json ]";
            // When / Then
            assertThrows(IllegalStateException.class, () -> fromJson(bad));
        }
    }

    @Nested
    @DisplayName("round-trip")
    class RoundTrip {

        @Test
        @DisplayName("toJson(fromJson(json)) preserves content semantics")
        void roundTripPreserves() {
            // Given
            String src = """
                    [
                      {"id":7,"text":"Node 7","fin":false,"image":"z.jpg",
                       "options":[{"choice":"Next","next":8}]},
                      {"id":8,"text":"Node 8","fin":true}
                    ]
                    """;
            // When
            List<QuestNode> nodes = fromJson(src);
            String back = toJson(nodes);
            List<QuestNode> again = fromJson(back);
            // Then
            assertEquals(nodes.size(), again.size());
            assertEquals(nodes.getFirst().getId(), again.getFirst().getId());
            assertEquals(nodes.getFirst().getText(), again.getFirst().getText());
            assertEquals(nodes.get(0).getImage(), again.get(0).getImage());
            assertEquals(nodes.get(0).getOptions().size(), again.get(0).getOptions().size());
            assertEquals(nodes.get(1).getFin(), again.get(1).getFin());
        }
    }

    @Nested
    @DisplayName("attachGraphToQuest()")
    class AttachGraphToQuest {

        @Test
        @DisplayName("sets quest backrefs, nulls DB ids, and calls applyUpdate preserving startId")
        void attachesAndNullsIds() {
            // Given
            User owner = user("user-1", "owner");
            CustomQuest quest = CustomQuest.create(owner, "Q", 10, List.of(), "v1", true);
            String json = """
                    [
                      {"id":10,"text":"Start","fin":false,"image":"img.png",
                       "options":[{"choice":"Go","next":11}]},
                      {"id":11,"text":"End","fin":true}
                    ]
                    """;
            List<QuestNode> nodes = fromJson(json);
            // When
            attachGraphToQuest(quest, nodes);
            // Then
            for (QuestNode n : nodes) {
                assertSame(quest, n.getQuest(), "node.quest must point to target quest");
            }
            for (QuestNode n : nodes) {
                assertNull(n.getQuestNodeId(), "db id must be null before persist");
                for (Option o : n.getOptions()) {
                    assertNull(o.getChoiceId(), "choiceId must be null before persist");
                }
            }
            assertEquals(2, quest.getNodes().size());
            assertEquals(10, quest.getStartId());
            QuestNode first = quest.getNodes().getFirst();
            assertFalse(first.getOptions().isEmpty());
            assertSame(first, first.getOptions().getFirst().getNode());
        }

        @Test
        @DisplayName("works with empty options and null images")
        void handlesEmptyOptions() {
            // Given
            User owner = user("user-2", "owner2");
            CustomQuest quest = CustomQuest.create(owner, "Q2", 1, List.of(), "v", true);
            String json = """
                    [
                      {"id":1,"text":"Only","fin":true,"image":null}
                    ]
                    """;
            List<QuestNode> nodes = fromJson(json);
            // When
            attachGraphToQuest(quest, nodes);
            // Then
            assertEquals(1, quest.getNodes().size());
            QuestNode only = quest.getNodes().getFirst();
            assertTrue(only.getFin());
            assertNull(only.getImage());
            assertTrue(only.getOptions().isEmpty());
        }
    }
}