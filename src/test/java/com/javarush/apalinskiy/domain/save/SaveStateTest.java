package com.javarush.apalinskiy.domain.save;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SaveState")
class SaveStateTest {

    @Nested
    @DisplayName("constructors")
    class Ctors {

        @Test
        @DisplayName("default ctor sets slotCount=10 when created then 10")
        void defaultCtorSlotCount() {
            // Given / When
            SaveState s = new SaveState("u1");
            // Then
            assertEquals(10, s.getSlotCount());
        }

        @Test
        @DisplayName("ctor sets slotCount when positive then exact")
        void ctorPositiveSlotCount() {
            // Given / When
            SaveState s = new SaveState("u1", 3);
            // Then
            assertEquals(3, s.getSlotCount());
        }

        @Test
        @DisplayName("ctor clamps slotCount>=1 when zero given then 1")
        void ctorClampsToOne() {
            // Given / When
            SaveState s = new SaveState("u1", 0);
            // Then
            assertEquals(1, s.getSlotCount());
        }

        @Test
        @DisplayName("stores userId when created then equals")
        void storesUserId() {
            // Given / When
            SaveState s = new SaveState("userX");
            // Then
            assertEquals("userX", s.getUserId());
        }

        @Test
        @DisplayName("sets version=3 by default when created then 3")
        void defaultVersion() {
            // Given / When
            SaveState s = new SaveState("u1");
            // Then
            assertEquals(3, s.getVersion());
        }
    }

    @Nested
    @DisplayName("questKey()")
    class QuestKey {

        @Test
        @DisplayName("returns 'main' when id is null then main")
        void nullIsMain() {
            // Given
            // When
            String key = SaveState.questKey(null);
            // Then
            assertEquals(SaveState.MAIN_QUEST_KEY, key);
        }

        @Test
        @DisplayName("returns 'main' when id is blank then main")
        void blankIsMain() {
            // Given
            String in = "   ";
            // When
            String key = SaveState.questKey(in);
            // Then
            assertEquals(SaveState.MAIN_QUEST_KEY, key);
        }

        @Test
        @DisplayName("returns same value when id present then same")
        void valuePassesThrough() {
            // Given
            String in = "q123";
            // When
            String key = SaveState.questKey(in);
            // Then
            assertEquals("q123", key);
        }
    }

    @Nested
    @DisplayName("getGlobalSlot()")
    class GetGlobalSlot {

        @Test
        @DisplayName("uses MAIN when questId is blank then questId='main'")
        void usesMainWhenQuestIdBlank() {
            // Given
            SaveState s = new SaveState("u1");
            // When
            s.setGlobalSlot(0, "   ", "Q", 1, "T");
            // Then
            assertEquals(SaveState.MAIN_QUEST_KEY, s.getGlobalSlot(0).getQuestId());
        }

        @Test
        @DisplayName("passes through non-blank questId then questId kept")
        void passesThroughQuestId() {
            // Given
            SaveState s = new SaveState("u1");
            // When
            s.setGlobalSlot(0, "q42", "Q", 1, "T");
            // Then
            assertEquals("q42", s.getGlobalSlot(0).getQuestId());
        }

        @Test
        @DisplayName("accepts last valid index (slotCount-1) then slot set")
        void acceptsLastIndex() {
            // Given
            SaveState s = new SaveState("u1", 2);
            // When
            s.setGlobalSlot(1, "q1", "Q", 7, "T");
            // Then
            assertNotNull(s.getGlobalSlot(1));
        }

        @Test
        @DisplayName("throws when index == length then IAE")
        void throwsWhenIndexEqualsLength() {
            // Given
            SaveState s = new SaveState("u1", 2);
            // When / Then
            assertThrows(IllegalArgumentException.class,
                    () -> s.setGlobalSlot(2, "q", "Q", 1, "T"));
        }

        @Test
        @DisplayName("returns null when index negative then null")
        void returnsNullNegative() {
            // Given
            SaveState s = new SaveState("u1");
            // When
            SaveState.SaveSlot slot = s.getGlobalSlot(-1);
            // Then
            assertNull(slot);
        }

        @Test
        @DisplayName("returns null when index >= count then null")
        void returnsNullOutOfBounds() {
            // Given
            SaveState s = new SaveState("u1");
            // When
            SaveState.SaveSlot slot = s.getGlobalSlot(10);
            // Then
            assertNull(slot);
        }

        @Test
        @DisplayName("returns null when not set yet then null")
        void returnsNullWhenEmpty() {
            // Given
            SaveState s = new SaveState("u1");
            // When
            SaveState.SaveSlot slot = s.getGlobalSlot(0);
            // Then
            assertNull(slot);
        }
    }

    @Nested
    @DisplayName("setGlobalSlot()")
    class SetGlobalSlot {

