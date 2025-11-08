package com.javarush.apalinskiy.service.quest;

import com.javarush.apalinskiy.domain.quest.Option;
import com.javarush.apalinskiy.domain.quest.QuestNode;
import com.javarush.apalinskiy.domain.quest.custom.CustomQuest;
import com.javarush.apalinskiy.domain.quest.custom.DraftRow;
import com.javarush.apalinskiy.repository.hibernate.quest.HDraftRepository;
import com.javarush.apalinskiy.repository.quest.CustomQuestRepository;
import com.javarush.apalinskiy.repository.quest.QuestDraftStore;
import com.javarush.apalinskiy.repository.quest.QuestStore;
import com.javarush.apalinskiy.utils.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("QuestAuthoringService (unit)")
@ExtendWith(MockitoExtension.class)
class QuestAuthoringServiceTest {

    @Mock
    QuestDraftStore editorRepo;
    @Mock
    QuestStore prodRepo;
    @Mock
    CustomQuestRepository catalogRepo;
    @Mock
    HDraftRepository draftRepo;
    @Mock
    CurrentUserProvider currentUser;

    QuestAuthoringService sut;
    QuestAuthoringService sutWithDrafts;

    private QuestNode node(int id, String text, boolean fin, Option... opts) {
        List<Option> list = new ArrayList<>();
        if (opts != null) Collections.addAll(list, opts);
        return QuestNode.of(id, text, list, fin, null);
    }

    private Option opt(Integer next) {
        return new Option("Go", next);
    }

    private List<QuestNode> simpleGraph() {
        return List.of(
                node(1, "Start", false, opt(2)),
                node(2, "End", true)
        );
    }

    @BeforeEach
    void init() {
        sut = new QuestAuthoringService(editorRepo, prodRepo, catalogRepo);
        sutWithDrafts = new QuestAuthoringService(editorRepo, prodRepo, catalogRepo, draftRepo, currentUser);
    }

    @Nested
    @DisplayName("loadToEditor(questId)")
    class LoadToEditor {

        @Test
        @DisplayName("Given quest exists and no draft — When loadToEditor — Then editor reloads LIVE graph and autosaves")
        void loadsLiveWhenNoDraft() {
            // Given
            String qid = "q-1";
            CustomQuest live = mock(CustomQuest.class);
            when(catalogRepo.get(qid)).thenReturn(Optional.of(live));
            when(live.getNodes()).thenReturn(simpleGraph());
            when(live.getStartId()).thenReturn(1);
            when(editorRepo.startId()).thenReturn(1);
            when(editorRepo.nodes()).thenReturn(simpleGraph());
            when(currentUser.currentUserId()).thenReturn("u-1");
            when(draftRepo.findLatest("u-1", qid)).thenReturn(Optional.empty());
            // When
            sutWithDrafts.loadToEditor(qid);
            // Then
            verify(catalogRepo).get(qid);
            verify(editorRepo).reload(live.getNodes(), 1, false);
            verify(draftRepo).findLatest("u-1", qid);
            verify(draftRepo).upsertDraft(eq("u-1"), eq(qid), anyString(), eq(1), anyList(), anyString());
        }

        @Test
        @DisplayName("Given draft exists — When loadToEditor — Then restores draft instead of LIVE and autosaves")
        void restoresDraftIfPresent() {
            // Given
            String qid = "q-1";
            when(currentUser.currentUserId()).thenReturn("u-1");
            var row = mock(com.javarush.apalinskiy.domain.quest.custom.DraftRow.class);
            when(draftRepo.findLatest("u-1", qid)).thenReturn(Optional.of(row));
            when(row.getNodesJson()).thenReturn("[]");
            when(row.getStartId()).thenReturn(1);
            when(row.getName()).thenReturn("DraftName");
            when(row.getVersionNote()).thenReturn("note");
            doNothing().when(editorRepo).reload(anyList(), eq(1), eq(true));
            when(editorRepo.startId()).thenReturn(1);
            when(editorRepo.nodes()).thenReturn(List.of());
            // When
            sutWithDrafts.loadToEditor(qid);
            // Then
            verify(draftRepo).findLatest("u-1", qid);
            verify(editorRepo).reload(anyList(), eq(1), eq(true));
            verify(catalogRepo, never()).get(anyString());
            verify(draftRepo).upsertDraft(eq("u-1"), eq(qid), anyString(), eq(1), anyList(), anyString());
        }

