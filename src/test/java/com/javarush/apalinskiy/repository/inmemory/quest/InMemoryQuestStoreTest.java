package com.javarush.apalinskiy.repository.inmemory.quest;

import com.javarush.apalinskiy.domain.quest.Option;
import com.javarush.apalinskiy.domain.quest.QuestNode;
import com.javarush.apalinskiy.domain.quest.index.QuestNavigator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InMemoryQuestStore")
class InMemoryQuestStoreTest {

    @Mock
    QuestNavigator nav;

    private InMemoryQuestStore storeWithSnapshot(Integer startId, QuestNavigator nav, boolean editingMode) throws Exception {
        Class<?> snapCls = Arrays.stream(InMemoryQuestStore.class.getDeclaredClasses())
                .filter(c -> c.getSimpleName().equals("Snapshot"))
                .findFirst().orElse(null);
        assertNotNull(snapCls);
        Constructor<?> snapCtor = snapCls.getDeclaredConstructor(QuestNavigator.class, String.class);
        snapCtor.setAccessible(true);
        Object snap = snapCtor.newInstance(nav, "ver");
        Constructor<InMemoryQuestStore> ctor =
                InMemoryQuestStore.class.getDeclaredConstructor(int.class, snapCls, boolean.class);
        ctor.setAccessible(true);
        return ctor.newInstance(startId, snap, editingMode);
    }

    private QuestNode nf(int id) {
        return QuestNode.nonFin(id, "n" + id, List.of(new Option("go", id + 1)), null);
    }

    private QuestNode fin(int id) {
        return QuestNode.fin(id, "end" + id, null);
    }

    private QuestNode nfGoTo() {
        return QuestNode.nonFin(1, "n1", List.of(new Option("go", 2)), null);
    }

    @Nested
    @DisplayName("empty()")
    class EmptyFactory {

        @Test
        @DisplayName("version starts with draft:empty when created then exact")
        void versionDraftEmpty() {
            // Given / When
            InMemoryQuestStore s = InMemoryQuestStore.empty(123);
            // Then
            assertEquals("draft:empty", s.version());
            assertEquals(123, s.startId());
        }

        @Test
        @DisplayName("start/get/choose/nodes when empty then null/empty/empty")
        void emptyAccessors() {
            // Given
            InMemoryQuestStore s = InMemoryQuestStore.empty(1);
            // When / Then
            assertNull(s.start());
            assertNull(s.get(1));
            assertTrue(s.choose(1, "go").isEmpty());
            assertTrue(s.nodes().isEmpty());
        }
    }

    @Nested
    @DisplayName("fromClasspath()")
    class FromClasspath {

        @Test
        @DisplayName("throws when resource missing then IOException")
        void throwsWhenMissing() {
            // Given / When / Then
            assertThrows(java.io.IOException.class,
                    () -> InMemoryQuestStore.fromClasspath("no/such/resource.json", 1));
        }

        @Test
        @DisplayName("loads valid resource then builds live store and nav")
        void loadsResourceAndBuildsNav() throws Exception {
            // Given
            Path dir = Files.createTempDirectory("res");
            String json = """
                    [
                      {"id":1,"text":"start","options":[{"choice":"go","next":2}],"final":false},
                      {"id":2,"text":"end","options":[],"final":true}
                    ]
                    """;
            Files.writeString(dir.resolve("quest.json"), json, StandardCharsets.UTF_8);
            ClassLoader prev = Thread.currentThread().getContextClassLoader();
            try (URLClassLoader cl = new URLClassLoader(new URL[]{dir.toUri().toURL()}, null)) {
                Thread.currentThread().setContextClassLoader(cl);
                // When
                InMemoryQuestStore s = InMemoryQuestStore.fromClasspath("quest.json", 1);
                // Then
                String v = s.version();
                assertTrue(v.startsWith("sha256:") || v.startsWith("live:"), "version=" + v);
                assertNotNull(s.start());
                assertEquals(1, s.start().getId());
                assertTrue(s.choose(1, "go").isPresent());
                assertEquals(2, s.nodes().size());
            } finally {
                Thread.currentThread().setContextClassLoader(prev);
            }
        }
    }

