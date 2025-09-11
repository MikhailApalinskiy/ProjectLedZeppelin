package com.javarush.apalinskiy.service.quest;

import com.javarush.apalinskiy.domain.quest.Option;
import com.javarush.apalinskiy.domain.quest.QuestNode;
import com.javarush.apalinskiy.domain.quest.custom.CustomQuest;
import com.javarush.apalinskiy.repository.quest.CustomQuestRepository;
import com.javarush.apalinskiy.repository.quest.QuestDraftStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuestAuthoringService")
class QuestAuthoringServiceTest {

    @Mock
    QuestDraftStore editorRepo;
    @Mock
    CustomQuestRepository catalogRepo;

    @InjectMocks
    QuestAuthoringService service;

    @Nested
    @DisplayName("Basic delegations")
    class BasicDelegations {

        @Test
        @DisplayName("get(id) delegates to editorRepo.get")
        void getDelegates() {
            // Given
            QuestNode stub = mock(QuestNode.class);
            when(editorRepo.get(42)).thenReturn(stub);
            // When
            QuestNode result = service.get(42);
            // Then
            assertSame(stub, result);
            verify(editorRepo).get(42);
            verifyNoMoreInteractions(editorRepo);
        }

        @Test
        @DisplayName("saveNode/setStart/deleteNode/nodes/clear delegate correctly")
        void otherDelegations() {
            // Given
            QuestNode any = mock(QuestNode.class);
            when(editorRepo.deleteNode(7)).thenReturn(true);
            // When
            service.saveNode(any);
            service.setStart(5);
            boolean deleted = service.deleteNode(7);
            service.nodes();
            service.clearEditorDraft();
            // Then
            assertTrue(deleted);
            verify(editorRepo).replaceNode(any);
            verify(editorRepo).setStartId(5);
            verify(editorRepo).deleteNode(7);
            verify(editorRepo).nodes();
            verify(editorRepo).clearDraft(0);
            verifyNoMoreInteractions(editorRepo);
        }
    }

    @Nested
    @DisplayName("loadToEditor")
    class LoadToEditor {

        @Test
        @DisplayName("loads quest from catalog and reloads editor")
        void loadOk() {
            // Given
            QuestNode n1 = mock(QuestNode.class);
            List<QuestNode> nodes = List.of(n1);
            CustomQuest q = mock(CustomQuest.class);
            when(q.getNodes()).thenReturn(nodes);
            when(q.getStartId()).thenReturn(1);
            when(catalogRepo.get("Q1")).thenReturn(Optional.of(q));
            // When
            service.loadToEditor("Q1");
            // Then
            verify(editorRepo).reload(nodes, 1, false);
        }

        @Test
        @DisplayName("throws IllegalArgumentException if quest not found")
        void loadNotFound() {
            // Given
            when(catalogRepo.get("missing")).thenReturn(Optional.empty());
            // When / Then
            IllegalArgumentException ex =
                    assertThrows(IllegalArgumentException.class, () -> service.loadToEditor("missing"));
            assertTrue(ex.getMessage().contains("Quest not found"));
        }
    }

    @Nested
    @DisplayName("publish / publishNew")
    class PublishNewTests {

        @Test
        @DisplayName("creates published quest with default name and clears editor")
        void publishNew_DefaultNameAndReloads() {
            // Given
            QuestNode node1 = mock(QuestNode.class);
            QuestNode node2 = mock(QuestNode.class);
            Option opt12 = mock(Option.class);
            when(node1.getId()).thenReturn(1);
            when(node1.isFin()).thenReturn(false);
            when(node1.getOptions()).thenReturn(List.of(opt12));
            when(node1.getText()).thenReturn("n1");
            when(node1.getImage()).thenReturn(null);
            when(node2.getId()).thenReturn(2);
            when(node2.isFin()).thenReturn(true);
            when(node2.getOptions()).thenReturn(List.of());
            when(node2.getText()).thenReturn("n2");
            when(node2.getImage()).thenReturn(null);
            when(opt12.choice()).thenReturn("go");
            when(opt12.next()).thenReturn(2);
            List<QuestNode> nodes = List.of(node1, node2);
            when(editorRepo.nodes()).thenReturn(nodes);
            when(editorRepo.startId()).thenReturn(1);
            // When
            service.publishNew("owner1", "  ");
            // Then
            verify(catalogRepo).create(eq("owner1"), eq("Untitled Quest"),
                    eq(1), eq(nodes), eq(true), anyString());
            verify(editorRepo).reload(eq(Collections.emptyList()), eq(0), eq(false));
        }

