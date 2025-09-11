package com.javarush.apalinskiy.service.impl.quest;

import com.javarush.apalinskiy.domain.quest.Option;
import com.javarush.apalinskiy.domain.quest.QuestNode;
import com.javarush.apalinskiy.domain.quest.choice.ChoiceError;
import com.javarush.apalinskiy.domain.quest.choice.ChooseResult;
import com.javarush.apalinskiy.repository.quest.QuestStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultQuestService")
class DefaultQuestServiceTest {

    @Mock
    QuestStore store;

    DefaultQuestService sut;

    @BeforeEach
    void setUp() {
        sut = new DefaultQuestService(store);
    }

    private QuestNode nf(int id) {
        return QuestNode.nonFin(id, "n" + id, List.of(new Option("go", id + 1)), null);
    }

    private QuestNode fin(int id) {
        return QuestNode.fin(id, "end" + id, null);
    }

    @Nested
    @DisplayName("ctor")
    class Ctor {
        @Test
        @DisplayName("throws NPE when store is null")
        void throwsOnNullStore() {
            // Given / When / Then
            assertThrows(NullPointerException.class, () -> new DefaultQuestService(null));
        }
    }

    @Nested
    @DisplayName("delegation")
    class Delegation {
        @Test
        @DisplayName("getStart delegates to store.start")
        void getStartDelegates() {
            // Given
            when(store.start()).thenReturn(fin(2));
            // When
            QuestNode got = sut.getStart();
            // Then
            assertEquals(2, got.getId());
            verify(store, times(1)).start();
        }

        @Test
        @DisplayName("getById delegates to store.get")
        void getByIdDelegates() {
            // Given
            when(store.get(42)).thenReturn(nf(42));
            // When
            QuestNode got = sut.getById(42);
            // Then
            assertNotNull(got);
            assertEquals(42, got.getId());
            verify(store).get(42);
        }

        @Test
        @DisplayName("version delegates to store.version")
        void versionDelegates() {
            // Given
            when(store.version()).thenReturn("v1");
            // When / Then
            assertEquals("v1", sut.version());
            verify(store).version();
        }
    }

    @Nested
    @DisplayName("choose (through AbstractQuestService)")
    class ChooseFlow {

        @Test
        @DisplayName("returns NODE_NOT_FOUND when store.get returns null")
        void nodeNotFound() {
            // Given
            when(store.get(5)).thenReturn(null);
            // When
            ChooseResult r = sut.choose(5, "go");
            // Then
            assertFalse(r.isOk());
            assertEquals(ChoiceError.NODE_NOT_FOUND, r.getError());
        }

        @Test
        @DisplayName("returns FINAL_NODE when from is final")
        void finalNode() {
            // Given
            when(store.get(1)).thenReturn(fin(1));
            // When
            ChooseResult r = sut.choose(1, "any");
            // Then
            assertFalse(r.isOk());
            assertEquals(ChoiceError.FINAL_NODE, r.getError());
        }

        @Test
        @DisplayName("unwraps Optional: present -> OK with that next")
        void unwrapsPresent() {
            // Given
            QuestNode from = nf(1);
            QuestNode next = fin(2);
            when(store.get(1)).thenReturn(from);
            when(store.choose(1, "go")).thenReturn(Optional.of(next));
            // When
            ChooseResult r = sut.choose(1, "go");
            // Then
            assertTrue(r.isOk());
            assertEquals(2, r.getNext().getId());
            verify(store).choose(1, "go");
        }

        @Test
        @DisplayName("unwraps Optional: empty -> NO_SUCH_OPTION")
        void unwrapsEmpty() {
            // Given
            when(store.get(1)).thenReturn(nf(1));
            when(store.choose(1, "left")).thenReturn(Optional.empty());
            // When
            ChooseResult r = sut.choose(1, "left");
            // Then
            assertFalse(r.isOk());
            assertEquals(ChoiceError.NO_SUCH_OPTION, r.getError());
            verify(store).choose(1, "left");
        }
    }
}