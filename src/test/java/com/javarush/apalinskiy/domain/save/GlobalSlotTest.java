package com.javarush.apalinskiy.domain.save;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GlobalSlot")
class GlobalSlotTest {

    @Nested
    @DisplayName("Lifecycle callbacks")
    class LifecycleCallbacks {

        @Test
        @DisplayName("@PrePersist sets updatedAt when null")
        void prePersist_setsTimestampWhenNull() {
            // Given
            GlobalSlot slot = new GlobalSlot();
            slot.setUpdatedAt(null);
            Instant before = Instant.now();
            // When
            slot.prePersist();
            Instant after = Instant.now();
            // Then
            assertNotNull(slot.getUpdatedAt(), "updatedAt must be set on prePersist when null");
            assertFalse(slot.getUpdatedAt().isBefore(before), "updatedAt must be >= before");
            assertFalse(slot.getUpdatedAt().isAfter(after), "updatedAt must be <= after");
            assertTrue(Duration.between(before, slot.getUpdatedAt()).toMillis() >= 0);
        }

        @Test
        @DisplayName("@PrePersist keeps existing updatedAt")
        void prePersist_keepsExistingTimestamp() {
            // Given
            Instant existing = Instant.parse("2025-01-02T03:04:05Z");
            GlobalSlot slot = new GlobalSlot();
            slot.setUpdatedAt(existing);
            // When
            slot.prePersist();
            // Then
            assertSame(existing, slot.getUpdatedAt(), "prePersist must not overwrite non-null updatedAt");
        }

        @Test
        @DisplayName("@PreUpdate bumps updatedAt to a newer instant")
        void preUpdate_bumpsTimestamp() {
            // Given
            Instant oldTs = Instant.now().minusSeconds(60);
            GlobalSlot slot = new GlobalSlot();
            slot.setUpdatedAt(oldTs);
            // When
            slot.preUpdate();
            // Then
            assertNotNull(slot.getUpdatedAt(), "updatedAt must be set");
            assertTrue(slot.getUpdatedAt().isAfter(oldTs), "updatedAt must be strictly newer after preUpdate()");
        }
    }

    @Nested
    @DisplayName("Accessors & basic state")
    class Accessors {

        @Test
        @DisplayName("stores composite id (GlobalSlotId)")
        void storesCompositeId() {
            // Given
            GlobalSlotId id = new GlobalSlotId("user-123", 7);
            GlobalSlot slot = new GlobalSlot();
            // When
            slot.setId(id);
            // Then
            assertSame(id, slot.getId(), "GlobalSlotId should be stored and returned as-is");
            assertEquals("user-123", slot.getId().getUserId());
            assertEquals(7, slot.getId().getSlotIndex());
        }

        @Test
        @DisplayName("stores quest fields (questId, questName, nodeId, title)")
        void storesQuestFields() {
            // Given
            GlobalSlot slot = new GlobalSlot();
            // When
            slot.setQuestId("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
            slot.setQuestName("Demo Quest");
            slot.setNodeId(42);
            slot.setTitle("My Save");
            // Then
            assertEquals("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", slot.getQuestId());
            assertEquals("Demo Quest", slot.getQuestName());
            assertEquals(42, slot.getNodeId());
            assertEquals("My Save", slot.getTitle());
        }

        @Test
        @DisplayName("stores reference to parent SaveState (mapped via @MapsId at runtime)")
        void storesSaveStateReference() {
            // Given
            SaveState parent = new SaveState();
            parent.setUserId("user-xyz");
            GlobalSlot slot = new GlobalSlot();
            // When
            slot.setSaveState(parent);
            // Then
            assertSame(parent, slot.getSaveState(), "SaveState reference should be stored");
        }
    }
}