        @Test
        @DisplayName("Given quest missing and no draft — When loadToEditor — Then throws IllegalArgumentException")
        void throwsIfQuestMissing() {
            // Given
            String qid = "missing";
            when(currentUser.currentUserId()).thenReturn("u-1");
            when(draftRepo.findLatest("u-1", qid)).thenReturn(Optional.empty());
            when(catalogRepo.get(qid)).thenReturn(Optional.empty());
            // When / Then
            assertThrows(IllegalArgumentException.class, () -> sutWithDrafts.loadToEditor(qid));
            verify(editorRepo, never()).reload(anyList(), anyInt(), anyBoolean());
        }
    }

    @Nested
    @DisplayName("publishNew(ownerId, name) / submitNewForModeration(ownerId, name)")
    class PublishAndSubmit {

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("Given valid draft — When publishNew — Then catalog.create called with LIVE, editor cleared, temp draft removed")
        void publishNewHappyPath() {
            // Given
            var graph = simpleGraph();
            when(editorRepo.nodes()).thenReturn(graph);
            when(editorRepo.startId()).thenReturn(1);
            when(currentUser.currentUserId()).thenReturn("u-1");
            DraftRow row = mock(DraftRow.class);
            when(row.getDraftId()).thenReturn("draft-1");
            when(draftRepo.findLatest("u-1", null)).thenReturn(Optional.of(row));
            // When
            sutWithDrafts.publishNew("owner-1", "My Quest");
            // Then
            ArgumentCaptor<List<QuestNode>> capNodes = ArgumentCaptor.forClass(List.class);
            verify(catalogRepo).create(eq("owner-1"), eq("My Quest"), eq(1), capNodes.capture(), eq(true), anyString());
            assertEquals(2, capNodes.getValue().size());
            verify(editorRepo).reload(eq(Collections.emptyList()), eq(0), eq(false));
            verify(draftRepo).delete(eq("draft-1"), eq("u-1"));
        }


        @Test
        @DisplayName("Given invalid draft — When publishNew — Then throws IllegalStateException and nothing persisted")
        void publishNewInvalid() {
            // Given
            var badGraph = List.of(node(2, "Only Final", true));
            when(editorRepo.nodes()).thenReturn(badGraph);
            when(editorRepo.startId()).thenReturn(0);
            // When / Then
            assertThrows(IllegalStateException.class, () -> sut.publishNew("owner", "X"));
            verify(catalogRepo, never()).create(any(), any(), anyInt(), anyList(), anyBoolean(), anyString());
        }

        @Test
        @DisplayName("Given valid draft — When submitNewForModeration — Then stageCreate called, editor cleared, temp draft removed")
        void submitNewHappyPath() {
            // Given
            var graph = simpleGraph();
            when(editorRepo.nodes()).thenReturn(graph);
            when(editorRepo.startId()).thenReturn(1);
            when(currentUser.currentUserId()).thenReturn("u-1");
            DraftRow row = mock(DraftRow.class);
            when(row.getDraftId()).thenReturn("draft-2");
            when(draftRepo.findLatest("u-1", null)).thenReturn(Optional.of(row));
            // When
            sutWithDrafts.submitNewForModeration("owner-1", "Name");
            // Then
            verify(catalogRepo).stageCreate(eq("owner-1"), eq("Name"), eq(1), anyList(), anyString());
            verify(editorRepo).reload(eq(Collections.emptyList()), eq(0), eq(false));
            verify(draftRepo).delete(eq("draft-2"), eq("u-1"));
        }
    }

    @Nested
    @DisplayName("updateExisting(questId, asAdmin)")
    class UpdateExisting {

