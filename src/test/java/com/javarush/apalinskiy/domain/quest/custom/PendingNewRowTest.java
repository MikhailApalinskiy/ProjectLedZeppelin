package com.javarush.apalinskiy.domain.quest.custom;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PendingNewRow entity")
class PendingNewRowTest {

    @Nested
    @DisplayName("default state")
    class DefaultState {

        @Test
        @DisplayName("should initialize all fields as null")
        void initializesWithNulls() {
            // Given
            // When
            PendingNewRow row = new PendingNewRow();
            // Then
            assertNull(row.getPendingId());
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
        @DisplayName("should correctly set and get all fields")
        void setsAndGetsAllFields() {
            // Given
            String pendingId = UUID.randomUUID().toString();
            String ownerId = UUID.randomUUID().toString();
            String name = "Quest Submission";
            Integer startId = 10;
            String nodesJson = "{\"nodes\":[]}";
            String versionNote = "Initial draft";
            Instant submitted = Instant.now();
            // When
            PendingNewRow row = new PendingNewRow();
            row.setPendingId(pendingId);
            row.setOwnerId(ownerId);
            row.setName(name);
            row.setStartId(startId);
            row.setNodesJson(nodesJson);
            row.setVersionNote(versionNote);
            row.setSubmittedAt(submitted);
            // Then
            assertEquals(pendingId, row.getPendingId());
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
            PendingNewRow row = new PendingNewRow();
            // When
            row.setVersionNote("");
            // Then
            assertEquals("", row.getVersionNote());
        }

        @Test
        @DisplayName("should allow empty nodesJson")
        void allowsEmptyNodesJson() {
            // Given
            PendingNewRow row = new PendingNewRow();
            // When
            row.setNodesJson("");
            // Then
            assertEquals("", row.getNodesJson());
        }

        @Test
        @DisplayName("should accept null submittedAt")
        void allowsNullSubmittedAt() {
            // Given
            PendingNewRow row = new PendingNewRow();
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
        @DisplayName("should preserve identifiers as set")
        void preservesIdentifiers() {
            // Given
            String pid = UUID.randomUUID().toString();
            String oid = UUID.randomUUID().toString();
            // When
            PendingNewRow row = new PendingNewRow();
            row.setPendingId(pid);
            row.setOwnerId(oid);
            // Then
            assertEquals(pid, row.getPendingId());
            assertEquals(oid, row.getOwnerId());
        }

        @Test
        @DisplayName("should correctly update name field")
        void updatesName() {
            // Given
            PendingNewRow row = new PendingNewRow();
            // When
            row.setName("Quest A");
            String first = row.getName();
            row.setName("Quest B");
            String second = row.getName();
            // Then
            assertEquals("Quest A", first);
            assertEquals("Quest B", second);
        }

        @Test
        @DisplayName("should correctly store and retrieve submittedAt timestamp")
        void storesSubmittedAt() {
            // Given
            Instant now = Instant.now();
            // When
            PendingNewRow row = new PendingNewRow();
            row.setSubmittedAt(now);
            // Then
            assertEquals(now, row.getSubmittedAt());
        }
    }
}