        @Test
        @DisplayName("sets last slot (slotCount-1) when in bounds then fields populated")
        void setsLastValidIndex() {
            // Given
            SaveState s = new SaveState("u1", 2);
            // When
            s.setGlobalSlot(1, "q42", "Quest 42", 99, "Final checkpoint");
            // Then
            assertNotNull(s.getGlobalSlot(1));
            assertEquals("q42", s.getGlobalSlot(1).getQuestId());
            assertEquals("Quest 42", s.getGlobalSlot(1).getQuestName());
            assertEquals(99, s.getGlobalSlot(1).getNodeId());
            assertEquals("Final checkpoint", s.getGlobalSlot(1).getTitle());
            assertNotNull(s.getGlobalSlot(1).getUpdatedAt());
        }

        @Test
        @DisplayName("sets slot when in bounds then not null")
        void setsSlot() {
            // Given
            SaveState s = new SaveState("u1");
            // When
            s.setGlobalSlot(0, "q1", "Quest 1", 7, "Title");
            // Then
            assertNotNull(s.getGlobalSlot(0));
        }

        @Test
        @DisplayName("does nothing when index == length then updatedAt unchanged")
        void noopWhenIndexEqualsLength() {
            // Given
            SaveState s = new SaveState("u1", 2);
            Instant before = s.getUpdatedAt();
            // When
            s.clearGlobalSlot(2);
            // Then
            assertEquals(before, s.getUpdatedAt());
        }

        @Test
        @DisplayName("applies questKey when questId=null then 'main'")
        void appliesQuestKey() {
            // Given
            SaveState s = new SaveState("u1");
            // When
            s.setGlobalSlot(0, null, "Quest", 1, "T");
            // Then
            assertEquals(SaveState.MAIN_QUEST_KEY, s.getGlobalSlot(0).getQuestId());
        }

        @Test
        @DisplayName("sets fields questName/nodeId/title when provided then equals")
        void setsFields() {
            // Given
            SaveState s = new SaveState("u1");
            // When
            s.setGlobalSlot(0, "q1", "Quest", 42, "Checkpoint");
            // Then
            assertEquals("Quest", s.getGlobalSlot(0).getQuestName());
            assertEquals(42, s.getGlobalSlot(0).getNodeId());
            assertEquals("Checkpoint", s.getGlobalSlot(0).getTitle());
        }

        @Test
        @DisplayName("sets slot.updatedAt to now when set then not null")
        void setsSlotUpdatedAt() {
            // Given
            SaveState s = new SaveState("u1");
            // When
            s.setGlobalSlot(0, "q1", "Q", 1, "T");
            // Then
            assertNotNull(s.getGlobalSlot(0).getUpdatedAt());
        }

        @Test
        @DisplayName("touches SaveState.updatedAt when set then >= before")
        void touchesOnSet() {
            // Given
            SaveState s = new SaveState("u1");
            Instant before = s.getUpdatedAt();
            // When
            s.setGlobalSlot(0, "q1", "Q", 1, "T");
            Instant after = s.getUpdatedAt();
            // Then
            assertFalse(after.isBefore(before));
        }

        @Test
        @DisplayName("throws when index out of bounds then IAE")
        void throwsWhenOob() {
            // Given
            SaveState s = new SaveState("u1");
            // When / Then
            assertThrows(IllegalArgumentException.class,
                    () -> s.setGlobalSlot(-1, "q", "Q", 1, "T"));
        }
    }

    @Nested
    @DisplayName("clearGlobalSlot()")
    class ClearGlobalSlot {

        @Test
        @DisplayName("clears slot when in bounds then null afterwards")
        void clearsSlot() {
            // Given
            SaveState s = new SaveState("u1");
            s.setGlobalSlot(0, "q1", "Q", 1, "T");
            // When
            s.clearGlobalSlot(0);
            // Then
            assertNull(s.getGlobalSlot(0));
        }

        @Test
        @DisplayName("clears in-bounds empty slot when called then touches updatedAt")
        void clearsEmptyInBoundsAndTouches() {
            // Given
            SaveState s = new SaveState("u1");
            Instant before = s.getUpdatedAt();
            // When
            s.clearGlobalSlot(0);
            // Then
            assertNull(s.getGlobalSlot(0));
            assertFalse(s.getUpdatedAt().isBefore(before));
        }

        @Test
        @DisplayName("does nothing when index out of bounds then no change to updatedAt")
        void noopWhenOob() {
            // Given
            SaveState s = new SaveState("u1");
            Instant before = s.getUpdatedAt();
            // When
            s.clearGlobalSlot(-5);
            // Then
            assertEquals(before, s.getUpdatedAt());
        }
    }

    @Nested
    @DisplayName("touch()")
    class TouchMethod {

        @Test
        @DisplayName("updates updatedAt when called then >= before")
        void updatesUpdatedAt() {
            // Given
            SaveState s = new SaveState("u1");
            Instant before = s.getUpdatedAt();
            // When
            s.touch();
            Instant after = s.getUpdatedAt();
            // Then
            assertFalse(after.isBefore(before));
        }
    }
}