        @Test
        @DisplayName("Given quest exists and asAdmin=true — When updateExisting — Then update(LIVE) called and draft removed")
        void adminPublishesUpdate() {
            // Given
            String qid = "q1";
            when(catalogRepo.get(qid)).thenReturn(Optional.of(mock(CustomQuest.class)));
            when(editorRepo.nodes()).thenReturn(simpleGraph());
            when(editorRepo.startId()).thenReturn(1);
            when(currentUser.currentUserId()).thenReturn("u-1");
            DraftRow row = mock(DraftRow.class);
            when(row.getDraftId()).thenReturn("draft-q1");
            when(draftRepo.findLatest("u-1", qid)).thenReturn(Optional.of(row));
            // When
            sutWithDrafts.updateExisting(qid, true);
            // Then
            verify(catalogRepo).update(eq(qid), eq(1), anyList(), eq(true), anyString());
            verify(draftRepo).delete(eq("draft-q1"), eq("u-1"));
        }

        @Test
        @DisplayName("Given quest exists and asAdmin=false — When updateExisting — Then stageEdit called and draft removed")
        void nonAdminStagesEdit() {
            // Given
            String qid = "q1";
            when(catalogRepo.get(qid)).thenReturn(Optional.of(mock(CustomQuest.class)));
            when(editorRepo.nodes()).thenReturn(simpleGraph());
            when(editorRepo.startId()).thenReturn(1);
            when(currentUser.currentUserId()).thenReturn("u-1");
            DraftRow row = mock(DraftRow.class);
            when(row.getDraftId()).thenReturn("draft-q1b");
            when(draftRepo.findLatest("u-1", qid)).thenReturn(Optional.of(row));
            // When
            sutWithDrafts.updateExisting(qid, false);
            // Then
            verify(catalogRepo).stageEdit(eq(qid), eq(1), anyList(), anyString());
            verify(draftRepo).delete(eq("draft-q1b"), eq("u-1"));
        }

        @Test
        @DisplayName("Given quest missing — When updateExisting — Then throws IllegalArgumentException")
        void updateMissingThrows() {
            // Given
            String qid = "missing";
            when(catalogRepo.get(qid)).thenReturn(Optional.empty());
            // When / Then
            assertThrows(IllegalArgumentException.class, () -> sut.updateExisting(qid, true));
            verify(catalogRepo, never()).update(anyString(), anyInt(), anyList(), anyBoolean(), anyString());
            verify(catalogRepo, never()).stageEdit(anyString(), anyInt(), anyList(), anyString());
        }
    }

    @Nested
    @DisplayName("validateCurrentDraft()")
    class ValidateDraft {

        @Test
        @DisplayName("Given empty editor — When validate — Then returns error 'Draft is empty'")
        void emptyDraft() {
            // Given
            when(editorRepo.nodes()).thenReturn(List.of());
            // When
            List<String> errs = sut.validateCurrentDraft();
            // Then
            assertFalse(errs.isEmpty());
            assertTrue(errs.stream().anyMatch(s -> s.toLowerCase(Locale.ROOT).contains("empty")));
        }

        @Test
        @DisplayName("Given no node #1 — When validate — Then error 'Start node #1 is required'")
        void missingStartNodeOne() {
            // Given
            when(editorRepo.nodes()).thenReturn(List.of(node(2, "X", true)));
            when(editorRepo.startId()).thenReturn(0);
            // When
            List<String> errs = sut.validateCurrentDraft();
            // Then
            assertTrue(errs.stream().anyMatch(s -> s.contains("#1")));
        }

        @Test
        @DisplayName("Given non-final without options — When validate — Then error on that node")
        void nonFinalWithoutOptions() {
            // Given
            List<QuestNode> bad = List.of(node(1, "Root", false));
            when(editorRepo.nodes()).thenReturn(bad);
            when(editorRepo.startId()).thenReturn(1);
            // When
            List<String> errs = sut.validateCurrentDraft();
            // Then
            assertTrue(errs.stream().anyMatch(s -> s.contains("Node #1")));
        }

        @Test
        @DisplayName("Given broken link — When validate — Then error 'broken link'")
        void brokenLink() {
            // Given
            List<QuestNode> g = List.of(node(1, "Root", false, opt(42)));
            when(editorRepo.nodes()).thenReturn(g);
            when(editorRepo.startId()).thenReturn(1);
            // When
            List<String> errs = sut.validateCurrentDraft();
            // Then
            assertTrue(errs.stream().anyMatch(s -> s.toLowerCase(Locale.ROOT).contains("broken link")));
        }

