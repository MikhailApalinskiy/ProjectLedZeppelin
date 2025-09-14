package com.javarush.apalinskiy.domain.quest.custom;

import com.javarush.apalinskiy.domain.quest.QuestNode;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Immutable catalog entity representing a user-authored quest.
 * Holds identity, ownership, structure, publish state, version and audit timestamps.
 * All fields are non-null (except primitives); node list is defensively copied.
 */
@Getter
public class CustomQuest {

    private final String id;
    private final String ownerId;
    private final String name;
    private final int startId;
    private final List<QuestNode> nodes;
    private final boolean published;
    private final String version;
    private final Instant createdAt;
    private final Instant updatedAt;

    /**
     * Creates a new {@code CustomQuest}.
     * Required arguments must be non-null; {@code nodes} is defensively copied;
     * {@code version} is normalized to an empty string when null.
     */
    public CustomQuest(String id, String ownerId, String name, int startId,
                       List<QuestNode> nodes, boolean published, String version,
                       Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.ownerId = Objects.requireNonNull(ownerId);
        this.name = Objects.requireNonNull(name);
        this.startId = startId;
        this.nodes = List.copyOf(Objects.requireNonNull(nodes));
        this.published = published;
        this.version = Objects.requireNonNullElse(version, "");
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    /**
     * Returns a new instance with updated graph, start node, publish flag, and version.
     * {@code updatedAt} is set to {@link Instant#now()} while other identity fields are preserved.
     *
     * @param newNodes     replacement node list
     * @param newStartId   new start node id
     * @param newPublished new publish state
     * @param newVersion   new version label
     * @return updated {@code CustomQuest} copy
     */
    public CustomQuest withUpdate(List<QuestNode> newNodes, int newStartId, boolean newPublished, String newVersion) {
        return new CustomQuest(
                id, ownerId, name, newStartId,
                newNodes, newPublished, newVersion,
                createdAt, Instant.now()
        );
    }
}