        @Test
        @DisplayName("publish delegates to publishNew")
        void publishDelegates() {
            // Given
            QuestNode node1 = mock(QuestNode.class);
            QuestNode node2 = mock(QuestNode.class);
            Option opt12 = mock(Option.class);
            when(node1.getId()).thenReturn(1);
            when(node1.isFin()).thenReturn(false);
            when(node1.getOptions()).thenReturn(List.of(opt12));
            when(node1.getText()).thenReturn("n1");
            when(node1.getImage()).thenReturn(null);
            when(node2.getId()).thenReturn(2);
            when(node2.isFin()).thenReturn(true);
            when(node2.getOptions()).thenReturn(List.of());
            when(node2.getText()).thenReturn("n2");
            when(node2.getImage()).thenReturn(null);
            when(opt12.choice()).thenReturn("go");
            when(opt12.next()).thenReturn(2);
            when(editorRepo.nodes()).thenReturn(List.of(node1, node2));
            when(editorRepo.startId()).thenReturn(1);
            // When
            service.publish("alice", "Quest X");
            // Then
            verify(catalogRepo).create(eq("alice"), eq("Quest X"),
                    eq(1), anyList(), eq(true), anyString());
        }

        @Test
        @DisplayName("throws IllegalStateException for invalid draft (no final node)")
        void publishInvalidDraft() {
            // Given
            QuestNode node1 = mock(QuestNode.class);
            QuestNode node2 = mock(QuestNode.class);
            Option opt12 = mock(Option.class);
            when(node1.getId()).thenReturn(1);
            when(node1.isFin()).thenReturn(false);
            when(node1.getOptions()).thenReturn(List.of(opt12));
            when(opt12.next()).thenReturn(2);
            when(node2.getId()).thenReturn(2);
            when(node2.isFin()).thenReturn(false);
            when(node2.getOptions()).thenReturn(List.of());
            when(editorRepo.nodes()).thenReturn(List.of(node1, node2));
            when(editorRepo.startId()).thenReturn(1);
            // When / Then
            IllegalStateException ex =
                    assertThrows(IllegalStateException.class, () -> service.publishNew("o", "n"));
            assertTrue(ex.getMessage().contains("At least one final node is required"));
            verify(catalogRepo, never()).create(any(), any(), anyInt(), anyList(), anyBoolean(), anyString());
        }

        @Test
        @DisplayName("throws NullPointerException when ownerLogin is null (fails early, no draft needed)")
        void publishNullOwner() {
            // Given
            // When / Then
            assertThrows(NullPointerException.class, () -> service.publishNew(null, "n"));
            verifyNoInteractions(editorRepo);
            verifyNoInteractions(catalogRepo);
        }
    }

    @Nested
    @DisplayName("submitNewForModeration")
    class SubmitForModeration {

        @Test
        @DisplayName("stages new quest and clears editor")
        void submitStagesAndReloads() {
            // Given
            QuestNode node1 = mock(QuestNode.class);
            QuestNode node2 = mock(QuestNode.class);
            Option opt12 = mock(Option.class);
            when(node1.getId()).thenReturn(1);
            when(node1.isFin()).thenReturn(false);
            when(node1.getOptions()).thenReturn(List.of(opt12));
            when(node1.getText()).thenReturn("n1");
            when(node1.getImage()).thenReturn(null);
            when(node2.getId()).thenReturn(2);
            when(node2.isFin()).thenReturn(true);
            when(node2.getOptions()).thenReturn(List.of());
            when(node2.getText()).thenReturn("n2");
            when(node2.getImage()).thenReturn(null);
            when(opt12.choice()).thenReturn("go");
            when(opt12.next()).thenReturn(2);
            List<QuestNode> nodes = List.of(node1, node2);
            when(editorRepo.nodes()).thenReturn(nodes);
            when(editorRepo.startId()).thenReturn(1);
            // When
            service.submitNewForModeration("ownerX", "  ");
            // Then
            verify(catalogRepo).stageCreate(eq("ownerX"), eq("Untitled Quest"),
                    eq(1), eq(nodes), anyString());
            verify(editorRepo).reload(eq(Collections.emptyList()), eq(0), eq(false));
        }
    }

