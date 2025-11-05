package com.javarush.apalinskiy.service.impl.quest;

import com.javarush.apalinskiy.repository.quest.QuestStore;
import com.javarush.apalinskiy.domain.quest.QuestNode;


import java.util.Objects;

/**
 * Default implementation of {@link com.javarush.apalinskiy.service.quest.QuestService}
 * that operates on an immutable {@link QuestStore}.
 *
 * <p>This service provides basic quest navigation logic by delegating all read operations
 * to the underlying {@link QuestStore}. It does not perform any persistence,
 * synchronization, or modification of quest data.</p>
 *
 * <p>All navigation behavior (retrieving nodes, starting point, version, and transitions)
 * is determined entirely by the state of the provided store instance.</p>
 */
public class DefaultQuestService extends AbstractQuestService {

    private final QuestStore store;

    /**
     * Constructs a quest service based on the given {@link QuestStore}.
     *
     * @param store underlying quest store; must not be {@code null}
     * @throws NullPointerException if {@code store} is null
     */
    public DefaultQuestService(QuestStore store) {
        this.store = Objects.requireNonNull(store);
    }

    /**
     * Returns the starting quest node.
     *
     * <p>Delegates to {@link QuestStore#start()}.</p>
     *
     * @return the starting {@link QuestNode}, or {@code null} if none defined
     */
    @Override
    public QuestNode getStart() {
        return store.start();
    }

    /**
     * Retrieves a quest node by its identifier.
     *
     * <p>Delegates to {@link QuestStore#get(int)}.</p>
     *
     * @param id node identifier
     * @return the corresponding {@link QuestNode}, or {@code null} if not found
     */
    @Override
    public QuestNode getById(int id) {
        return store.get(id);
    }

    /**
     * Returns the version string representing the current quest state.
     *
     * <p>Delegates to {@link QuestStore#version()}.</p>
     *
     * @return store version string (format depends on implementation)
     */
    @Override
    public String version() {
        return store.version();
    }

    /**
     * Resolves the next quest node based on the current node and player’s answer.
     *
     * <p>Delegates to {@link QuestStore#choose(int, String)} and unwraps
     * the optional result, returning {@code null} if no match exists.</p>
     *
     * @param fromId current quest node ID
     * @param answer user’s answer text
     * @return the resolved {@link QuestNode}, or {@code null} if no valid transition exists
     */
    @Override
    protected QuestNode resolveNext(int fromId, String answer) {
        return store.choose(fromId, answer).orElse(null);
    }
}
