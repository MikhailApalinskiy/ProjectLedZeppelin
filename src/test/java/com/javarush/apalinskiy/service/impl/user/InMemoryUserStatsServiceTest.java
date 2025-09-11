package com.javarush.apalinskiy.service.impl.user;

import com.javarush.apalinskiy.domain.user.UserStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InMemoryUserStatsService")
class InMemoryUserStatsServiceTest {

    InMemoryUserStatsService sut;

    @BeforeEach
    void setUp() {
        sut = new InMemoryUserStatsService();
    }

    @Test
    @DisplayName("statsOf unknown user -> zeros")
    void statsOfUnknown() {
        // When
        UserStats s = sut.statsOf("ghost");
        // Then
        assertEquals(0, s.getQuestsCreated());
        assertEquals(0, s.getQuestsCompleted());
        assertEquals(0, s.getEndingsUnlocked());
    }

    @Nested
    @DisplayName("incCreated(userId)")
    class IncCreated {

        @Test
        @DisplayName("ignores null/blank userId -> no entry")
        void ignoresBadIds() {
            // When
            sut.incCreated(null);
            sut.incCreated("   ");
            // Then
            assertEquals(0, sut.statsOf("   ").getQuestsCreated());
        }

        @Test
        @DisplayName("increments created counter")
        void increments() {
            // When
            sut.incCreated("u");
            sut.incCreated("u");
            // Then
            assertEquals(2, sut.statsOf("u").getQuestsCreated());
        }
    }

    @Nested
    @DisplayName("onQuestCompleted(userId, questKey, finalNodeId)")
    class OnQuestCompleted {

        @Test
        @DisplayName("ignores null/blank userId")
        void ignoresBadIds() {
            // When
            sut.onQuestCompleted(null, "main", 1);
            sut.onQuestCompleted("   ", "main", 1);
            // Then
            assertEquals(0, sut.statsOf("   ").getQuestsCompleted());
        }

        @Test
        @DisplayName("increments completed always when valid user")
        void completedIncrements() {
            // When
            sut.onQuestCompleted("u", "x", null);
            sut.onQuestCompleted("u", "y", 10);
            sut.onQuestCompleted("u", "main", 1);
            // Then
            assertEquals(3, sut.statsOf("u").getQuestsCompleted());
        }

        @Test
        @DisplayName("endingsUnlocked counts unique finals only for questKey='main' and non-null finalNodeId")
        void endingsCountedForMainOnlyAndUnique() {
            // When
            sut.onQuestCompleted("u", "main", 10);
            sut.onQuestCompleted("u", "main", 10);
            sut.onQuestCompleted("u", "main", 11);
            sut.onQuestCompleted("u", "other", 99);
            sut.onQuestCompleted("u", "main", null);
            // Then
            UserStats s = sut.statsOf("u");
            assertEquals(5, s.getQuestsCompleted());
            assertEquals(2, s.getEndingsUnlocked());
        }
    }
}