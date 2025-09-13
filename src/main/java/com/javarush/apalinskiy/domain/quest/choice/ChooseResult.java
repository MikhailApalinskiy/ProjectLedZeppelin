package com.javarush.apalinskiy.domain.quest.choice;

import com.javarush.apalinskiy.domain.quest.QuestNode;
import lombok.Getter;

/**
 * Result object returned after attempting to process a player's choice
 * in a quest.
 * <p>
 * This class is immutable and provides information about whether the
 * choice was successful, the next {@link QuestNode} to navigate to,
 * or details about the failure.
 * </p>
 *
 * <ul>
 *   <li>{@link #ok} – {@code true} if the choice was valid and led to a next node.</li>
 *   <li>{@link #next} – the next quest node (only if successful).</li>
 *   <li>{@link #error} – the type of error if the choice failed.</li>
 *   <li>{@link #message} – optional descriptive message about the error.</li>
 * </ul>
 *
 * <p>Instances are created using the static factory methods
 * {@link #ok(QuestNode)} and {@link #error(ChoiceError, String)}.</p>
 */
@Getter
public final class ChooseResult {

    /**
     * Whether the choice was processed successfully.
     */
    private final boolean ok;
    /**
     * The next quest node to navigate to (if successful).
     */
    private final QuestNode next;
    /**
     * Error type describing why the choice failed (if unsuccessful).
     */
    private final ChoiceError error;
    /**
     * Optional message describing the error or additional context.
     */
    private final String message;

    /**
     * Constructs a new {@code ChooseResult}.
     *
     * @param ok      whether the choice was successful
     * @param next    the next quest node (if successful)
     * @param error   the error type (if unsuccessful)
     * @param message optional error description
     */
    private ChooseResult(boolean ok, QuestNode next, ChoiceError error, String message) {
        this.ok = ok;
        this.next = next;
        this.error = error;
        this.message = message;
    }

    /**
     * Creates a successful result with the given next node.
     *
     * @param next the next quest node
     * @return a successful {@code ChooseResult}
     */
    public static ChooseResult ok(QuestNode next) {
        return new ChooseResult(true, next, null, null);
    }

    /**
     * Creates an error result with the given error type and message.
     *
     * @param err the error type
     * @param msg descriptive error message
     * @return a failed {@code ChooseResult}
     */
    public static ChooseResult error(ChoiceError err, String msg) {
        return new ChooseResult(false, null, err, msg);
    }
}

