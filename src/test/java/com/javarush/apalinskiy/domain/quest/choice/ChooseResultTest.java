package com.javarush.apalinskiy.domain.quest.choice;

import com.javarush.apalinskiy.domain.quest.QuestNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ChooseResult")
class ChooseResultTest {

    @Nested
    @DisplayName("ok()")
    class OkFactory {

        @Test
        @DisplayName("creates result with ok=true when node provided then holds node")
        void createsOkWithNode() {
            // Given
            QuestNode node = QuestNode.of(1, "text", List.of(), false, "img.png");
            // When
            ChooseResult res = ChooseResult.ok(node);
            // Then
            assertTrue(res.isOk());
            assertSame(node, res.getNext());
        }

        @Test
        @DisplayName("sets error and message to null when ok then nulls")
        void okSetsNullErrorAndMessage() {
            // Given
            QuestNode node = QuestNode.of(2, "other", List.of(), true, null);
            // When
            ChooseResult res = ChooseResult.ok(node);
            // Then
            assertNull(res.getError());
            assertNull(res.getMessage());
        }
    }

    @Nested
    @DisplayName("error()")
    class ErrorFactory {

        @Test
        @DisplayName("creates result with ok=false when NODE_NOT_FOUND then has error and message")
        void createsErrorNodeNotFound() {
            // Given
            ChoiceError err = ChoiceError.NODE_NOT_FOUND;
            String msg = "Node not found";
            // When
            ChooseResult res = ChooseResult.error(err, msg);
            // Then
            assertFalse(res.isOk());
            assertEquals(err, res.getError());
            assertEquals(msg, res.getMessage());
        }

        @Test
        @DisplayName("creates result with ok=false when FINAL_NODE then has error and message")
        void createsErrorFinalNode() {
            // Given
            ChoiceError err = ChoiceError.FINAL_NODE;
            String msg = "Final node reached";
            // When
            ChooseResult res = ChooseResult.error(err, msg);
            // Then
            assertFalse(res.isOk());
            assertEquals(err, res.getError());
            assertEquals(msg, res.getMessage());
        }

        @Test
        @DisplayName("creates result with ok=false when EMPTY_ANSWER then has error and message")
        void createsErrorEmptyAnswer() {
            // Given
            ChoiceError err = ChoiceError.EMPTY_ANSWER;
            String msg = "Answer cannot be empty";
            // When
            ChooseResult res = ChooseResult.error(err, msg);
            // Then
            assertFalse(res.isOk());
            assertEquals(err, res.getError());
            assertEquals(msg, res.getMessage());
        }

        @Test
        @DisplayName("creates result with ok=false when NO_SUCH_OPTION then has error and message")
        void createsErrorNoSuchOption() {
            // Given
            ChoiceError err = ChoiceError.NO_SUCH_OPTION;
            String msg = "No such option";
            // When
            ChooseResult res = ChooseResult.error(err, msg);
            // Then
            assertFalse(res.isOk());
            assertEquals(err, res.getError());
            assertEquals(msg, res.getMessage());
        }

        @Test
        @DisplayName("sets next=null when error then no node")
        void errorSetsNextNull() {
            // Given
            ChoiceError err = ChoiceError.NODE_NOT_FOUND;
            // When
            ChooseResult res = ChooseResult.error(err, "msg");
            // Then
            assertNull(res.getNext());
        }
    }
}