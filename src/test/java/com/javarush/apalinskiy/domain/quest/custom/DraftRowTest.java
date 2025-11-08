package com.javarush.apalinskiy.domain.quest.custom;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DraftRow entity")
class DraftRowTest {

    @Nested
    @DisplayName("default state")
    class DefaultState {

        @Test
        @DisplayName("should initialize with nulls and default version note")
        void initializesWithDefaults() {
            // Given
            // When
            DraftRow row = new DraftRow();
            // Then
            assertNull(row.getDraftId());
            assertNull(row.getOwnerId());
            assertNull(row.getTargetQuestId());
            assertNull(row.getName());
            assertNull(row.getStartId());
            assertNull(row.getNodesJson());
            assertEquals("", row.getVersionNote());
            assertNull(row.getUpdatedAt());
        }
    }

    @Nested
    @DisplayName("field assignment")
    class FieldAssignment {

        @Test
        @DisplayName("should correctly set and retrieve all fields")
        void setsAndGetsAllFields() {
            // Given
            String draftId = UUID.randomUUID().toString();
            String ownerId = UUID.randomUUID().toString();
            String targetId = UUID.randomUUID().toString();
            String name = "My Quest";
            Integer startId = 7;
            String json = "{\"nodes\":[]}";
            String version = "v1.0";
            Instant now = Instant.now();
            // When
            DraftRow row = new DraftRow();
            row.setDraftId(draftId);
            row.setOwnerId(ownerId);
            row.setTargetQuestId(targetId);
            row.setName(name);
            row.setStartId(startId);
            row.setNodesJson(json);
            row.setVersionNote(version);
            row.setUpdatedAt(now);
            // Then
            assertEquals(draftId, row.getDraftId());
            assertEquals(ownerId, row.getOwnerId());
            assertEquals(targetId, row.getTargetQuestId());
            assertEquals(name, row.getName());
            assertEquals(startId, row.getStartId());
            assertEquals(json, row.getNodesJson());
            assertEquals(version, row.getVersionNote());
            assertEquals(now, row.getUpdatedAt());
        }
    }

    @Nested
    @DisplayName("consistency rules")
    class Consistency {

        @Test
        @DisplayName("should allow nullable targetQuestId")
        void allowsNullTargetQuestId() {
            // Given
            DraftRow row = new DraftRow();
            row.setTargetQuestId(null);
            // When
            String result = row.getTargetQuestId();
            // Then
            assertNull(result);
        }

        @Test
        @DisplayName("should not modify versionNote default when not set")
        void keepsDefaultVersionNote() {
            // Given
            DraftRow row = new DraftRow();
            // When
            String version = row.getVersionNote();
            // Then
            assertEquals("", version);
        }

        @Test
        @DisplayName("should accept and store empty JSON string")
        void acceptsEmptyJson() {
            // Given
            DraftRow row = new DraftRow();
            // When
            row.setNodesJson("");
            // Then
            assertEquals("", row.getNodesJson());
        }
    }

    @Nested
    @DisplayName("updatedAt field")
    class UpdatedAtField {

        @Test
        @DisplayName("should correctly store timestamp")
        void storesTimestamp() {
            // Given
            Instant now = Instant.now();
            // When
            DraftRow row = new DraftRow();
            row.setUpdatedAt(now);
            // Then
            assertEquals(now, row.getUpdatedAt());
        }

        @Test
        @DisplayName("should allow updatedAt to be null")
        void allowsNullUpdatedAt() {
            // Given
            DraftRow row = new DraftRow();
            // When
            row.setUpdatedAt(null);
            // Then
            assertNull(row.getUpdatedAt());
        }
    }
}