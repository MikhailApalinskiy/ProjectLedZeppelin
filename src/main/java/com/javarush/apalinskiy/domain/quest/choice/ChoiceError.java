package com.javarush.apalinskiy.domain.quest.choice;

/**
 * Enumerates possible validation or logic errors that can occur
 * during quest choice processing.
 *
 * <p>Each {@link ChoiceError} represents a specific failure scenario
 * encountered when a player attempts to make a choice in a quest node.</p>
 *
 * <h2>Available error types</h2>
 * <ul>
 *   <li>{@link #NODE_NOT_FOUND} — the target quest node was not found</li>
 *   <li>{@link #FINAL_NODE} — the current node is a final (terminal) node with no outgoing choices</li>
 *   <li>{@link #EMPTY_ANSWER} — the provided answer or option text is empty</li>
 *   <li>{@link #NO_SUCH_OPTION} — the selected option does not exist for the given node</li>
 * </ul>
 */
public enum ChoiceError {

    /**
     * The target quest node was not found.
     */
    NODE_NOT_FOUND,

    /**
     * The current node is final and has no available options.
     */
    FINAL_NODE,

    /**
     * The provided answer text is empty or invalid.
     */
    EMPTY_ANSWER,

    /**
     * The chosen option does not exist in the node’s available choices.
     */

    NO_SUCH_OPTION
}
