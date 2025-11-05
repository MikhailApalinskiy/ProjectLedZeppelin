package com.javarush.apalinskiy.domain.quest.choice;

import com.javarush.apalinskiy.domain.quest.QuestNode;
import lombok.Getter;

/**
 * Represents the result of a player's choice during quest progression.
 *
 * <p>A {@code ChooseResult} indicates whether a choice was successful
 * and provides either the next {@link QuestNode} to navigate to
 * or an error describing the failure reason.</p>
 *
 * <p>This class is immutable and provides two static factory methods:
 * {@link #ok(QuestNode)} for successful outcomes and
 * {@link #error(ChoiceError, String)} for invalid or failed choices.</p>
 */
@Getter
public final class ChooseResult {

    /**
     * Whether the choice was successful and led to a valid next node.
     */
    private final boolean ok;

    /**
     * The next quest node to navigate to (non-null only if {@link #ok} is true).
     */
    private final QuestNode next;

    /**
     * The type of error that occurred (non-null only if {@link #ok} is false).
     */
    private final ChoiceError error;

    /**
     * Optional descriptive message explaining the result or error details.
     */
    private final String message;


    private ChooseResult(boolean ok, QuestNode next, ChoiceError error, String message) {
        this.ok = ok;
        this.next = next;
        this.error = error;
        this.message = message;
    }

    /**
     * Creates a successful {@code ChooseResult} indicating a valid transition
     * to the specified next quest node.
     *
     * @param next the next {@link QuestNode} to navigate to
     * @return a successful result instance
     */
    public static ChooseResult ok(QuestNode next) {
        return new ChooseResult(true, next, null, null);
    }

    /**
     * Creates a failed {@code ChooseResult} describing the reason for an invalid choice.
     *
     * @param err the {@link ChoiceError} describing the failure type
     * @param msg optional human-readable explanation of the error
     * @return an error result instance
     */
    public static ChooseResult error(ChoiceError err, String msg) {
        return new ChooseResult(false, null, err, msg);
    }
}

