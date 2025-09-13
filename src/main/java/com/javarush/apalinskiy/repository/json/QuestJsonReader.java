package com.javarush.apalinskiy.repository.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javarush.apalinskiy.domain.quest.QuestNode;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

/**
 * Utility class for reading quest definitions from JSON.
 * <p>
 * Wraps a configured Jackson {@link ObjectMapper} that
 * deserializes JSON arrays of {@link QuestNode} objects.
 * Unknown JSON properties are ignored to allow forward compatibility.
 * </p>
 *
 * <h3>Usage example</h3>
 * <pre>{@code
 * QuestJsonReader reader = new QuestJsonReader();
 * try (Reader r = Files.newBufferedReader(Path.of("quest.json"))) {
 *     List<QuestNode> nodes = reader.read(r);
 * }
 * }</pre>
 *
 * <h3>Configuration</h3>
 * <ul>
 *   <li>{@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES} is disabled –
 *       extra fields in JSON will not cause errors.</li>
 * </ul>
 */
public class QuestJsonReader {

    /**
     * Shared Jackson {@link ObjectMapper} configured for quest JSON parsing.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * Reads quest nodes from a JSON array using the provided {@link Reader}.
     *
     * @param reader the character stream containing quest JSON
     * @return list of parsed {@link QuestNode} objects
     * @throws IOException if reading or parsing fails
     */
    public List<QuestNode> read(Reader reader) throws IOException {
        return MAPPER.readerForListOf(QuestNode.class).readValue(reader);
    }
}
