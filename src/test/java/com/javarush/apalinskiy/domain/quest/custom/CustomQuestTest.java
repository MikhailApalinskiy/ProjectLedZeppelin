package com.javarush.apalinskiy.domain.quest.custom;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.javarush.apalinskiy.domain.quest.QuestNode;
import com.javarush.apalinskiy.domain.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomQuest entity")
class CustomQuestTest {

    @Mock
    User user;
    @Mock
    QuestNode nodeA;
    @Mock
    QuestNode nodeB;

    @Captor
    ArgumentCaptor<CustomQuest> questCaptor;

    private static void invoke(Object target, String name) throws Exception {
        Method m = CustomQuest.class.getDeclaredMethod(name);
        m.setAccessible(true);
        m.invoke(target);
    }

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("should initialize required fields and defaults")
        void initializesFieldsAndDefaults() {
            // Given
            String id = UUID.randomUUID().toString();
            List<QuestNode> nodes = List.of();
            // When
            CustomQuest q = new CustomQuest(id, user, "Quest", 1, nodes, null, null, null, null);
            // Then
            assertEquals(id, q.getId());
            assertSame(user, q.getUser());
            assertEquals("Quest", q.getName());
            assertEquals(1, q.getStartId());
            assertEquals(CustomQuest.ModerationStatus.LIVE, q.getModerationStatus());
            assertEquals("", q.getVersion());
            assertNotNull(q.getCreatedAt());
            assertEquals(q.getCreatedAt(), q.getUpdatedAt());
        }

        @Test
        @DisplayName("should trim name")
        void trimsName() {
            // Given
            String raw = "   Trim  ";
            // When
            CustomQuest q = new CustomQuest(UUID.randomUUID().toString(), user, raw, 10, null, null, null, null, null);
            // Then
            assertEquals("Trim", q.getName());
        }

        @Test
        @DisplayName("should throw when name blank or too long")
        void throwsOnInvalidName() {
            // Given
            String tooLong = "a".repeat(51);
            // When / Then
            assertThrows(IllegalArgumentException.class, () ->
                    new CustomQuest(UUID.randomUUID().toString(), user, "   ", 1, null, null, null, null, null));
            assertThrows(IllegalArgumentException.class, () ->
                    new CustomQuest(UUID.randomUUID().toString(), user, tooLong, 1, null, null, null, null, null));
        }