        @Test
        @DisplayName("Given valid graph — When validate — Then returns empty list")
        void validDraft() {
            // Given
            when(editorRepo.nodes()).thenReturn(simpleGraph());
            when(editorRepo.startId()).thenReturn(1);
            // When
            List<String> errs = sut.validateCurrentDraft();
            // Then
            assertTrue(errs.isEmpty());
        }
    }

    @Nested
    @DisplayName("saveNode / deleteNode / setStart")
    class Mutations {

        @Test
        @DisplayName("Given new #1 — When saveNode — Then startId forced to 1 and autosave happens if drafts enabled")
        void saveNodeForcesStartOne() {
            // Given
            when(currentUser.currentUserId()).thenReturn("u-1");
            when(editorRepo.nodes()).thenReturn(simpleGraph());
            when(editorRepo.startId()).thenReturn(1);
            // When
            sutWithDrafts.saveNode(node(1, "Start", false, opt(2)));
            // Then
            verify(editorRepo).replaceNode(any(QuestNode.class));
            verify(editorRepo).setStartId(1);
            verify(draftRepo).upsertDraft(eq("u-1"), any(), anyString(), eq(1), anyList(), anyString());
        }

        @Test
        @DisplayName("Given existing #1 — When deleteNode(1) — Then startId reset to 0 and autosave (drafts on)")
        void deleteNodeResetsStartWhenOne() {
            // Given
            when(editorRepo.deleteNode(1)).thenReturn(true);
            when(editorRepo.startId()).thenReturn(0);
            when(editorRepo.nodes()).thenReturn(List.of());
            when(currentUser.currentUserId()).thenReturn("u-1");
            // When
            boolean ok = sutWithDrafts.deleteNode(1);
            // Then
            assertTrue(ok);
            verify(editorRepo).setStartId(0);
            verify(draftRepo).upsertDraft(eq("u-1"), any(), anyString(), eq(0), anyList(), anyString());
        }

        @Test
        @DisplayName("Given anything — When setStart(x) — Then ignores x and forces 1")
        void setStartAlwaysOne() {
            // Given / When
            sut.setStart(42);
            // Then
            verify(editorRepo).setStartId(1);
        }
    }

    @Nested
    @DisplayName("listAllFromCatalogPaged / listOwnerFromCatalogPaged")
    class CatalogPaging {

        @Test
        @DisplayName("Given total=0 — When listAllFromCatalogPaged — Then returns empty page 1/1")
        void allEmpty() {
            // Given
            when(catalogRepo.countAllLive(null)).thenReturn(0);
            // When
            QuestAuthoringService.Paged<CustomQuest> out = sut.listAllFromCatalogPaged(null, 1, 10);
            // Then
            assertEquals(0, out.getItems().size());
            assertEquals(0, out.getTotal());
            assertEquals(1, out.getPage());
            assertEquals(1, out.getPages());
            verify(catalogRepo, never()).findAllLivePaged(anyInt(), anyInt(), any());
        }

        @Test
        @DisplayName("Given total>0 — When listAllFromCatalogPaged — Then clamps page/size and fetches items")
        void allNonEmpty() {
            // Given
            when(catalogRepo.countAllLive("demo")).thenReturn(3);
            List<CustomQuest> items = List.of(mock(CustomQuest.class), mock(CustomQuest.class));
            when(catalogRepo.findAllLivePaged(1, 2, "demo")).thenReturn(items);
            // When
            QuestAuthoringService.Paged<CustomQuest> out = sut.listAllFromCatalogPaged("demo", 1, 2);
            // Then
            assertEquals(2, out.getItems().size());
            assertEquals(3, out.getTotal());
            assertEquals(1, out.getPage());
            assertEquals(2, out.getSize());
        }

        @Test
        @DisplayName("Given viewer not owner — When listOwnerFromCatalogPaged — Then onlyLive=true")
        void ownerPagedOnlyLiveForStranger() {
            // Given
            when(catalogRepo.countByOwner("owner", null, true)).thenReturn(2);
            when(catalogRepo.findByOwnerPaged("owner", 1, 10, null, true)).thenReturn(List.of(mock(CustomQuest.class)));
            // When
            QuestAuthoringService.Paged<CustomQuest> out = sut.listOwnerFromCatalogPaged("owner", null, 1, 10, false);
            // Then
            assertEquals(2, out.getTotal());
            verify(catalogRepo).findByOwnerPaged("owner", 1, 10, null, true);
        }

