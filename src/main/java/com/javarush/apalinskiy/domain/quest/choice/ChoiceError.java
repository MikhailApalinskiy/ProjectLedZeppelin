package com.javarush.apalinskiy.domain.quest.choice;

/**
 * Enumeration of possible errors that may occur when processing
 * a player's choice within a quest.
 * <p>
 * These values are typically returned or thrown by quest navigation logic
 * to indicate why a transition could not be completed.
 * </p>
 *
 * <ul>
 *   <li>{@link #NODE_NOT_FOUND} – the referenced node does not exist.</li>
 *   <li>{@link #FINAL_NODE} – the current node is marked as final and has no further choices.</li>
 *   <li>{@link #EMPTY_ANSWER} – the provided answer text was empty or invalid.</li>
 *   <li>{@link #NO_SUCH_OPTION} – the requested option index does not exist for the node.</li>
 * </ul>
 */
public enum ChoiceError {
    /**
     * The referenced node does not exist.
     */
    NODE_NOT_FOUND,
    /**
     * The current node is final and has no further choices.
     */
    FINAL_NODE,
    /**
     * The provided answer was empty or invalid.
     */
    EMPTY_ANSWER,
    /**
     * The requested option does not exist for the node.
     */
    NO_SUCH_OPTION
}