        @Test
        @DisplayName("should throw when id/user/startId is null")
        void throwsOnNulls() {
            // Given
            String id = UUID.randomUUID().toString();
            // When / Then
            assertThrows(NullPointerException.class, () ->
                    new CustomQuest(null, user, "N", 1, null, null, null, null, null));
            assertThrows(NullPointerException.class, () ->
                    new CustomQuest(id, null, "N", 1, null, null, null, null, null));
            assertThrows(NullPointerException.class, () ->
                    new CustomQuest(id, user, "N", null, null, null, null, null, null));
        }
    }

    @Nested
    @DisplayName("factory method 'create'")
    class FactoryMethod {

        @Test
        @DisplayName("should generate UUID and timestamps")
        void generatesUuidAndTimestamps() {
            // Given
            // When
            CustomQuest q = CustomQuest.create(user, "Q", 1, null, "v", true);
            // Then
            assertDoesNotThrow(() -> UUID.fromString(q.getId()));
            assertNotNull(q.getCreatedAt());
            assertNotNull(q.getUpdatedAt());
        }

        @Test
        @DisplayName("should set LIVE when publishImmediately=true")
        void setsLive() {
            // Given
            // When
            CustomQuest q = CustomQuest.create(user, "Q", 1, null, "v", true);
            // Then
            assertEquals(CustomQuest.ModerationStatus.LIVE, q.getModerationStatus());
        }

        @Test
        @DisplayName("should set PENDING_NEW when publishImmediately=false")
        void setsPendingNew() {
            // Given
            // When
            CustomQuest q = CustomQuest.create(user, "Q", 1, null, "v", false);
            // Then
            assertEquals(CustomQuest.ModerationStatus.PENDING_NEW, q.getModerationStatus());
        }

        @Test
        @DisplayName("should assign input fields correctly")
        void assignsInputs() {
            // Given
            List<QuestNode> nodes = new ArrayList<>();
            // When
            CustomQuest q = CustomQuest.create(user, "N", 42, nodes, "v1", true);
            // Then
            assertSame(user, q.getUser());
            assertEquals("N", q.getName());
            assertEquals(42, q.getStartId());
            assertEquals("v1", q.getVersion());
        }
    }

    @Nested
    @DisplayName("node handling")
    class NodeHandling {

        @Test
        @DisplayName("constructor sets quest on each node")
        void ctorSetsBackrefs() {
            // Given
            List<QuestNode> nodes = List.of(nodeA, nodeB);
            // When
            CustomQuest q = new CustomQuest(UUID.randomUUID().toString(), user, "N", 1, nodes, null, null, null, null);
            // Then
            verify(nodeA).setQuest(q);
            verify(nodeB).setQuest(q);
            assertEquals(2, q.getNodes().size());
        }

        @Test
        @DisplayName("constructor ignores null nodes")
        void ignoresNullNodes() {
            // Given
            List<QuestNode> nodes = new ArrayList<>(Arrays.asList(nodeA, null));
            // When
            CustomQuest q = new CustomQuest(UUID.randomUUID().toString(), user, "N", 1, nodes, null, null, null, null);
            // Then
            verify(nodeA).setQuest(q);
            assertEquals(1, q.getNodes().size());
        }

        @Test
        @DisplayName("applyUpdate replaces nodes and sets quest refs")
        void applyUpdateSetsBackrefs() {
            // Given
            CustomQuest q = CustomQuest.create(user, "Q", 1, null, "v", true);
            // When
            q.applyUpdate(List.of(nodeA, nodeB), 7, null, null);
            // Then
            verify(nodeA).setQuest(q);
            verify(nodeB).setQuest(q);
            assertEquals(2, q.getNodes().size());
            assertEquals(7, q.getStartId());
        }
    }

    @Nested
    @DisplayName("applyUpdate")
    class ApplyUpdate {

        @Test
        @DisplayName("requires non-null startId")
        void requiresStartId() {
            // Given
            CustomQuest q = CustomQuest.create(user, "N", 1, null, "v", true);
            // When / Then
            assertThrows(NullPointerException.class, () ->
                    q.applyUpdate(List.of(), null, null, null));
        }

        @Test
        @DisplayName("updates status and version when provided")
        void updatesStatusAndVersion() {
            // Given
            CustomQuest q = CustomQuest.create(user, "N", 1, null, "v1", true);
            // When
            q.applyUpdate(List.of(), 2, CustomQuest.ModerationStatus.REJECTED, "v2");
            // Then
            assertEquals(CustomQuest.ModerationStatus.REJECTED, q.getModerationStatus());
            assertEquals("v2", q.getVersion());
        }

        @Test
        @DisplayName("preserves status and version when null")
        void keepsStatusAndVersion() {
            // Given
            CustomQuest q = CustomQuest.create(user, "N", 1, null, "v1", true);
            // When
            q.applyUpdate(List.of(), 2, null, null);
            // Then
            assertEquals(CustomQuest.ModerationStatus.LIVE, q.getModerationStatus());
            assertEquals("v1", q.getVersion());
        }

        @Test
        @DisplayName("should bump updatedAt timestamp")
        void bumpsUpdatedAt() {
            // Given
            CustomQuest q = CustomQuest.create(user, "N", 1, null, "v", true);
            Instant before = q.getUpdatedAt();
            // When
            q.applyUpdate(List.of(), 1, null, null);
            // Then
            assertFalse(q.getUpdatedAt().isBefore(before));
        }
    }

    @Nested
    @DisplayName("lifecycle callbacks")
    class Lifecycle {

        @Test
        @DisplayName("@PrePersist fills defaults when null")
        void prePersistFillsDefaults() throws Exception {
            // Given
            CustomQuest q = new CustomQuest(UUID.randomUUID().toString(), user, "N", 1, null, null, null, null, null);
            // When
            invoke(q, "onCreate");
            // Then
            assertNotNull(q.getCreatedAt());
            assertEquals(q.getCreatedAt(), q.getUpdatedAt());
            assertEquals(CustomQuest.ModerationStatus.LIVE, q.getModerationStatus());
            assertEquals("", q.getVersion());
        }

        @Test
        @DisplayName("@PreUpdate updates timestamp")
        void preUpdateUpdatesTimestamp() throws Exception {
            // Given
            CustomQuest q = CustomQuest.create(user, "N", 1, null, "v", true);
            Instant before = q.getUpdatedAt();
            // When
            invoke(q, "onUpdate");
            // Then
            assertFalse(q.getUpdatedAt().isBefore(before));
        }

        @Test
        @DisplayName("@PostLoad syncs ownerId from user")
        void postLoadSyncsOwnerId() throws Exception {
            // Given
            when(user.getUserId()).thenReturn("U-1");
            CustomQuest q = CustomQuest.create(user, "N", 1, null, "v", true);
            q.setOwnerId(null);
            // When
            invoke(q, "syncOwnerIdAfterLoad");
            // Then
            assertEquals("U-1", q.getOwnerId());
        }

        @Test
        @DisplayName("@PostLoad sets ownerId=null when user is null")
        void postLoadSetsNullWhenUserNull() throws Exception {
            // Given
            CustomQuest q = CustomQuest.create(user, "N", 1, null, "v", true);
            q.setUser(null);
            q.setOwnerId("x");
            // When
            invoke(q, "syncOwnerIdAfterLoad");
            // Then
            assertNull(q.getOwnerId());
        }
    }

    @Nested
    @DisplayName("misc")
    class Misc {

        @Test
        @DisplayName("applyUpdate should call setQuest with same quest instance")
        void applyUpdateBackrefIdentity() {
            // Given
            CustomQuest q = CustomQuest.create(user, "N", 1, null, "v", true);
            // When
            q.applyUpdate(List.of(nodeA), 2, null, null);
            // Then
            verify(nodeA).setQuest(questCaptor.capture());
            assertSame(q, questCaptor.getValue());
        }
    }
}