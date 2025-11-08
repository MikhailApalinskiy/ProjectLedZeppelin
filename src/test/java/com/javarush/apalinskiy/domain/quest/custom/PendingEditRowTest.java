package com.javarush.apalinskiy.domain.quest.custom;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PendingEditRow entity")
class PendingEditRowTest {

    @Nested
    @DisplayName("default state")
    class DefaultState {

        @Test
        @DisplayName("should initialize with all fields null")
        void initializesWithNulls() {
            // Given
            // When
            PendingEditRow row = new PendingEditRow();
            // Then
            assertNull(row.getQuestId());
            assertNull(row.getOwnerId());
            assertNull(row.getName());
            assertNull(row.getStartId());
            assertNull(row.getNodesJson());
            assertNull(row.getVersionNote());
            assertNull(row.getSubmittedAt());
        }
    }

    @Nested
    @DisplayName("field assignment")
    class FieldAssignment {

        @Test
        @DisplayName("should correctly set and retrieve all fields")
        void setsAndGetsAllFields() {
            // Given
            String questId = UUID.randomUUID().toString();
            String ownerId = UUID.randomUUID().toString();
            String name = "Edited Quest";
            Integer startId = 99;
            String nodesJson = "{\"nodes\":[]}";
            String versionNote = "Minor text fixes";
            Instant submitted = Instant.now();
            // When
            PendingEditRow row = new PendingEditRow();
            row.setQuestId(questId);
            row.setOwnerId(ownerId);
            row.setName(name);
            row.setStartId(startId);
            row.setNodesJson(nodesJson);
            row.setVersionNote(versionNote);
            row.setSubmittedAt(submitted);
            // Then
            assertEquals(questId, row.getQuestId());
            assertEquals(ownerId, row.getOwnerId());
            assertEquals(name, row.getName());
            assertEquals(startId, row.getStartId());
            assertEquals(nodesJson, row.getNodesJson());
            assertEquals(versionNote, row.getVersionNote());
            assertEquals(submitted, row.getSubmittedAt());
        }
    }

    @Nested
    @DisplayName("consistency rules")
    class Consistency {

        @Test
        @DisplayName("should allow empty version note")
        void allowsEmptyVersionNote() {
            // Given
            PendingEditRow row = new PendingEditRow();
            // When
            row.setVersionNote("");
            // Then
            assertEquals("", row.getVersionNote());
        }

        @Test
        @DisplayName("should allow empty nodesJson")
        void allowsEmptyNodesJson() {
            // Given
            PendingEditRow row = new PendingEditRow();
            // When
            row.setNodesJson("");
            // Then
            assertEquals("", row.getNodesJson());
        }

        @Test
        @DisplayName("should accept null submittedAt")
        void allowsNullSubmittedAt() {
            // Given
            PendingEditRow row = new PendingEditRow();
            // When
            row.setSubmittedAt(null);
            // Then
            assertNull(row.getSubmittedAt());
        }
    }

    @Nested
    @DisplayName("data integrity")
    class DataIntegrity {

        @Test
        @DisplayName("should preserve questId and ownerId values as set")
        void preservesIdentifiers() {
            // Given
            String qid = UUID.randomUUID().toString();
            String oid = UUID.randomUUID().toString();
            // When
            PendingEditRow row = new PendingEditRow();
            row.setQuestId(qid);
            row.setOwnerId(oid);
            // Then
            assertEquals(qid, row.getQuestId());
            assertEquals(oid, row.getOwnerId());
        }

        @Test
        @DisplayName("should accept different name values")
        void acceptsDifferentNames() {
            // Given
            PendingEditRow row = new PendingEditRow();
            // When
            row.setName("Q1");
            String first = row.getName();
            row.setName("Q2");
            String second = row.getName();
            // Then
            assertEquals("Q1", first);
            assertEquals("Q2", second);
        }

        @Test
        @DisplayName("should correctly handle submittedAt timestamps")
        void handlesTimestamps() {
            // Given
            Instant time = Instant.now();
            // When
            PendingEditRow row = new PendingEditRow();
            row.setSubmittedAt(time);
            // Then
            assertEquals(time, row.getSubmittedAt());
        }
    }
}