package com.javarush.apalinskiy.domain.save;

import com.javarush.apalinskiy.domain.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SaveState")
class SaveStateTest {

    @Nested
    @DisplayName("questKey() normalization")
    class QuestKeyNormalization {

        @Test
        @DisplayName("returns MAIN_QUEST_KEY when input is null")
        void returnsMainKeyWhenNull() {
            // Given
            // When
            String result = SaveState.questKey(null);
            // Then
            assertEquals(SaveState.MAIN_QUEST_KEY, result);
        }

        @Test
        @DisplayName("returns MAIN_QUEST_KEY when input is blank")
        void returnsMainKeyWhenBlank() {
            // Given
            String input = "   ";
            // When
            String result = SaveState.questKey(input);
            // Then
            assertEquals(SaveState.MAIN_QUEST_KEY, result);
        }

        @Test
        @DisplayName("returns input when non-blank")
        void returnsInputWhenNonBlank() {
            // Given
            String input = "quest-123";
            // When
            String result = SaveState.questKey(input);
            // Then
            assertEquals("quest-123", result);
        }
    }

    @Nested
    @DisplayName("Lifecycle callbacks")
    class LifecycleCallbacks {

        @Test
        @DisplayName("@PrePersist sets updatedAt if null")
        void prePersist_setsTimestampIfNull() {
            // Given
            SaveState s = new SaveState();
            s.setUpdatedAt(null);
            Instant before = Instant.now();
            // When
            s.prePersist();
            // Then
            Instant after = Instant.now();
            assertNotNull(s.getUpdatedAt(), "updatedAt must be set");
            assertFalse(s.getUpdatedAt().isBefore(before));
            assertFalse(s.getUpdatedAt().isAfter(after));
        }

        @Test
        @DisplayName("@PrePersist keeps existing updatedAt")
        void prePersist_keepsExistingTimestamp() {
            // Given
            Instant ts = Instant.parse("2025-01-01T00:00:00Z");
            SaveState s = new SaveState();
            s.setUpdatedAt(ts);
            // When
            s.prePersist();
            // Then
            assertSame(ts, s.getUpdatedAt(), "prePersist should not override non-null timestamp");
        }

        @Test
        @DisplayName("@PreUpdate always bumps updatedAt to newer value")
        void preUpdate_bumpsTimestamp() {
            // Given
            SaveState s = new SaveState();
            Instant oldTs = Instant.now().minusSeconds(60);
            s.setUpdatedAt(oldTs);
            // When
            s.preUpdate();
            // Then
            assertTrue(s.getUpdatedAt().isAfter(oldTs), "updatedAt must move forward on update");
        }
    }

    @Nested
    @DisplayName("Default values and relationships")
    class DefaultsAndRelations {

        @Test
        @DisplayName("has default slotCount = 10")
        void hasDefaultSlotCount() {
            // Given / When
            SaveState s = new SaveState();
            // Then
            assertEquals(10, s.getSlotCount());
        }

        @Test
        @DisplayName("has default version = 3")
        void hasDefaultVersion() {
            // Given / When
            SaveState s = new SaveState();
            // Then
            assertEquals(3, s.getVersion());
        }

        @Test
        @DisplayName("has non-null globalSlots list initially")
        void hasEmptyGlobalSlotsList() {
            // Given / When
            SaveState s = new SaveState();
            // Then
            assertNotNull(s.getGlobalSlots());
            assertTrue(s.getGlobalSlots().isEmpty());
        }

        @Test
        @DisplayName("stores reference to User entity")
        void storesUserReference() {
            // Given
            User u = new User();
            u.setUserId("abc-123");
            SaveState s = new SaveState();
            // When
            s.setUser(u);
            // Then
            assertSame(u, s.getUser());
        }

        @Test
        @DisplayName("stores globalSlots collection and maintains order")
        void storesAndOrdersGlobalSlots() {
            // Given
            SaveState s = new SaveState();
            GlobalSlot g1 = new GlobalSlot();
            GlobalSlot g2 = new GlobalSlot();
            s.setGlobalSlots(List.of(g2, g1));
            // When
            List<GlobalSlot> list = s.getGlobalSlots();
            // Then
            assertEquals(2, list.size());
            assertTrue(list.contains(g1));
            assertTrue(list.contains(g2));
        }
    }
}