package com.javarush.apalinskiy.service.impl.save;

import com.javarush.apalinskiy.domain.save.SaveState;
import com.javarush.apalinskiy.service.save.SaveStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InMemorySaveStateService")
class InMemorySaveStateServiceTest {

    InMemorySaveStateService sut;

    @BeforeEach
    void setUp() {
        sut = new InMemorySaveStateService();
    }

    @Nested
    @DisplayName("getOrCreate(userId)")
    class GetOrCreate {

        @Test
        @DisplayName("creates new state when absent then same instance on next call")
        void createsAndReturnsSame() {
            // Given / When
            SaveState s1 = sut.getOrCreate("u1");
            SaveState s2 = sut.getOrCreate("u1");
            // Then
            assertSame(s1, s2);
        }

        @Test
        @DisplayName("separate instances for different users then not same")
        void separatePerUser() {
            // Given / When
            SaveState a = sut.getOrCreate("a");
            SaveState b = sut.getOrCreate("b");
            // Then
            assertNotSame(a, b);
        }
    }

    @Nested
    @DisplayName("getGlobalSlot(userId, slot)")
    class GetGlobalSlot {

        @Test
        @DisplayName("returns empty when user state missing then empty")
        void emptyWhenNoUser() {
            // Given / When
            Optional<SaveStateService.GlobalSlot> res = sut.getGlobalSlot("ghost", 0);
            // Then
            assertTrue(res.isEmpty());
        }

        @Test
        @DisplayName("returns empty when slot not set then empty")
        void emptyWhenSlotUnset() {
            // Given
            sut.getOrCreate("u");
            // When
            Optional<SaveStateService.GlobalSlot> res = sut.getGlobalSlot("u", 0);
            // Then
            assertTrue(res.isEmpty());
        }

        @Test
        @DisplayName("maps fields when slot set then all fields returned")
        void mapsAllFields() {
            // Given
            sut.setGlobalSlot("u", 1, "qid", "qname", 42, "title");
            // When
            SaveStateService.GlobalSlot gs = sut.getGlobalSlot("u", 1).orElseThrow();
            // Then
            assertEquals(1, gs.index());
            assertEquals(42, gs.nodeId());
            assertEquals("title", gs.title());
            assertEquals("qid", gs.questId());
            assertEquals("qname", gs.questName());
            assertNotNull(gs.updatedAt());
        }

        @Test
        @DisplayName("returns empty when slot out of range then empty")
        void emptyWhenOutOfRange() {
            // Given
            sut.getOrCreate("u2");
            // When
            Optional<SaveStateService.GlobalSlot> res = sut.getGlobalSlot("u2", 999);
            // Then
            assertTrue(res.isEmpty());
        }
    }

    @Nested
    @DisplayName("setGlobalSlot(userId, slot, questId, questName, nodeId, title)")
    class SetGlobalSlot {

        @Test
        @DisplayName("creates state and sets slot when called then retrievable")
        void createsAndSets() {
            // Given / When
            sut.setGlobalSlot("u3", 0, "q", "Q", 7, "T");
            // Then
            SaveStateService.GlobalSlot gs = sut.getGlobalSlot("u3", 0).orElseThrow();
            assertEquals(7, gs.nodeId());
            assertEquals("q", gs.questId());
            assertEquals("Q", gs.questName());
            assertEquals("T", gs.title());
        }

        @Test
        @DisplayName("questId null becomes MAIN key then 'main'")
        void questIdNullBecomesMain() {
            // Given / When
            sut.setGlobalSlot("u4", 0, null, "Quest", 1, "ttl");
            // Then
            SaveStateService.GlobalSlot gs = sut.getGlobalSlot("u4", 0).orElseThrow();
            assertEquals(SaveState.MAIN_QUEST_KEY, gs.questId());
        }

        @Test
        @DisplayName("throws on slot out of bounds then IAE")
        void throwsOnOutOfBounds() {
            // Given / When / Then
            assertThrows(IllegalArgumentException.class,
                    () -> sut.setGlobalSlot("u5", -1, "q", "Q", 1, "t"));
            assertThrows(IllegalArgumentException.class,
                    () -> sut.setGlobalSlot("u5", 10, "q", "Q", 1, "t"));
        }
    }

    @Nested
    @DisplayName("clearGlobalSlot(userId, slot)")
    class ClearGlobalSlot {

        @Test
        @DisplayName("no-op when user missing then no throw")
        void noopWhenNoUser() {
            // Given / When / Then
            assertDoesNotThrow(() -> sut.clearGlobalSlot("none", 0));
        }

        @Test
        @DisplayName("clears existing slot then getGlobalSlot empty")
        void clearsExisting() {
            // Given
            sut.setGlobalSlot("u6", 2, "q", "Q", 10, "T");
            assertTrue(sut.getGlobalSlot("u6", 2).isPresent());
            // When
            sut.clearGlobalSlot("u6", 2);
            // Then
            assertTrue(sut.getGlobalSlot("u6", 2).isEmpty());
        }

        @Test
        @DisplayName("out-of-range clear is no-op then other slots preserved")
        void clearOutOfRangeNoop() {
            // Given
            sut.setGlobalSlot("u7", 1, "q", "Q", 5, "T");
            // When
            sut.clearGlobalSlot("u7", 99);
            // Then
            assertTrue(sut.getGlobalSlot("u7", 1).isPresent());
        }
    }
}