    @Nested
    @DisplayName("deleteNode() loop branches")
    class DeleteNodeLoop {

        @Test
        @DisplayName("iterates ids, skips null and id==target; also skips q==null then ok (and resets start when deleting current start)")
        void skipsNullIdAndNullNode_resetsStart() throws Exception {
            // Given
            Set<Integer> ids = new LinkedHashSet<>(Arrays.asList(1, null, 2));
            when(nav.allIds()).thenReturn(ids);
            when(nav.get(2)).thenReturn(null);
            InMemoryQuestStore s = storeWithSnapshot(1, nav, true);
            // When
            boolean result = s.deleteNode(1);
            // Then
            assertTrue(result);
            assertNull(s.get(1));
            assertEquals(0, s.startId());
        }
    }

    @Nested
    @DisplayName("nodes() q==null branch")
    class NodesNullBranch {

        @Test
        @DisplayName("skips entries where get(id) returns null then not added")
        void skipsNulls() throws Exception {
            // Given
            when(nav.allIds()).thenReturn(Set.of(7));
            when(nav.get(7)).thenReturn(null);
            InMemoryQuestStore s = storeWithSnapshot(7, nav, true);
            // When
            List<QuestNode> list = s.nodes();
            // Then
            assertTrue(list.isEmpty());
        }
    }

    @Nested
    @DisplayName("replaceNode() with cur!=null and ex==null")
    class ReplaceNodeWithNullExisting {

        @Test
        @DisplayName("does not add null existing node then only new node kept")
        void doesNotAddNullExisting() throws Exception {
            // Given
            when(nav.allIds()).thenReturn(Set.of(5));
            when(nav.get(5)).thenReturn(null);
            InMemoryQuestStore s = storeWithSnapshot(0, nav, true);
            QuestNode fin5 = fin(5);
            // When
            s.replaceNode(fin5);
            // Then
            assertEquals(5, s.start().getId());
            assertEquals(List.of(5), s.nodes().stream().map(QuestNode::getId).toList());
        }
    }

    @Nested
    @DisplayName("rebuild() live mode")
    class LiveMode {

        @Test
        @DisplayName("rebuild picks strict nav and sets version 'live:' when editingMode=false then ok")
        void liveVersionPrefixAndNav() throws Exception {
            // Given
            InMemoryQuestStore s = storeWithSnapshot(0, null, false);
            QuestNode a1 = nfGoTo();
            QuestNode a2 = fin(2);
            // When
            s.reload(List.of(a1, a2), 1, false);
            // Then
            assertNotNull(s.start());
            assertEquals(1, s.start().getId());
            assertTrue(s.version().startsWith("live:"));
        }
    }

    @Nested
    @DisplayName("replaceNode()")
    class ReplaceNode {

        @Test
        @DisplayName("when cur!=null adds other existing nodes (ex!=null & ex.id!=node.id) then they persist")
        void addsOtherExistingNodes() {
            // Given
            InMemoryQuestStore s = InMemoryQuestStore.empty(1);
            QuestNode n1 = QuestNode.nonFin(1, "old-1", List.of(new Option("go", 2)), null);
            QuestNode n2 = QuestNode.nonFin(2, "keep-2", List.of(new Option("go", 3)), null);
            s.reload(List.of(n1, n2), 1, true);
            // When
            QuestNode replacement = QuestNode.nonFin(1, "new-1", List.of(new Option("go", 2)), null);
            s.replaceNode(replacement);
            // Then
            List<Integer> ids = s.nodes().stream().map(QuestNode::getId).sorted().toList();
            assertEquals(List.of(1, 2), ids);
            assertEquals("new-1", s.get(1).getText());
            assertEquals("keep-2", s.get(2).getText());
        }

        @Test
        @DisplayName("sets startId to node.id when startId==0 then startId becomes node.id")
        void setsStartIdWhenZero() {
            // Given
            InMemoryQuestStore s = InMemoryQuestStore.empty(0);
            QuestNode n10 = nf(10);
            // When
            s.replaceNode(n10);
            // Then
            assertEquals(10, s.startId());
            assertEquals(10, s.start().getId());
        }

