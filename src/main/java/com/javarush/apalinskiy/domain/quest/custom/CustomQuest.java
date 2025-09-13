package com.javarush.apalinskiy.domain.quest.custom;

import com.javarush.apalinskiy.domain.quest.QuestNode;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Immutable representation of a user-authored quest.
 * <p>
 * A {@code CustomQuest} bundles metadata (owner, name, version),
 * topology (list of {@link QuestNode} and {@code startId}),
 * publication flag, and audit timestamps.
 * <br/>
 * All fields are exposed via Lombok-generated getters ({@code @Getter});
 * the internal list of nodes is defensively copied and thus unmodifiable.
 *
 * <ul>
 *   <li>{@link #id} – stable unique identifier of the quest.</li>
 *   <li>{@link #ownerLogin} – author's login (owner).</li>
 *   <li>{@link #name} – human-readable quest name.</li>
 *   <li>{@link #startId} – ID of the start node within {@link #nodes}.</li>
 *   <li>{@link #nodes} – immutable snapshot of quest nodes.</li>
 *   <li>{@link #published} – whether the quest is visible to others.</li>
 *   <li>{@link #version} – optional version label (may be empty).</li>
 *   <li>{@link #createdAt} – creation timestamp.</li>
 *   <li>{@link #updatedAt} – last update timestamp.</li>
 * </ul>
 *
 * <p><strong>Immutability &amp; copying:</strong> to update a quest use
 * {@link #withUpdate(List, int, boolean, String)} which returns a new instance
 * with updated fields and refreshed {@link #updatedAt}; {@link #createdAt} is preserved.</p>
 */
@Getter
public class CustomQuest {

    /**
     * Stable unique identifier of the quest.
     */
    private final String id;
    /**
     * Author's login (owner of the quest).
     */
    private final String ownerLogin;
    /**
     * Human-readable quest name.
     */
    private final String name;
    /**
     * Identifier of the start node within {@link #nodes}.
     */
    private final int startId;
    /**
     * Immutable snapshot of quest nodes.
     */
    private final List<QuestNode> nodes;
    /**
     * Publication status flag.
     */
    private final boolean published;
    /**
     * Optional version tag (may be empty string).
     */
    private final String version;
    /**
     * Creation timestamp.
     */
    private final Instant createdAt;
    /**
     * Last update timestamp.
     */
    private final Instant updatedAt;

    /**
     * Creates a new immutable {@code CustomQuest}.
     *
     * @param id         unique quest identifier (non-null)
     * @param ownerLogin author's login (non-null)
     * @param name       quest name (non-null)
     * @param startId    start node ID
     * @param nodes      list of quest nodes; copied defensively (non-null)
     * @param published  publication flag
     * @param version    optional version label; defaults to empty string if {@code null}
     * @param createdAt  creation timestamp (non-null)
     * @param updatedAt  last update timestamp (non-null)
     * @throws NullPointerException if any required argument is {@code null}
     */
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

    /**
     * Returns a new {@code CustomQuest} instance with updated structure/state.
     * <p>
     * The following fields are changed:
     * <ul>
     *   <li>{@code nodes} – replaced with {@code newNodes} (copied defensively);</li>
     *   <li>{@code startId} – set to {@code newStartId};</li>
     *   <li>{@code published} – set to {@code newPublished};</li>
     *   <li>{@code version} – set to {@code newVersion} (empty if {@code null});</li>
     *   <li>{@code updatedAt} – set to {@link Instant#now()}.</li>
     * </ul>
     * The identity ({@code id}), ownership ({@code ownerLogin}), name, and {@code createdAt}
     * are preserved.
     *
     * @param newNodes     new list of nodes
     * @param newStartId   new start node ID
     * @param newPublished new publication flag
     * @param newVersion   new version label (may be {@code null} to reset to empty string)
     * @return a new {@code CustomQuest} reflecting the provided updates
     */
    public CustomQuest withUpdate(List<QuestNode> newNodes, int newStartId, boolean newPublished, String newVersion) {
        return new CustomQuest(
                id, ownerLogin, name, newStartId,
                newNodes, newPublished, newVersion,
                createdAt, Instant.now()
        );
    }
}
