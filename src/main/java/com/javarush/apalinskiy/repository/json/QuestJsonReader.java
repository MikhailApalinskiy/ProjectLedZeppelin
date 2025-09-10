package com.javarush.apalinskiy.repository.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javarush.apalinskiy.domain.quest.QuestNode;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

public class QuestJsonReader {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public List<QuestNode> read(Reader reader) throws IOException {
        return MAPPER.readerForListOf(QuestNode.class).readValue(reader);
    }
}