        @Test
        @DisplayName("keeps startId when non-zero then startId unchanged and nav starts at min id (editing)")
        void keepsStartIdWhenNonZero() {
            // Given
            InMemoryQuestStore s = InMemoryQuestStore.empty(100);
            QuestNode n10 = nf(10);
            // When
            s.replaceNode(n10);
            // Then
            assertEquals(100, s.startId());
            assertEquals(10, s.start().getId());
        }

        @Test
        @DisplayName("bumps version (draft:timestamp) when replaced then version changes")
        void bumpsVersion() {
            // Given
            InMemoryQuestStore s = InMemoryQuestStore.empty(0);
            String v0 = s.version();
            // When
            s.replaceNode(nf(1));
            String v1 = s.version();
            // Then
            assertNotEquals(v0, v1);
            assertTrue(v1.startsWith("draft:"));
        }
    }

    @Nested
    @DisplayName("deleteNode()")
    class DeleteNode {

        @Test
        @DisplayName("returns false when nav is null then no-op")
        void returnsFalseWhenNoNav() {
            // Given
            InMemoryQuestStore s = InMemoryQuestStore.empty(1);
            // When / Then
            assertFalse(s.deleteNode(1));
        }

        @Test
        @DisplayName("returns false when id not present then no-op")
        void returnsFalseWhenMissingId() {
            // Given
            InMemoryQuestStore s = InMemoryQuestStore.empty(1);
            s.replaceNode(nf(2));
            // When / Then
            assertFalse(s.deleteNode(99));
        }

        @Test
        @DisplayName("deletes node when present then nodes shrink and startId unchanged if not start")
        void deletesNonStart() {
            // Given
            InMemoryQuestStore s = InMemoryQuestStore.empty(1);
            s.reload(List.of(nf(1), nf(2), fin(3)), 1, true);
            int before = s.nodes().size();
            // When
            boolean ok = s.deleteNode(2);
            // Then
            assertTrue(ok);
            assertEquals(before - 1, s.nodes().size());
            assertEquals(1, s.startId());
            assertNull(s.get(2));
        }

        @Test
        @DisplayName("deleting start node sets startId=0 then start becomes null or min (after rebuild later)")
        void deletingStartSetsZero() {
            // Given
            InMemoryQuestStore s = InMemoryQuestStore.empty(1);
            s.reload(List.of(nf(1), nf(2)), 1, true);
            // When
            boolean ok = s.deleteNode(1);
            // Then
            assertTrue(ok);
            assertEquals(0, s.startId());
        }
    }

    @Nested
    @DisplayName("nodes()")
    class Nodes {

        @Test
        @DisplayName("returns sorted immutable list when nodes present then sorted and unmodifiable")
        void sortedAndUnmodifiable() {
            // Given
            InMemoryQuestStore s = InMemoryQuestStore.empty(1);
            s.reload(List.of(nf(5), nf(3), nf(4)), 3, true);
            // When
            List<QuestNode> list = s.nodes();
            // Then
            assertEquals(List.of(3, 4, 5), list.stream().map(QuestNode::getId).toList());
            assertThrows(UnsupportedOperationException.class, () -> list.add(nf(99)));
        }
    }

    @Nested
    @DisplayName("setStartId()")
    class SetStartId {

        @Test
        @DisplayName("updates startId and rebuilds when nav exists then start matches")
        void updatesAndRebuilds() {
            // Given
            InMemoryQuestStore s = InMemoryQuestStore.empty(1);
            s.reload(List.of(nf(1), nf(2)), 1, true);
            // When
            s.setStartId(2);
            // Then
            assertEquals(2, s.startId());
            assertEquals(2, s.start().getId());
        }

        @Test
        @DisplayName("updates startId when nav is null then only field changed")
        void updatesWhenNoNav() {
            // Given
            InMemoryQuestStore s = InMemoryQuestStore.empty(5); // nav==null
            // When
            s.setStartId(7);
            // Then
            assertEquals(7, s.startId());
            assertNull(s.start());
        }
    }

    @Nested
    @DisplayName("clearDraft()")
    class ClearDraft {

