package com.javarush.apalinskiy.service.impl.quest;

import com.javarush.apalinskiy.domain.quest.Option;
import com.javarush.apalinskiy.domain.quest.QuestNode;
import com.javarush.apalinskiy.domain.quest.choice.ChoiceError;
import com.javarush.apalinskiy.domain.quest.choice.ChooseResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AbstractQuestService.choose")
class AbstractQuestServiceTest {

    static class TestSvc extends AbstractQuestService {
        private final Map<Integer, QuestNode> byId = new HashMap<>();
        private QuestNode nextToReturn;

        @Override
        public QuestNode getStart() {
            return null;
        }

        @Override
        public QuestNode getById(int id) {
            return byId.get(id);
        }

        @Override
        public String version() {
            return "";
        }

        @Override
        protected QuestNode resolveNext(int fromId, String answer) {
            return nextToReturn;
        }

        void put(QuestNode n) {
            byId.put(n.getId(), n);
        }

        void next(QuestNode n) {
            nextToReturn = n;
        }
    }

    private static QuestNode fin(int id) {
        return QuestNode.fin(id, "end" + id, null);
    }

    private static QuestNode nonFin() {
        return QuestNode.nonFin(1, "n1", List.of(new Option("go", 2)), null);
    }

    TestSvc sut;

    @BeforeEach
    void setUp() {
        sut = new TestSvc();
    }

    @Nested
    @DisplayName("error paths")
    class Errors {
        @Test
        @DisplayName("returns NODE_NOT_FOUND when from node missing")
        void nodeNotFound() {
            // When
            ChooseResult r = sut.choose(99, "go");
            // Then
            assertFalse(r.isOk());
            assertEquals(ChoiceError.NODE_NOT_FOUND, r.getError());
        }

        @Test
        @DisplayName("returns FINAL_NODE when from node is final")
        void finalNode() {
            // Given
            sut.put(fin(1));
            // When
            ChooseResult r = sut.choose(1, "anything");
            // Then
            assertFalse(r.isOk());
            assertEquals(ChoiceError.FINAL_NODE, r.getError());
        }

        @Test
        @DisplayName("returns EMPTY_ANSWER when answer is null")
        void emptyAnswerNull() {
            // Given
            sut.put(nonFin());
            // When
            ChooseResult r = sut.choose(1, null);
            // Then
            assertFalse(r.isOk());
            assertEquals(ChoiceError.EMPTY_ANSWER, r.getError());
        }

        @Test
        @DisplayName("returns EMPTY_ANSWER when answer is blank")
        void emptyAnswerBlank() {
            // Given
            sut.put(nonFin());
            // When
            ChooseResult r = sut.choose(1, "   ");
            // Then
            assertFalse(r.isOk());
            assertEquals(ChoiceError.EMPTY_ANSWER, r.getError());
        }

        @Test
        @DisplayName("returns NO_SUCH_OPTION when resolveNext returns null")
        void noSuchOption() {
            // Given
            sut.put(nonFin());
            sut.next(null);
            // When
            ChooseResult r = sut.choose(1, "unknown");
            // Then
            assertFalse(r.isOk());
            assertEquals(ChoiceError.NO_SUCH_OPTION, r.getError());
        }
    }

    @Nested
    @DisplayName("success path")
    class Success {
        @Test
        @DisplayName("returns OK with next node when resolveNext returns non-null")
        void okWhenResolved() {
            // Given
            QuestNode from = nonFin();
            QuestNode next = fin(2);
            sut.put(from);
            sut.put(next);
            sut.next(next);
            // When
            ChooseResult r = sut.choose(1, "go");
            // Then
            assertTrue(r.isOk());
            assertEquals(next, r.getNext());
        }
    }
}