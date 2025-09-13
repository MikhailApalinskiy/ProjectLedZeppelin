package com.javarush.apalinskiy.service.impl.quest;

import com.javarush.apalinskiy.repository.quest.QuestStore;
import com.javarush.apalinskiy.domain.quest.QuestNode;
import com.javarush.apalinskiy.service.quest.QuestAuthoringService;


import java.util.Objects;

/**
 * Default quest service backed by a {@link QuestStore}.
 * <p>
 * This class delegates all read and navigation operations to the underlying store
 * and uses the base logic from {@link AbstractQuestService} for handling choices.
 * </p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Expose quest metadata such as {@link #version()}.</li>
 *   <li>Provide access to the start node via {@link #getStart()}.</li>
 *   <li>Lookup quest nodes by ID via {@link #getById(int)}.</li>
 *   <li>Resolve navigation to the next node using {@link QuestStore#choose(int, String)}.</li>
 * </ul>
 *
 * <p>
 * In authoring scenarios this service is typically complemented by
 * {@link QuestAuthoringService}, which manages editing and publishing.
 * </p>
 */
public class DefaultQuestService extends AbstractQuestService {
    private final QuestStore store;

    /**
     * Creates a new quest service backed by the given store.
     *
     * @param store quest store (must not be {@code null})
     * @throws NullPointerException if {@code store} is {@code null}
     */
    public DefaultQuestService(QuestStore store) {
        this.store = Objects.requireNonNull(store);
    }

    /**
     * Returns the start node of the quest from the underlying store.
     *
     * @return start node, or {@code null} if store is empty
     */
    @Override
    public QuestNode getStart() {
        return store.start();
    }

    /**
     * Returns a node by its ID from the underlying store.
     *
     * @param id node ID
     * @return quest node, or {@code null} if not found
     */
    @Override
    public QuestNode getById(int id) {
        return store.get(id);
    }

    /**
     * Returns the version string of the underlying quest store.
     *
     * @return version string (never {@code null})
     */
    @Override
    public String version() {
        return store.version();
    }

    /**
     * Resolves the next quest node by delegating to the store.
     *
     * @param fromId current node ID
     * @param answer user input
     * @return next node, or {@code null} if no option matches
     */
    @Override
    protected QuestNode resolveNext(int fromId, String answer) {
        return store.choose(fromId, answer).orElse(null);
    }
}
