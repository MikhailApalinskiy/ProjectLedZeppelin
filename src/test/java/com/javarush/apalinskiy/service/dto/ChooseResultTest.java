package com.javarush.apalinskiy.service.dto;

import com.javarush.apalinskiy.quest.model.QuestNode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class ChooseResultTest {

    private static QuestNode finNode(int id, String text) {
        return QuestNode.fin(id, text, null);
    }

    @Nested
    class Factories {

        @Test
        void okFactorySetsFieldsTest() {
            // Given
            QuestNode next = finNode(2, "End");
            // When
            ChooseResult r = ChooseResult.ok(next);
            // Then
            assertTrue(r.isOk());
            assertSame(next, r.getNext());
            assertNull(r.getError());
            assertNull(r.getMessage());
        }

        @Test
        void okFactoryAllowsNullNextTest() {
            // Given
            // When
            ChooseResult r = ChooseResult.ok(null);
            // Then
            assertTrue(r.isOk());
            assertNull(r.getNext());
            assertNull(r.getError());
            assertNull(r.getMessage());
        }

        @Test
        void errorFactorySetsFieldsTest() {
            // Given
            String msg = "Invalid answer";
            // When
            ChooseResult r = ChooseResult.error(null, msg);
            // Then
            assertFalse(r.isOk());
            assertNull(r.getNext());
            assertNull(r.getError());
            assertEquals("Invalid answer", r.getMessage());
        }

        @Test
        void errorFactoryAllowsNullsTest() {
            // Given
            // When
            ChooseResult r = ChooseResult.error(null, null);
            // Then
            assertFalse(r.isOk());
            assertNull(r.getNext());
            assertNull(r.getError());
            assertNull(r.getMessage());
        }
    }

    @Nested
    class Getters {

        @Test
        void gettersReturnValuesForOkCaseTest() {
            // Given
            QuestNode next = finNode(5, "Done");
            ChooseResult r = ChooseResult.ok(next);
            // When / Then
            assertTrue(r.isOk());
            assertSame(next, r.getNext());
            assertNull(r.getError());
            assertNull(r.getMessage());
        }

        @Test
        void gettersReturnValuesForErrorCaseTest() {
            // Given
            ChooseResult r = ChooseResult.error(null, "Oops");
            // When / Then
            assertFalse(r.isOk());
            assertNull(r.getNext());
            assertNull(r.getError());
            assertEquals("Oops", r.getMessage());
        }
    }

    @Nested
    class StructuralProperties {

        @Test
        void classIsFinalTest() {
            // Given / When
            int m = ChooseResult.class.getModifiers();
            // Then
            assertTrue(Modifier.isFinal(m), "ChooseResult must be final");
        }

        @Test
        void allFieldsAreFinalTest() {
            // Given / When
            Field[] fields = ChooseResult.class.getDeclaredFields();
            // Then
            for (Field f : fields) {
                if (f.isSynthetic()) continue;
                assertTrue(Modifier.isFinal(f.getModifiers()),
                        "Field must be final: " + f.getName());
            }
        }
    }

}