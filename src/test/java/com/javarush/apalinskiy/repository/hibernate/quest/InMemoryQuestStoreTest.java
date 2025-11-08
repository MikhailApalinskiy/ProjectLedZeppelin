package com.javarush.apalinskiy.repository.hibernate.quest;

import com.javarush.apalinskiy.domain.quest.Option;
import com.javarush.apalinskiy.domain.quest.QuestNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InMemoryQuestStore — unit tests (granular BDD style)")
class InMemoryQuestStoreTest {

    private static QuestNode node(int id, String title, List<Option> options, boolean terminal) {
        return QuestNode.of(id, title, options, terminal, null);
    }

    private static Option go() {
        return new Option("Next", 2);
    }

    private static List<QuestNode> simpleTwo() {
        return List.of(
                node(1, "Start", List.of(go()), false),
                node(2, "End", List.of(), true)
        );
    }

    @Nested
    @DisplayName("empty(startId)")
    class EmptyFactory {

        @Test
        @DisplayName("creates editable store with draft:empty version and null navigator")
        void createsEditableEmpty() {
            // given
            int startId = 42;
            // when
            InMemoryQuestStore store = InMemoryQuestStore.empty(startId);
            // then
            assertEquals("draft:empty", store.version());
            assertEquals(startId, store.startId());
            assertNull(store.start(), "start() should be null for empty store");
            assertNull(store.get(1), "get() should be null for empty store");
            assertTrue(store.nodes().isEmpty(), "nodes() should be empty");
        }
    }

    @Nested
    @DisplayName("version / start / get / choose with empty store")
    class BasicAccessOnEmpty {

        InMemoryQuestStore store;

        @BeforeEach
        void setUp() {
            store = InMemoryQuestStore.empty(7);
        }

        @Test
        @DisplayName("version() returns draft:empty")
        void versionDraftEmpty() {
            // when
            String v = store.version();
            // then
            assertEquals("draft:empty", v);
        }

        @Test
        @DisplayName("start() returns null")
        void startIsNull() {
            // when / then
            assertNull(store.start());
        }

        @Test
        @DisplayName("get(id) returns null")
        void getIsNull() {
            // when / then
            assertNull(store.get(123));
        }

        @Test
        @DisplayName("choose(fromId, answer) returns Optional.empty()")
        void chooseEmpty() {
            // when
            Optional<QuestNode> next = store.choose(1, "anything");
            // then
            assertTrue(next.isEmpty());
        }
    }

    @Nested
    @DisplayName("reload(nodes, startId, markEdited)")
    class ReloadBehavior {

        InMemoryQuestStore store;

        @BeforeEach
        void init() {
            store = InMemoryQuestStore.empty(0);
        }

        @Test
        @DisplayName("replaces graph; nodes() sorted; start() and get() are available")
        void reloadBuildsNavigator() {
            // given
            List<QuestNode> nodes = simpleTwo();
            // when
            store.reload(nodes, 1, true);
            // then
            assertTrue(store.version().startsWith("draft:"), "empty() creates editing store -> draft:* after rebuild");
            assertNotNull(store.start());
            assertEquals(1, store.startId());
            assertEquals(2, store.nodes().size());
            assertEquals(1, store.nodes().get(0).getId());
            assertEquals(2, store.nodes().get(1).getId());
            assertEquals(2, store.get(2).getId());
            assertTrue(store.choose(1, "Next").isPresent());
            assertEquals(2, store.choose(1, "Next").get().getId());
        }

        @Test
        @DisplayName("invalid start -> falls back to smallest id (no id=1 present)")
        void invalidStartFallsBackToMin() {
            // given
            List<QuestNode> nodes = List.of(
                    node(5, "A", List.of(), false),
                    node(7, "B", List.of(), true)
            );
            // when
            store.reload(nodes, 999, true);
            // then
            assertEquals(999, store.startId(), "Field keeps requested startId");
            assertNotNull(store.start());
            assertEquals(5, store.start().getId(), "Navigator falls back to min id");
        }

        @Test
        @DisplayName("if id=1 exists -> start forced to 1 regardless of provided start")
        void id1ForcesStartToOne() {
            // given
            List<QuestNode> nodes = List.of(
                    node(1, "One", List.of(), false),
                    node(5, "Five", List.of(), true)
            );
            // when
            store.reload(nodes, 5, true);
            // then
            assertEquals(5, store.startId(), "Field keeps requested startId even if id=1 exists");
            assertNotNull(store.start());
            assertEquals(1, store.start().getId(), "Navigator start forced to id=1");
        }

        @Test
        @DisplayName("reload(null) → empty navigator; nodes() empty; start() null; startId set")
        void reloadNullClears() {
            // when
            store.reload(null, 123, true);
            // then
            assertEquals(123, store.startId());
            assertTrue(store.nodes().isEmpty());
            assertNull(store.start());
        }
    }

