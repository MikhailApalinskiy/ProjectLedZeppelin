package com.javarush.apalinskiy.domain.save;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Composite primary key for the {@link GlobalSlot} entity.
 *
 * <p>The {@code GlobalSlotId} combines the user identifier and the
 * slot index to uniquely identify each global save slot belonging
 * to a specific user.</p>
 *
 * <p>This class is annotated with {@link Embeddable} and is used in conjunction
 * with {@link jakarta.persistence.EmbeddedId} in {@link GlobalSlot}.</p>
 */
@Getter
@Setter
@Embeddable
@EqualsAndHashCode
public class GlobalSlotId {

    /**
     * Identifier of the user who owns this save slot.
     */
    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    /**
     * Index of the save slot within the user’s available slots.
     */
    @Column(name = "slot_index", nullable = false)
    private int slotIndex;

    /**
     * Default constructor for JPA.
     */
    public GlobalSlotId() {
    }

    /**
     * Constructs a composite ID with the specified user and slot index.
     *
     * @param userId    identifier of the user
     * @param slotIndex index of the save slot
     */
    public GlobalSlotId(String userId, int slotIndex) {
        this.userId = userId;
        this.slotIndex = slotIndex;
    }
}