    @Nested
    @DisplayName("updateExisting")
    class UpdateExisting {

        @Test
        @DisplayName("as admin: updates quest directly (needs valid draft after repo.get)")
        void adminUpdate() {
            // Given
            when(catalogRepo.get("Q")).thenReturn(Optional.of(mock(CustomQuest.class)));
            QuestNode node1 = mock(QuestNode.class);
            when(node1.getId()).thenReturn(1);
            when(node1.isFin()).thenReturn(true);
            when(node1.getOptions()).thenReturn(List.of());
            when(node1.getText()).thenReturn("n1");
            when(node1.getImage()).thenReturn(null);
            when(editorRepo.nodes()).thenReturn(List.of(node1));
            when(editorRepo.startId()).thenReturn(1);
            // When
            service.updateExisting("Q", true);
            // Then
            verify(catalogRepo).update(eq("Q"), eq(1), anyList(), eq(true), anyString());
        }

        @Test
        @DisplayName("as author: stages edit (needs valid draft after repo.get)")
        void authorStageEdit() {
            // Given
            when(catalogRepo.get("Q")).thenReturn(Optional.of(mock(CustomQuest.class)));
            QuestNode node1 = mock(QuestNode.class);
            when(node1.getId()).thenReturn(1);
            when(node1.isFin()).thenReturn(true);
            when(node1.getOptions()).thenReturn(List.of());
            when(node1.getText()).thenReturn("n1");
            when(node1.getImage()).thenReturn(null);
            when(editorRepo.nodes()).thenReturn(List.of(node1));
            when(editorRepo.startId()).thenReturn(1);
            // When
            service.updateExisting("Q", false);
            // Then
            verify(catalogRepo).stageEdit(eq("Q"), eq(1), anyList(), anyString());
        }

        @Test
        @DisplayName("throws IllegalArgumentException if quest not found (fails early, no draft needed)")
        void updateNotFound() {
            // Given
            when(catalogRepo.get("missing")).thenReturn(Optional.empty());
            // When / Then
            IllegalArgumentException ex =
                    assertThrows(IllegalArgumentException.class, () -> service.updateExisting("missing", true));
            assertTrue(ex.getMessage().contains("Quest not found"));
            verifyNoInteractions(editorRepo);
        }
    }

    @Nested
    @DisplayName("Catalog deletions")
    class Deletions {

        @Test
        @DisplayName("deleteIfOwner returns repository result")
        void deleteIfOwner() {
            // Given
            when(catalogRepo.deleteIfOwner("Q", "alice")).thenReturn(true);
            // When
            boolean ok = service.deleteFromCatalogIfOwner("Q", "alice");
            // Then
            assertTrue(ok);
            verify(catalogRepo).deleteIfOwner("Q", "alice");
        }

        @Test
        @DisplayName("deleteAsAdmin calls delete(questId)")
        void deleteAsAdmin() {
            // When
            service.deleteFromCatalogAsAdmin("Q2");
            // Then
            verify(catalogRepo).delete("Q2");
        }
    }

    @Nested
    @DisplayName("Catalog reads")
    class CatalogRead {

        @Test
        @DisplayName("getFromCatalog/listAllFromCatalog/listOwnerFromCatalog delegate to repository")
        void reads() {
            // When
            service.getFromCatalog("Q");
            service.listAllFromCatalog();
            service.listOwnerFromCatalog("bob");
            // Then
            verify(catalogRepo).get("Q");
            verify(catalogRepo).listAll();
            verify(catalogRepo).listByOwner("bob");
        }
    }

    @Nested
    @DisplayName("validateCurrentDraft")
    class ValidateDraft {

        @Test
        @DisplayName("empty nodes -> 'Draft is empty.'")
        void emptyDraft() {
            // Given
            when(editorRepo.nodes()).thenReturn(List.of());
            // When
            List<String> errors = service.validateCurrentDraft();
            // Then
            assertEquals(1, errors.size());
            assertEquals("Draft is empty.", errors.getFirst());
        }

