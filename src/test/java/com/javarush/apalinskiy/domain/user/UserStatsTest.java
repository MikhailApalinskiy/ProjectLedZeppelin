package com.javarush.apalinskiy.domain.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserStats")
class UserStatsTest {

    @Nested
    @DisplayName("constructor")
    class Ctor {

        @Test
        @DisplayName("sets fields when positive values then getters return them")
        void setsPositiveValues() {
            // Given
            long created = 5, completed = 12, endings = 3;
            // When
            UserStats s = new UserStats(created, completed, endings);
            // Then
            assertEquals(5, s.getQuestsCreated());
            assertEquals(12, s.getQuestsCompleted());
            assertEquals(3, s.getEndingsUnlocked());
        }

        @Test
        @DisplayName("accepts zeros when given then getters return zeros")
        void acceptsZeros() {
            // Given
            long created = 0, completed = 0, endings = 0;
            // When
            UserStats s = new UserStats(created, completed, endings);
            // Then
            assertEquals(0, s.getQuestsCreated());
            assertEquals(0, s.getQuestsCompleted());
            assertEquals(0, s.getEndingsUnlocked());
        }

        @Test
        @DisplayName("accepts negatives (no validation) when given then getters return negatives")
        void acceptsNegatives() {
            // Given
            long created = -1, completed = -2, endings = -3;
            // When
            UserStats s = new UserStats(created, completed, endings);
            // Then
            assertEquals(-1, s.getQuestsCreated());
            assertEquals(-2, s.getQuestsCompleted());
            assertEquals(-3, s.getEndingsUnlocked());
        }

        @Test
        @DisplayName("supports large values when Long.MAX_VALUE then ok")
        void supportsLargeValues() {
            // Given
            long big = Long.MAX_VALUE;
            // When
            UserStats s = new UserStats(big, big - 1, big - 2);
            // Then
            assertEquals(Long.MAX_VALUE, s.getQuestsCreated());
            assertEquals(Long.MAX_VALUE - 1, s.getQuestsCompleted());
            assertEquals(Long.MAX_VALUE - 2, s.getEndingsUnlocked());
        }
    }
}