package com.javarush.apalinskiy.domain.save;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GlobalSlotId")
class GlobalSlotIdTest {

    @Nested
    @DisplayName("constructors")
    class Constructors {

        @Test
        @DisplayName("no-args constructor creates empty instance")
        void createsEmptyInstance() {
            // Given / When
            GlobalSlotId id = new GlobalSlotId();
            // Then
            assertNull(id.getUserId());
            assertEquals(0, id.getSlotIndex());
        }

        @Test
        @DisplayName("parameterized constructor sets fields correctly")
        void setsFieldsViaConstructor() {
            // Given
            String userId = "user-123";
            int slotIndex = 7;
            // When
            GlobalSlotId id = new GlobalSlotId(userId, slotIndex);
            // Then
            assertEquals(userId, id.getUserId());
            assertEquals(slotIndex, id.getSlotIndex());
        }
    }

    @Nested
    @DisplayName("equals() and hashCode()")
    class Equality {

        @Test
        @DisplayName("two identical IDs are equal and have same hashCode")
        void identicalIdsEqual() {
            // Given
            GlobalSlotId a = new GlobalSlotId("u1", 1);
            GlobalSlotId b = new GlobalSlotId("u1", 1);
            // When / Then
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("different userIds produce inequality")
        void differentUserIdsNotEqual() {
            // Given
            GlobalSlotId a = new GlobalSlotId("userA", 1);
            GlobalSlotId b = new GlobalSlotId("userB", 1);
            // When / Then
            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("different slotIndex produce inequality")
        void differentSlotIndexNotEqual() {
            // Given
            GlobalSlotId a = new GlobalSlotId("userA", 1);
            GlobalSlotId b = new GlobalSlotId("userA", 2);
            // When / Then
            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("equals() handles null and different class")
        void equalsHandlesNullAndDifferentClass() {
            // Given
            GlobalSlotId a = new GlobalSlotId("userA", 1);
            // When / Then
            assertNotEquals(null, a);
            assertNotEquals("some string", a);
        }

        @Test
        @DisplayName("reflexivity and symmetry hold")
        void reflexiveAndSymmetric() {
            // Given
            GlobalSlotId a = new GlobalSlotId("userA", 1);
            GlobalSlotId b = new GlobalSlotId("userA", 1);
            // When / Then
            assertEquals(a, a);
            assertTrue(a.equals(b) && b.equals(a));
        }
    }
}