        @Test
        @DisplayName("missing start -> 'Start node is not set.'")
        void missingStart() {
            // Given
            QuestNode node1 = mock(QuestNode.class);
            when(node1.getId()).thenReturn(1);
            when(node1.isFin()).thenReturn(true);
            when(editorRepo.nodes()).thenReturn(List.of(node1));
            when(editorRepo.startId()).thenReturn(999);
            // When
            List<String> errors = service.validateCurrentDraft();
            // Then
            assertTrue(errors.contains("Start node is not set."));
        }

        @Test
        @DisplayName("non-final node without valid options -> error about at least one option")
        void noOptionsOnNonFinal() {
            // Given
            QuestNode node1 = mock(QuestNode.class);
            QuestNode node2 = mock(QuestNode.class);
            when(node1.getId()).thenReturn(1);
            when(node1.isFin()).thenReturn(false);
            when(node1.getOptions()).thenReturn(List.of());
            when(node2.getId()).thenReturn(2);
            when(node2.isFin()).thenReturn(true);
            when(editorRepo.nodes()).thenReturn(List.of(node1, node2));
            when(editorRepo.startId()).thenReturn(1);
            // When
            List<String> errors = service.validateCurrentDraft();
            // Then
            assertTrue(errors.stream().anyMatch(s -> s.contains("Node #1 must have at least one option.")));
        }

        @Test
        @DisplayName("broken link -> error 'has a broken link'")
        void brokenLink() {
            // Given
            QuestNode node1 = mock(QuestNode.class);
            QuestNode node2 = mock(QuestNode.class);
            Option broken = mock(Option.class);
            when(node1.getId()).thenReturn(1);
            when(node1.isFin()).thenReturn(false);
            when(broken.next()).thenReturn(777);
            when(node1.getOptions()).thenReturn(List.of(broken));
            when(node2.getId()).thenReturn(2);
            when(node2.isFin()).thenReturn(true);
            when(editorRepo.nodes()).thenReturn(List.of(node1, node2));
            when(editorRepo.startId()).thenReturn(1);
            // When
            List<String> errors = service.validateCurrentDraft();
            // Then
            assertTrue(errors.stream().anyMatch(s -> s.contains("Node #1 has a broken link to #777.")));
        }
    }

    @Nested
    @DisplayName("Moderation delegations")
    class ModerationDelegations {

        @Test
        @DisplayName("listPendingNew delegates to catalogRepo")
        void listPendingNewDelegates() {
            // Given
            // When
            service.listPendingNew();
            // Then
            verify(catalogRepo).listPendingNew();
        }

        @Test
        @DisplayName("listPendingEdits delegates to catalogRepo")
        void listPendingEditsDelegates() {
            // Given
            // When
            service.listPendingEdits();
            // Then
            verify(catalogRepo).listPendingEdits();
        }

        @Test
        @DisplayName("approveCreate delegates to catalogRepo")
        void approveCreateDelegates() {
            // Given
            when(catalogRepo.approveCreate("pid")).thenReturn("qid");
            // When
            String result = service.approveCreate("pid");
            // Then
            assertEquals("qid", result);
            verify(catalogRepo).approveCreate("pid");
        }

        @Test
        @DisplayName("rejectCreate delegates to catalogRepo")
        void rejectCreateDelegates() {
            // Given
            // When
            service.rejectCreate("pid");
            // Then
            verify(catalogRepo).rejectCreate("pid");
        }

        @Test
        @DisplayName("approveEdit delegates to catalogRepo")
        void approveEditDelegates() {
            // Given
            // When
            service.approveEdit("qid");
            // Then
            verify(catalogRepo).approveEdit("qid");
        }

        @Test
        @DisplayName("rejectEdit delegates to catalogRepo")
        void rejectEditDelegates() {
            // Given
            // When
            service.rejectEdit("qid");
            // Then
            verify(catalogRepo).rejectEdit("qid");
        }
    }

    @Nested
    @DisplayName("Versioning (computeVersion via public API)")
    class Versioning {

