package com.javarush.apalinskiy.quest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Option(@Getter @JsonProperty("choice") String choice,
                     @Getter @JsonProperty("next") Integer next) {

    private static final Pattern WS = Pattern.compile("\\s+");

    public Option {
        choice = Objects.requireNonNull(choice, "choice");
    }

    public String normalizedChoice() {
        return WS.matcher(choice).replaceAll(" ")
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}
