package com.javarush.apalinskiy.repository.json;

import com.javarush.apalinskiy.domain.quest.QuestNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("QuestJsonReader")
class QuestJsonReaderTest {

    @Test
    @DisplayName("reads valid array then returns parsed nodes")
    void readsValidArray() throws Exception {
        // Given
        String json = """
                [
                  {"id":1,"text":"start","options":[{"choice":"go","next":2}],"final":false},
                  {"id":2,"text":"end","options":[],"final":true}
                ]
                """;
        QuestJsonReader reader = new QuestJsonReader();
        // When
        List<QuestNode> list = reader.read(new StringReader(json));
        // Then
        assertEquals(2, list.size());
        assertEquals(1, list.getFirst().getId());
        assertEquals("start", list.getFirst().getText());
        assertEquals(1, list.get(0).getOptions().size());
        assertEquals(2, list.get(0).getOptions().getFirst().getNext());
        assertTrue(list.get(1).getFin());
    }

    @Test
    @DisplayName("ignores unknown props then parses successfully")
    void ignoresUnknownProps() throws Exception {
        // Given
        String json = """
                [
                  {"id":1,"text":"start","options":[{"choice":"go","next":2,"x":1}],"final":false,"foo":"bar"},
                  {"id":2,"text":"end","options":[],"final":true,"extra":true}
                ]
                """;
        QuestJsonReader reader = new QuestJsonReader();
        // When
        List<QuestNode> list = reader.read(new StringReader(json));
        // Then
        assertEquals(2, list.size());
        assertEquals(1, list.get(0).getId());
        assertEquals(2, list.get(1).getId());
    }

    @Test
    @DisplayName("throws IOException on malformed JSON")
    void throwsOnMalformedJson() {
        // Given
        QuestJsonReader reader = new QuestJsonReader();
        // When / Then
        assertThrows(IOException.class, () -> reader.read(new StringReader("not a json")));
    }

    @Test
    @DisplayName("throws when node violates validation (final with options)")
    void throwsOnValidationError() {
        // Given
        String json = """
                [
                  {"id":2,"text":"end","options":[{"choice":"x","next":1}],"final":true}
                ]
                """;
        QuestJsonReader reader = new QuestJsonReader();
        // When / Then
        assertThrows(IOException.class, () -> reader.read(new StringReader(json)));
    }
}