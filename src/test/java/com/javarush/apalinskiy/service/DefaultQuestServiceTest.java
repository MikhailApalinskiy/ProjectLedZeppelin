package com.javarush.apalinskiy.service;

import static org.junit.jupiter.api.Assertions.*;

import com.javarush.apalinskiy.quest.model.Option;
import com.javarush.apalinskiy.quest.model.QuestNode;
import com.javarush.apalinskiy.repositories.QuestRepository;
import com.javarush.apalinskiy.service.dto.ChoiceError;
import com.javarush.apalinskiy.service.dto.ChooseResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultQuestServiceTest {

    @Mock
    private QuestRepository repo;
    private DefaultQuestService service;
    private QuestNode startNode;
    private QuestNode finalNode;

    @BeforeEach
    void setUp() {
        startNode = QuestNode.of(1, "Start", List.of(new Option("go", 2)), false, null);
        finalNode = QuestNode.fin(2, "End", null);
        service = new DefaultQuestService(repo);
    }

    @Nested
    class ConstructorValidation {

        @Test
        void nullRepoThrowsNpeTest() {
            // Given / When / Then
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> new DefaultQuestService(null));
            assertNull(ex.getMessage());
        }
    }

    @Nested
    class Delegation {

        @Test
        void getStartDelegatesToRepoTest() {
            // Given
            when(repo.start()).thenReturn(startNode);
            // When
            QuestNode n = service.getStart();
            // Then
            assertSame(startNode, n);
            verify(repo).start();
            verifyNoMoreInteractions(repo);
        }

        @Test
        void getByIdDelegatesToRepoAndReturnsValueTest() {
            // Given
            when(repo.get(2)).thenReturn(finalNode);
            // When
            QuestNode n = service.getById(2);
            // Then
            assertSame(finalNode, n);
            verify(repo).get(2);
            verifyNoMoreInteractions(repo);
        }

        @Test
        void getByIdDelegatesToRepoAndReturnsNullTest() {
            // Given
            when(repo.get(999)).thenReturn(null);
            // When
            QuestNode n = service.getById(999);
            // Then
            assertNull(n);
            verify(repo).get(999);
            verifyNoMoreInteractions(repo);
        }

        @Test
        void versionDelegatesToRepoTest() {
            // Given
            when(repo.version()).thenReturn("sha256:abc");
            // When
            String v = service.version();
            // Then
            assertEquals("sha256:abc", v);
            verify(repo).version();
            verifyNoMoreInteractions(repo);
        }
    }

    @Nested
    class ChooseMethod {

        @Test
        void returnsNodeNotFoundWhenFromNodeIsMissingTest() {
            // Given
            when(repo.get(77)).thenReturn(null);
            // When
            ChooseResult r = service.choose(77, "anything");
            // Then
            assertFalse(r.isOk());
            assertEquals(ChoiceError.NODE_NOT_FOUND, r.getError());
            assertEquals("Узел #77 не найден", r.getMessage());
            assertNull(r.getNext());
            verify(repo).get(77);
            verifyNoMoreInteractions(repo);
        }

        @Test
        void returnsFinalNodeErrorWhenFromIsFinalTest() {
            // Given
            when(repo.get(2)).thenReturn(finalNode);
            // When
            ChooseResult r = service.choose(2, "go");
            // Then
            assertFalse(r.isOk());
            assertEquals(ChoiceError.FINAL_NODE, r.getError());
            assertEquals("Это финальная ветка", r.getMessage());
            assertNull(r.getNext());
            verify(repo).get(2);
            verifyNoMoreInteractions(repo);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "\t\n"})
        void returnsEmptyAnswerErrorWhenAnswerBlankTest(String blank) {
            // Given
            when(repo.get(1)).thenReturn(startNode);
            // When
            ChooseResult r = service.choose(1, blank);
            // Then
            assertFalse(r.isOk());
            assertEquals(ChoiceError.EMPTY_ANSWER, r.getError());
            assertEquals("Пустой ответ", r.getMessage());
            assertNull(r.getNext());
            verify(repo).get(1);
            verifyNoMoreInteractions(repo);
        }

        @Test
        void returnsEmptyAnswerErrorWhenAnswerNullTest() {
            // Given
            when(repo.get(1)).thenReturn(startNode);
            // When
            ChooseResult r = service.choose(1, null);
            // Then
            assertFalse(r.isOk());
            assertEquals(ChoiceError.EMPTY_ANSWER, r.getError());
            assertEquals("Пустой ответ", r.getMessage());
            assertNull(r.getNext());
            verify(repo).get(1);
            verifyNoMoreInteractions(repo);
        }

        @Test
        void returnsOkWhenRepositoryFindsNextTest() {
            // Given
            when(repo.get(1)).thenReturn(startNode);
            when(repo.choose(1, "go")).thenReturn(Optional.of(finalNode));
            // When
            ChooseResult r = service.choose(1, "go");
            // Then
            assertTrue(r.isOk());
            assertSame(finalNode, r.getNext());
            assertNull(r.getError());
            assertNull(r.getMessage());
            verify(repo).get(1);
            verify(repo).choose(1, "go");
            verifyNoMoreInteractions(repo);
        }

        @Test
        void returnsNoSuchOptionWhenRepositoryReturnsEmptyTest() {
            // Given
            when(repo.get(1)).thenReturn(startNode);
            when(repo.choose(1, "nope")).thenReturn(Optional.empty());
            // When
            ChooseResult r = service.choose(1, "nope");
            // Then
            assertFalse(r.isOk());
            assertEquals(ChoiceError.NO_SUCH_OPTION, r.getError());
            assertEquals("Нет такого варианта ответа", r.getMessage());
            assertNull(r.getNext());
            verify(repo).get(1);
            verify(repo).choose(1, "nope");
            verifyNoMoreInteractions(repo);
        }
    }
}