package com.javarush.apalinskiy.web.view;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SlotView")
class SlotViewTest {

    @Nested
    @DisplayName("empty factory")
    class EmptyFactory {

        @Test
        @DisplayName("given index and quest info when empty then nodeId,title,updatedAtText are null")
        void emptyHasNulls() {
            // given
            int index = 5;
            String questId = "q1";
            String questName = "Quest";
            // when
            SlotView sv = SlotView.empty(index, questId, questName);
            // then
            assertEquals(index, sv.getIndex());
            assertNull(sv.getNodeId());
            assertNull(sv.getTitle());
            assertNull(sv.getUpdatedAtText());
            assertEquals(questId, sv.getQuestId());
            assertEquals(questName, sv.getQuestName());
        }

        @Test
        @DisplayName("given null quest info when empty then questId and questName are null")
        void emptyWithNullQuestInfo() {
            // when
            SlotView sv = SlotView.empty(0, null, null);
            // then
            assertEquals(0, sv.getIndex());
            assertNull(sv.getQuestId());
            assertNull(sv.getQuestName());
        }
    }

    @Nested
    @DisplayName("filled factory")
    class FilledFactory {

        @Test
        @DisplayName("given all fields when filled then preserved")
        void filledHasAllFields() {
            // given
            int index = 2;
            int nodeId = 42;
            String title = "Title";
            String updatedAtText = "2025-09-11";
            String questId = "q2";
            String questName = "QuestName";
            // when
            SlotView sv = SlotView.filled(index, nodeId, title, updatedAtText, questId, questName);
            // then
            assertEquals(index, sv.getIndex());
            assertEquals(nodeId, sv.getNodeId());
            assertEquals(title, sv.getTitle());
            assertEquals(updatedAtText, sv.getUpdatedAtText());
            assertEquals(questId, sv.getQuestId());
            assertEquals(questName, sv.getQuestName());
        }

        @Test
        @DisplayName("given null title when filled then allowed")
        void filledWithNullTitle() {
            // when
            SlotView sv = SlotView.filled(-1, 99, null, null, null, null);
            // then
            assertEquals(-1, sv.getIndex());
            assertEquals(99, sv.getNodeId());
            assertNull(sv.getTitle());
            assertNull(sv.getUpdatedAtText());
            assertNull(sv.getQuestId());
            assertNull(sv.getQuestName());
        }
    }
}