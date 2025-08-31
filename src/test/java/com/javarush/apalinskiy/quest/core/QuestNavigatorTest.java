package com.javarush.apalinskiy.quest.core;

import com.javarush.apalinskiy.quest.model.Option;
import com.javarush.apalinskiy.quest.model.QuestNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestNavigatorTest {
    private QuestNode n1;
    private QuestNode n2;

    @BeforeEach
    void setUp() {
        n1 = QuestNode.of(1, "Start", List.of(
                new Option("Go north", 2)
        ), false, null);
        n2 = QuestNode.of(2, "Second", List.of(), true, null);
    }

    @Nested
    class FactoryFromAndConstructorGuards {

        @Test
        void fromNullNodesThrowsNpeTest() {
            // Given
            // When / Then
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> QuestNavigator.from(null, 1));
            assertEquals("nodes", ex.getMessage());
        }

        @Test
        void fromMissingStartIdThrowsIllegalArgumentTest() {
            // Given
            List<QuestNode> nodes = List.of(n1, n2);
            // When / Then
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> QuestNavigator.from(nodes, 99));
            assertEquals("Node not found: id=99", ex.getMessage());
        }

        @Test
        void fromBrokenLinkThrowsIllegalStateTest() {
            // Given
            QuestNode bad = QuestNode.of(10, "Has bad link", List.of(
                    new Option("Open", 999)
            ), false, null);
            List<QuestNode> nodes = List.of(n1, n2, bad);
            // When / Then
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> QuestNavigator.from(nodes, 1));
            assertEquals("Broken link: #10 -> #999", ex.getMessage());
        }

        @Test
        void fromAllowsOptionWithNullNextAndBuildsNavigatorTest() {
            // Given
            QuestNode mocked = mock(QuestNode.class);
            when(mocked.getId()).thenReturn(20);
            when(mocked.getOptions()).thenReturn(List.of(
                    new Option("   \t  ", null),
                    new Option("Continue", 2)
            ));
            List<QuestNode> nodes = List.of(n1, n2, mocked);
            // When
            QuestNavigator nav = QuestNavigator.from(nodes, 1);
            // Then
            assertEquals(1, nav.start().getId());
            assertTrue(nav.choose(1, "go north").isPresent());
            assertEquals(2, nav.choose(20, "continue").orElseThrow().getId());
        }

        @Test
        void fromBuildsNavigatorSuccessfullyTest() {
            // Given
            List<QuestNode> nodes = List.of(n1, n2);
            // When
            QuestNavigator nav = QuestNavigator.from(nodes, 1);
            // Then
            assertNotNull(nav);
            assertEquals(1, nav.get(1).getId());
            assertEquals(2, nav.get(2).getId());
        }
    }

    @Nested
    class Accessors {

        @Test
        void getReturnsNodeTest() {
            // Given
            QuestNavigator nav = QuestNavigator.from(List.of(n1, n2), 1);
            // When
            QuestNode got = nav.get(2);
            // Then
            assertEquals(n2, got);
        }

        @Test
        void startReturnsStartNodeTest() {
            // Given
            QuestNavigator nav = QuestNavigator.from(List.of(n1, n2), 1);
            // When
            QuestNode start = nav.start();
            // Then
            assertEquals(n1, start);
        }
    }

    @Nested
    class ChooseMethod {

        @Test
        void chooseReturnsNextNodeForValidAnswerTest() {
            // Given
            QuestNavigator nav = QuestNavigator.from(List.of(n1, n2), 1);
            // When
            Optional<QuestNode> next = nav.choose(1, "  GO   north  ");
            // Then
            assertTrue(next.isPresent());
            assertEquals(2, next.get().getId());
        }

        @Test
        void chooseReturnsEmptyWhenNoSuchAnswerTest() {
            // Given
            QuestNavigator nav = QuestNavigator.from(List.of(n1, n2), 1);
            // When
            Optional<QuestNode> next = nav.choose(1, "does not exist");
            // Then
            assertTrue(next.isEmpty());
        }

        @Test
        void chooseReturnsEmptyIfChoicePointsToMissingIdViaMockedIndexTest() {
            // Given
            QuestIdIndex idIndex = mock(QuestIdIndex.class);
            QuestChoiceIndex choiceIndex = mock(QuestChoiceIndex.class);
            when(idIndex.require(1)).thenReturn(n1);
            when(idIndex.get(123)).thenReturn(null);
            when(choiceIndex.nextId(1, "any")).thenReturn(123);
            QuestNavigator nav = createNavigatorReflectively(idIndex, choiceIndex);
            // When
            Optional<QuestNode> next = Objects.requireNonNull(nav).choose(1, "any");
            // Then
            assertTrue(next.isEmpty());
        }

        private QuestNavigator createNavigatorReflectively(QuestIdIndex idIdx, QuestChoiceIndex chIdx) {
            try {
                var ctor = QuestNavigator.class.getDeclaredConstructor(QuestIdIndex.class, QuestChoiceIndex.class, int.class);
                ctor.setAccessible(true);
                return ctor.newInstance(idIdx, chIdx, 1);
            } catch (Exception e) {
                fail("Failed to construct QuestNavigator reflectively: " + e);
                return null;
            }
        }
    }

}