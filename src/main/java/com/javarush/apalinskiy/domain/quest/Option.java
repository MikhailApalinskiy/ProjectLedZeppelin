package com.javarush.apalinskiy.domain.quest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.javarush.apalinskiy.domain.quest.choice.ChoiceNormalizer;
import lombok.Getter;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Option(@Getter @JsonProperty("choice") String choice,
                     @Getter @JsonProperty("next") Integer next) {

    public Option {
        choice = java.util.Objects.requireNonNull(choice, "choice");
    }

    public String normalizedChoice() {
        return ChoiceNormalizer.normalize(choice);
    }
}
