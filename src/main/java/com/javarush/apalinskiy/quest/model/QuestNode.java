package com.javarush.apalinskiy.quest.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.*;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public final class QuestNode {

    private final int id;
    private final String text;
    private final List<Option> options;
    private final boolean fin;
    private final String image;

    private QuestNode(int id, String text, List<Option> options, boolean fin, String image) {
        this.id = id;
        this.text = Objects.requireNonNull(text, "text");
        this.options = List.copyOf(Objects.requireNonNullElseGet(options, List::of));
        this.fin = fin;
        this.image = (image == null || image.isBlank()) ? null : image.trim();
        validate();
    }

    public static QuestNode of(int id, String text, List<Option> options, boolean fin, String image) {
        return new QuestNode(id, text, options, fin, image);
    }

    public static QuestNode fin(int id, String text, String image) {
        return new QuestNode(id, text, List.of(), true, image);
    }

    @JsonCreator
    public static QuestNode json(
            @JsonProperty("id") int id,
            @JsonProperty("text") String text,
            @JsonProperty("options") List<Option> options,
            @JsonProperty("final") boolean fin,
            @JsonProperty("image") String image) {
        return new QuestNode(id, text, options, fin, image);
    }

    private void validate() {
        if (id <= 0) {
            throw new IllegalArgumentException("id must be > 0");
        }
        if (text.isBlank()) {
            throw new IllegalArgumentException("text must not be blank");
        }
        if (fin && !options.isEmpty()) {
            throw new IllegalArgumentException("final node should not have options");
        }
        HashSet<String> dupCheck = new HashSet<>();
        for (Option o : options) {
            String n = o.normalizedChoice();
            if (!dupCheck.add(n)) {
                throw new IllegalArgumentException("duplicate option choice: " + o.choice());
            }
            if (o.next() == null) {
                throw new IllegalArgumentException("non-final node option must have next: " + o.choice());
            }
        }
        if (text.length() > 10_000) {
            throw new IllegalArgumentException("text too long");
        }
    }

    public List<String> optionTexts() {
        return options.stream().map(Option::choice).toList();
    }
}