    @Nested
    @DisplayName("replaceNode(node)")
    class ReplaceNodeBehavior {

        InMemoryQuestStore store;

        @BeforeEach
        void init() {
            store = InMemoryQuestStore.empty(0);
            store.reload(simpleTwo(), 1, true);
        }

        @Test
        @DisplayName("inserts a new node when id is new; keeps others; startId stays same (if non-zero)")
        void insertNewNode() {
            // given
            QuestNode n3 = node(3, "Bonus", List.of(), false);
            // when
            store.replaceNode(n3);
            // then
            List<QuestNode> list = store.nodes();
            assertEquals(3, list.size());
            assertEquals(List.of(1, 2, 3), list.stream().map(QuestNode::getId).toList());
            assertEquals(1, store.startId());
        }

        @Test
        @DisplayName("replaces existing node by id")
        void replaceExisting() {
            // given
            QuestNode n2new = node(2, "End (patched)", List.of(), true);
            // when
            store.replaceNode(n2new);
            // then
            assertEquals(2, store.nodes().size());
            assertEquals("End (patched)", store.get(2).getText());
        }

        @Test
        @DisplayName("when startId is 0, replacing sets startId to that node id")
        void setsStartIfZero() {
            // given
            InMemoryQuestStore s = InMemoryQuestStore.empty(0);
            // when
            s.replaceNode(node(10, "First", List.of(), false));
            // then
            assertEquals(10, s.startId());
            assertEquals(10, s.start().getId());
        }
    }

    @Nested
    @DisplayName("deleteNode(id)")
    class DeleteNodeBehavior {

        @Test
        @DisplayName("returns false when navigator is null (empty store)")
        void returnsFalseWhenEmpty() {
            // given
            InMemoryQuestStore store = InMemoryQuestStore.empty(0);
            // when / then
            assertFalse(store.deleteNode(1));
        }

        @Test
        @DisplayName("returns false when id does not exist")
        void idNotFound() {
            // given
            InMemoryQuestStore store = InMemoryQuestStore.empty(0);
            store.reload(simpleTwo(), 1, true);
            // when / then
            assertFalse(store.deleteNode(999));
        }

        @Test
        @DisplayName("deletes existing node, rebuilds navigator, adjusts startId if needed")
        void deletesExisting() {
            // given
            InMemoryQuestStore store = InMemoryQuestStore.empty(1);
            store.reload(simpleTwo(), 1, true);
            // when
            boolean ok = store.deleteNode(1);
            // then
            assertTrue(ok);
            assertEquals(2, store.startId(), "Fallback to smallest remaining id after deletion");
            assertNull(store.get(1));
            assertEquals(1, store.nodes().size());
            assertEquals(2, store.nodes().getFirst().getId());
            assertNotNull(store.start());
            assertEquals(2, store.start().getId(), "Navigator start points to the remaining node");
        }
    }

    @Nested
    @DisplayName("nodes()")
    class NodesView {

        @Test
        @DisplayName("returns sorted immutable list")
        void sortedAndUnmodifiable() {
            // given
            InMemoryQuestStore store = InMemoryQuestStore.empty(0);
            store.reload(List.of(
                    node(3, "C", List.of(), false),
                    node(1, "A", List.of(), false),
                    node(2, "B", List.of(), false)
            ), 1, true);
            // when
            List<QuestNode> list = store.nodes();
            // then
            assertEquals(List.of(1, 2, 3), list.stream().map(QuestNode::getId).toList());
            assertThrows(UnsupportedOperationException.class, () -> list.add(node(9, "X", List.of(), false)));
        }
    }

    @Nested
    @DisplayName("clearDraft(newStartId)")
    class ClearDraftBehavior {

        @Test
        @DisplayName("resets to draft:empty and null navigator; sets new startId")
        void clears() {
            // given
            InMemoryQuestStore store = InMemoryQuestStore.empty(0);
            store.reload(simpleTwo(), 1, true);
            // when
            store.clearDraft(77);
            // then
            assertEquals("draft:empty", store.version());
            assertEquals(77, store.startId());
            assertNull(store.start());
            assertTrue(store.nodes().isEmpty());
        }
    }

    @Nested
    @DisplayName("setStartId(newStartId)")
    class SetStartIdBehavior {

        @Test
        @DisplayName("when navigator is null, only the field changes (no rebuild)")
        void whenNavNull() {
            // given
            InMemoryQuestStore store = InMemoryQuestStore.empty(5);
            // when
            store.setStartId(9);
            // then
            assertEquals(9, store.startId());
            assertNull(store.start());
        }