        @Test
        @DisplayName("same nodes different order -> same version")
        void stableAcrossOrder() {
            // Given
            QuestNode n1 = mock(QuestNode.class);
            QuestNode n2 = mock(QuestNode.class);
            Option o12 = mock(Option.class);
            when(n1.getId()).thenReturn(1);
            when(n1.isFin()).thenReturn(false);
            when(n1.getOptions()).thenReturn(List.of(o12));
            when(n1.getText()).thenReturn("n1");
            when(n1.getImage()).thenReturn(null);
            when(n2.getId()).thenReturn(2);
            when(n2.isFin()).thenReturn(true);
            when(n2.getOptions()).thenReturn(List.of());
            when(n2.getText()).thenReturn("n2");
            when(n2.getImage()).thenReturn(null);
            when(o12.choice()).thenReturn("go");
            when(o12.next()).thenReturn(2);
            List<QuestNode> reversed = List.of(n2, n1);
            List<QuestNode> sorted = List.of(n1, n2);
            doReturn(reversed).doReturn(sorted)
                    .when(editorRepo).nodes();
            when(editorRepo.startId()).thenReturn(1);
            when(editorRepo.startId()).thenReturn(1);
            ArgumentCaptor<String> versionCap = ArgumentCaptor.forClass(String.class);
            // When
            service.publishNew("owner", "A");
            service.publishNew("owner", "B");
            // Then
            verify(catalogRepo, times(2)).create(eq("owner"), anyString(), eq(1), anyList(), eq(true), versionCap.capture());
            var versions = versionCap.getAllValues();
            assertEquals(2, versions.size());
            assertEquals(versions.get(0), versions.get(1));
        }

        @Test
        @DisplayName("change image content -> version changes")
        void imageAffectsVersion() {
            // Given
            QuestNode n1 = mock(QuestNode.class);
            QuestNode n2 = mock(QuestNode.class);
            Option o12 = mock(Option.class);
            when(n1.getId()).thenReturn(1);
            when(n1.isFin()).thenReturn(false);
            when(n1.getOptions()).thenReturn(List.of(o12));
            when(n1.getText()).thenReturn("n1");
            when(n1.getImage()).thenReturn(null, "img.png");
            when(n2.getId()).thenReturn(2);
            when(n2.isFin()).thenReturn(true);
            when(n2.getOptions()).thenReturn(List.of());
            when(n2.getText()).thenReturn("n2");
            when(n2.getImage()).thenReturn(null);
            when(o12.choice()).thenReturn("go");
            when(o12.next()).thenReturn(2);
            List<QuestNode> nodes = List.of(n1, n2);
            doReturn(nodes).doReturn(nodes).when(editorRepo).nodes();
            when(editorRepo.startId()).thenReturn(1);
            ArgumentCaptor<String> versionCap = ArgumentCaptor.forClass(String.class);
            // When
            service.publishNew("owner", "First");
            service.publishNew("owner", "Second");
            // Then
            verify(catalogRepo, times(2)).create(eq("owner"), anyString(), eq(1), anyList(), eq(true), versionCap.capture());
            var versions = versionCap.getAllValues();
            assertEquals(2, versions.size());
            assertNotEquals(versions.get(0), versions.get(1));
        }

        @Test
        @DisplayName("internal error while hashing -> returns 'sha256:unknown'")
        void errorPathReturnsUnknown() {
            // Given
            QuestNode n1 = mock(QuestNode.class);
            QuestNode n2 = mock(QuestNode.class);
            Option o12 = mock(Option.class);
            when(n1.getId()).thenReturn(1);
            when(n1.isFin()).thenReturn(false);
            when(n1.getOptions()).thenReturn(List.of(o12));
            when(o12.next()).thenReturn(2);
            when(n2.getId()).thenReturn(2);
            when(n2.isFin()).thenReturn(true);
            when(n1.getImage()).thenReturn(null);
            when(n1.getText()).thenThrow(new RuntimeException("boom"));
            when(editorRepo.nodes()).thenReturn(List.of(n1, n2));
            when(editorRepo.startId()).thenReturn(1);
            ArgumentCaptor<String> versionCap = ArgumentCaptor.forClass(String.class);
            // When
            service.publishNew("owner", "Err");
            // Then
            verify(catalogRepo).create(eq("owner"), eq("Err"),
                    eq(1), anyList(), eq(true), versionCap.capture());
            assertEquals("sha256:unknown", versionCap.getValue());
        }
    }
}