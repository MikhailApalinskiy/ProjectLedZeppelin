package com.javarush.apalinskiy.quest;

import com.javarush.apalinskiy.domain.quest.QuestNode;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Getter
public class CustomQuest {
    private final String id;
    private final String ownerLogin;
    private final String name;
    private final int startId;
    private final List<QuestNode> nodes;
    private final boolean published;
    private final String version;
    private final Instant createdAt;
    private final Instant updatedAt;

    public CustomQuest(String id, String ownerLogin, String name, int startId,
                       List<QuestNode> nodes, boolean published, String version,
                       Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.ownerLogin = Objects.requireNonNull(ownerLogin);
        this.name = Objects.requireNonNull(name);
        this.startId = startId;
        this.nodes = List.copyOf(Objects.requireNonNull(nodes));
        this.published = published;
        this.version = Objects.requireNonNullElse(version, "");
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public CustomQuest withUpdate(List<QuestNode> newNodes, int newStartId, boolean newPublished, String newVersion) {
        return new CustomQuest(
                id, ownerLogin, name, newStartId,
                newNodes, newPublished, newVersion,
                createdAt, Instant.now()
        );
    }
}