        @Test
        @DisplayName("clears nav and sets version draft:empty when called then start becomes null")
        void clearsDraft() {
            // Given
            InMemoryQuestStore s = InMemoryQuestStore.empty(0);
            s.replaceNode(nf(10));
            assertNotNull(s.start());
            // When
            s.clearDraft(77);
            // Then
            assertEquals("draft:empty", s.version());
            assertEquals(77, s.startId());
            assertNull(s.start());
            assertTrue(s.nodes().isEmpty());
        }
    }

    @Nested
    @DisplayName("reload()")
    class ReloadMethod {

        @Test
        @DisplayName("rebuilds with provided nodes/start when called then start and nodes match")
        void rebuildsWithNodes() {
            // Given
            InMemoryQuestStore s = InMemoryQuestStore.empty(0);
            // When
            s.reload(List.of(nf(2), nf(5)), 5, true);
            // Then
            assertEquals(5, s.startId());
            assertEquals(5, s.start().getId());
            assertEquals(List.of(2, 5), s.nodes().stream().map(QuestNode::getId).toList());
        }

        @Test
        @DisplayName("with empty list builds nav=null then start() null and nodes empty")
        void emptyListMakesNullNav() {
            // Given
            InMemoryQuestStore s = InMemoryQuestStore.empty(1);
            // When
            s.reload(List.of(), 1, true);
            // Then
            assertNull(s.start());
            assertTrue(s.nodes().isEmpty());
        }

        @Test
        @DisplayName("invalid startId in editing mode picks min id for nav then start() = min")
        void invalidStartPicksMinInEditing() {
            // Given
            InMemoryQuestStore s = InMemoryQuestStore.empty(999);
            // When
            s.reload(List.of(nf(7), nf(3), nf(9)), 999, true);
            // Then
            assertEquals(999, s.startId());
            assertEquals(3, s.start().getId());
        }

        @Test
        @DisplayName("reload with nodes=null then nav=null and start()/nodes empty")
        void reloadWithNullNodes() throws Exception {
            // Given
            InMemoryQuestStore s = InMemoryQuestStore.empty(0);
            s.replaceNode(QuestNode.nonFin(10, "n10", List.of(new Option("go", 11)), null));
            String v0 = s.version();
            Thread.sleep(15);
            // When
            s.reload(null, 7, true);
            String v1 = s.version();
            // Then
            assertNull(s.start());
            assertTrue(s.nodes().isEmpty());
            assertEquals(7, s.startId());
            assertNotEquals(v0, v1);
            assertTrue(v1.startsWith("draft:"));
        }
    }

    @Nested
    @DisplayName("get()/start()/choose()")
    class Accessors {

        @Test
        @DisplayName("get returns node by id when present then same")
        void getReturns() {
            // Given
            InMemoryQuestStore s = InMemoryQuestStore.empty(1);
            s.reload(List.of(nf(1), fin(2)), 1, true);
            // When
            QuestNode q = s.get(1);
            // Then
            assertNotNull(q);
            assertEquals(1, q.getId());
        }

        @Test
        @DisplayName("choose resolves mapping when answer matches then present")
        void chooseResolves() {
            // Given
            InMemoryQuestStore s = InMemoryQuestStore.empty(1);
            s.reload(List.of(nf(1), fin(2)), 1, true);
            // When
            Optional<QuestNode> next = s.choose(1, "go");
            // Then
            assertTrue(next.isPresent());
            assertEquals(2, next.get().getId());
        }

        @Test
        @DisplayName("choose returns empty when no mapping then empty")
        void chooseEmptyWhenNoMapping() {
            // Given
            InMemoryQuestStore s = InMemoryQuestStore.empty(1);
            s.reload(List.of(nf(1), fin(2)), 1, true);
            // When
            Optional<QuestNode> next = s.choose(1, "nope");
            // Then
            assertTrue(next.isEmpty());
        }
    }

    @Nested
    @DisplayName("InMemoryQuestStore.sha256")
    class InMemoryQuestStoreSha256Test {

        @Test
        @DisplayName("returns 'unknown' when digest throws (null data) then catch branch hit")
        void returnsUnknownOnException() throws Exception {
            // Given
            Method m = InMemoryQuestStore.class.getDeclaredMethod("sha256", byte[].class);
            m.setAccessible(true);
            // When
            String res = (String) m.invoke(null, new Object[]{null});
            // Then
            assertEquals("unknown", res);
        }
    }
}