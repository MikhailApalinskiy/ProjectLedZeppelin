package com.javarush.apalinskiy.repository.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javarush.apalinskiy.domain.quest.QuestNode;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

/**
 * Utility class for reading quest data from JSON sources.
 *
 * <p>Uses a preconfigured Jackson {@link ObjectMapper} to deserialize JSON arrays
 * into lists of {@link QuestNode} objects. Unknown JSON fields are ignored
 * for forward compatibility.</p>
 *
 * <p>This class is thread-safe due to the static, immutable {@code ObjectMapper} configuration.</p>
 */
public class QuestJsonReader {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * Reads and deserializes a list of {@link QuestNode} objects from a character stream.
     *
     * @param reader a JSON reader providing quest data
     * @return list of quest nodes parsed from JSON
     * @throws IOException if an I/O or parsing error occurs
     */
    public List<QuestNode> read(Reader reader) throws IOException {
        return MAPPER.readerForListOf(QuestNode.class).readValue(reader);
    }
}