        @Test
        @DisplayName("when navigator exists, rebuilds and updates start node")
        void whenNavExists() {
            // given
            InMemoryQuestStore store = InMemoryQuestStore.empty(0);
            store.reload(List.of(
                    node(10, "Ten", List.of(), false),
                    node(20, "Twenty", List.of(), false)
            ), 10, true);
            // when
            store.setStartId(20);
            // then
            assertEquals(20, store.startId());
            assertEquals(20, store.start().getId());
        }

        @Test
        @DisplayName("setStartId to an invalid id -> rebuild fallback applies (min id or 1)")
        void invalidStartHandledOnRebuild() {
            // given
            InMemoryQuestStore store = InMemoryQuestStore.empty(0);
            store.reload(List.of(
                    node(5, "A", List.of(), false),
                    node(7, "B", List.of(), false)
            ), 5, true);
            // when
            store.setStartId(999);
            // then
            assertEquals(5, store.startId(), "Fallback to min id=5");
            assertEquals(5, store.start().getId());
        }
    }

    @Nested
    @DisplayName("fromClasspath(resource, startId)")
    class FromClasspathBehavior {

        @Test
        @DisplayName("throws IOException when resource is missing")
        void missingResource() {
            // given
            String missing = "no/such/resource.json";
            // when / then
            assertThrows(IOException.class, () -> InMemoryQuestStore.fromClasspath(missing, 1));
        }

        @Test
        @DisplayName("parses real JSON resource; starts with sha256:* and turns to live:* after rebuild")
        void createsLiveStore() throws IOException {
            // given
            String resource = "quests/demo.json";
            // when
            InMemoryQuestStore store = InMemoryQuestStore.fromClasspath(resource, 1);
            // then
            assertNotNull(store.start(), "Navigator must be initialized");
            assertTrue(store.version().startsWith("sha256:"), "Initial version should be sha256:*");
            assertEquals(1, store.startId());
            QuestNode start = store.get(1);
            assertNotNull(start, "Start node must be present");
            assertFalse(start.getOptions().isEmpty(), "Start node must have at least one option");
            String answer = start.getOptions().getFirst().getChoice();
            Optional<QuestNode> next = store.choose(1, answer);
            assertTrue(next.isPresent(), "Choosing the first option should transition to the next node");
            assertEquals(2, next.get().getId(), "Expected to go to node #2");
            store.setStartId(1);
            assertTrue(store.version().startsWith("live:"), "Version should switch to live:* after rebuild");
        }
    }

    @Nested
    @DisplayName("version prefix semantics (draft vs live) — behavior checks")
    class VersionPrefixSemantics {

        @Test
        @DisplayName("editing store (empty + reload) keeps draft:* prefix")
        void draftPrefix() {
            // given
            InMemoryQuestStore store = InMemoryQuestStore.empty(0);
            // when
            store.reload(simpleTwo(), 1, true);
            // then
            assertTrue(store.version().startsWith("draft:"), "Editing mode should produce draft:* versions");
        }
    }

    @Nested
    @DisplayName("choose(fromId, answer) path evaluation")
    class ChoosePath {

        @Test
        @DisplayName("valid answer transitions to the configured next node")
        void validAnswer() {
            // given
            InMemoryQuestStore store = InMemoryQuestStore.empty(0);
            store.reload(simpleTwo(), 1, true);
            // when
            Optional<QuestNode> next = store.choose(1, "Next");
            // then
            assertTrue(next.isPresent());
            assertEquals(2, next.get().getId());
        }

        @Test
        @DisplayName("invalid answer returns empty")
        void invalidAnswer() {
            // given
            InMemoryQuestStore store = InMemoryQuestStore.empty(0);
            store.reload(simpleTwo(), 1, true);
            // when
            Optional<QuestNode> next = store.choose(1, "Nope");
            // then
            assertTrue(next.isEmpty());
        }
    }

    @Nested
    @DisplayName("get(id) lookups")
    class GetLookups {

        @Test
        @DisplayName("returns node by id when present")
        void present() {
            // given
            InMemoryQuestStore store = InMemoryQuestStore.empty(0);
            store.reload(simpleTwo(), 1, true);
            // when
            QuestNode q = store.get(2);
            // then
            assertNotNull(q);
            assertEquals(2, q.getId());
        }

        @Test
        @DisplayName("returns null when absent")
        void absent() {
            // given
            InMemoryQuestStore store = InMemoryQuestStore.empty(0);
            store.reload(simpleTwo(), 1, true);
            // when / then
            assertNull(store.get(999));
        }
    }

    @Nested
    @DisplayName("startId()")
    class StartIdAccessor {

        @Test
        @DisplayName("reflects current field value")
        void returnsValue() {
            // given
            InMemoryQuestStore store = InMemoryQuestStore.empty(10);
            // when / then
            assertEquals(10, store.startId());
            // and when
            store.setStartId(77);
            // then
            assertEquals(77, store.startId());
        }
    }
}