        @Test
        @DisplayName("Given viewer owner/admin — When listOwnerFromCatalogPaged — Then onlyLive=false")
        void ownerPagedAllForOwner() {
            // Given
            when(catalogRepo.countByOwner("owner", "q", false)).thenReturn(1);
            when(catalogRepo.findByOwnerPaged("owner", 1, 5, "q", false)).thenReturn(List.of(mock(CustomQuest.class)));
            // When
            QuestAuthoringService.Paged<CustomQuest> out = sut.listOwnerFromCatalogPaged("owner", "q", 1, 5, true);
            // Then
            assertEquals(1, out.getTotal());
            verify(catalogRepo).findByOwnerPaged("owner", 1, 5, "q", false);
        }
    }

    @Nested
    @DisplayName("Moderation pass-through methods")
    class ModerationPassthrough {

        @Test
        @DisplayName("Given repository returns list — When listPendingNew — Then returns same list")
        void listPendingNew() {
            // Given
            List<CustomQuestRepository.PendingNew> list = List.of(mock(CustomQuestRepository.PendingNew.class));
            when(catalogRepo.listPendingNew()).thenReturn(list);
            // When
            var out = sut.listPendingNew();
            // Then
            assertEquals(list, out);
        }

        @Test
        @DisplayName("Given repository returns list — When listPendingEdits — Then returns same list")
        void listPendingEdits() {
            // Given
            List<CustomQuestRepository.PendingEdit> list = List.of(mock(CustomQuestRepository.PendingEdit.class));
            when(catalogRepo.listPendingEdits()).thenReturn(list);
            // When
            var out = sut.listPendingEdits();
            // Then
            assertEquals(list, out);
        }

        @Test
        @DisplayName("Given pendingId — When approveCreate — Then delegates to repo and returns new id")
        void approveCreate() {
            // Given
            when(catalogRepo.approveCreate("pid")).thenReturn("qid");
            // When
            String id = sut.approveCreate("pid");
            // Then
            assertEquals("qid", id);
        }

        @Test
        @DisplayName("Given pendingId — When rejectCreate — Then delegates to repo")
        void rejectCreate() {
            // When
            sut.rejectCreate("pid");
            // Then
            verify(catalogRepo).rejectCreate("pid");
        }

        @Test
        @DisplayName("Given questId — When approveEdit — Then delegates to repo")
        void approveEdit() {
            // When
            sut.approveEdit("qid");
            // Then
            verify(catalogRepo).approveEdit("qid");
        }

        @Test
        @DisplayName("Given questId — When rejectEdit — Then delegates to repo")
        void rejectEdit() {
            // When
            sut.rejectEdit("qid");
            // Then
            verify(catalogRepo).rejectEdit("qid");
        }
    }

    @Nested
    @DisplayName("deleteFromCatalogIfOwner / deleteFromCatalogAsAdmin")
    class Deletion {

        @Test
        @DisplayName("Given repo returns true — When deleteFromCatalogIfOwner — Then returns true")
        void deleteIfOwnerTrue() {
            when(catalogRepo.deleteIfOwner("q", "u")).thenReturn(true);
            assertTrue(sut.deleteFromCatalogIfOwner("q", "u"));
        }

        @Test
        @DisplayName("Given repo returns false — When deleteFromCatalogIfOwner — Then returns false")
        void deleteIfOwnerFalse() {
            when(catalogRepo.deleteIfOwner("q", "u")).thenReturn(false);
            assertFalse(sut.deleteFromCatalogIfOwner("q", "u"));
        }

        @Test
        @DisplayName("Given repo returns true — When deleteFromCatalogAsAdmin — Then returns true")
        void deleteAsAdminTrue() {
            when(catalogRepo.delete("q")).thenReturn(true);
            assertTrue(sut.deleteFromCatalogAsAdmin("q"));
        }

        @Test
        @DisplayName("Given repo returns false — When deleteFromCatalogAsAdmin — Then returns false")
        void deleteAsAdminFalse() {
            when(catalogRepo.delete("q")).thenReturn(false);
            assertFalse(sut.deleteFromCatalogAsAdmin("q"));
        }
    }
}