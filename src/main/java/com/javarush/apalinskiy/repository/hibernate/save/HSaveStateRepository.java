package com.javarush.apalinskiy.repository.hibernate.save;

import com.javarush.apalinskiy.utils.HibernateUtil;
import com.javarush.apalinskiy.domain.save.GlobalSlot;
import com.javarush.apalinskiy.domain.save.GlobalSlotId;
import com.javarush.apalinskiy.domain.save.SaveState;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.repository.save.SaveStateRepository;
import com.javarush.apalinskiy.service.save.SaveStateService;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.time.Instant;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Hibernate-backed implementation of {@link SaveStateRepository}.
 *
 * <p>Provides CRUD-like operations for {@link SaveState} and its associated
 * {@link GlobalSlot} entities. Each method operates within the current
 * Hibernate session and writes diagnostic logs using {@link java.util.logging.Logger}.</p>
 *
 * <p>This repository is responsible for persisting and retrieving player save slots,
 * ensuring each user has a corresponding {@link SaveState} entity and up-to-date
 * {@link GlobalSlot} entries.</p>
 */
public class HSaveStateRepository implements SaveStateRepository {

    private static final Logger log = Logger.getLogger(HSaveStateRepository.class.getName());

    /**
     * Shared Hibernate {@link SessionFactory} instance.
     */
    private final SessionFactory sessionFactory = HibernateUtil.getSessionFactory();

    /**
     * Retrieves the {@link SaveState} for a given user or creates a new one if missing.
     *
     * <p>If no existing record is found, a new {@link SaveState} entity is created,
     * initialized with the specified default slot count, and persisted.</p>
     *
     * @param userId           user identifier (must not be {@code null})
     * @param defaultSlotCount default number of save slots to initialize (min = 1)
     * @return existing or newly created {@link SaveState}
     * @throws IllegalArgumentException if the associated {@link User} cannot be found
     */
    @Override
    public SaveState getOrCreate(String userId, int defaultSlotCount) {
        log.fine(() -> String.format("getOrCreate: userId=%s defaultSlotCount=%d", userId, defaultSlotCount));
        Session session = sessionFactory.getCurrentSession();
        SaveState st = session.get(SaveState.class, userId);
        if (st != null) {
            log.fine(() -> String.format("getOrCreate: existing found userId=%s", userId));
            return st;
        }
        User user = session.get(User.class, userId);
        if (user == null) {
            log.warning(() -> String.format("getOrCreate: user not found userId=%s", userId));
            throw new IllegalArgumentException("User not found: " + userId);
        }
        SaveState fresh = new SaveState();
        fresh.setUser(user);
        fresh.setUserId(userId);
        fresh.setSlotCount(Math.max(1, defaultSlotCount));
        fresh.setUpdatedAt(Instant.now());
        session.persist(fresh);
        log.info(() -> String.format("getOrCreate: created new SaveState userId=%s slots=%d", userId, fresh.getSlotCount()));
        return fresh;
    }

    /**
     * Retrieves a global save slot for a user by its numeric index.
     *
     * <p>If the slot does not exist, an empty {@link Optional} is returned.
     * Otherwise, a lightweight DTO {@link SaveStateService.GlobalSlot} is constructed
     * to avoid exposing persistent entities outside the repository.</p>
     *
     * @param userId user identifier
     * @param slot   slot index (0-based)
     * @return optional DTO describing the global slot
     */
    @Override
    public Optional<SaveStateService.GlobalSlot> getGlobalSlot(String userId, int slot) {
        log.fine(() -> String.format("getGlobalSlot: userId=%s slot=%d", userId, slot));
        Session session = sessionFactory.getCurrentSession();
        GlobalSlotId id = new GlobalSlotId(userId, slot);
        GlobalSlot gs = session.get(GlobalSlot.class, id);
        if (gs == null) {
            log.fine(() -> String.format("getGlobalSlot: not found userId=%s slot=%d", userId, slot));
            return Optional.empty();
        }
        SaveStateService.GlobalSlot dto = new SaveStateService.GlobalSlot(
                slot,
                gs.getNodeId(),
                gs.getTitle(),
                gs.getQuestId(),
                gs.getQuestName(),
                gs.getUpdatedAt()
        );
        log.fine(() -> String.format("getGlobalSlot: found userId=%s slot=%d questId=%s", userId, slot, gs.getQuestId()));
        return Optional.of(dto);
    }

    /**
     * Creates or updates a global save slot entry for a given user and slot index.
     *
     * <p>If no {@link SaveState} exists for the user, it is created automatically.
     * The slot data (quest, node, title) are stored under a composite key
     * {@link GlobalSlotId}.</p>
     *
     * @param userId    user identifier
     * @param slot      slot index (0-based)
     * @param questId   quest identifier (may be {@code null} for main quest)
     * @param questName human-readable quest name
     * @param nodeId    current node identifier
     * @param title     display title for the save slot
     */
    @Override
    public void setGlobalSlot(String userId, int slot, String questId, String questName, int nodeId, String title) {
        log.info(() -> String.format("setGlobalSlot: userId=%s slot=%d questId=%s nodeId=%d title='%s'",
                userId, slot, questId, nodeId, title));
        Session session = sessionFactory.getCurrentSession();
        SaveState st = session.get(SaveState.class, userId);
        if (st == null) {
            log.fine(() -> String.format("setGlobalSlot: creating SaveState for userId=%s", userId));
            st = getOrCreate(userId, 10);
        }
        GlobalSlotId id = new GlobalSlotId(userId, slot);
        GlobalSlot gs = session.get(GlobalSlot.class, id);
        if (gs == null) {
            log.fine(() -> String.format("setGlobalSlot: creating new GlobalSlot userId=%s slot=%d", userId, slot));
            gs = new GlobalSlot();
            gs.setId(id);
            gs.setSaveState(st);
        }
        gs.setQuestId(SaveState.questKey(questId));
        gs.setQuestName(questName);
        gs.setNodeId(nodeId);
        gs.setTitle(title);
        gs.setUpdatedAt(Instant.now());
        session.merge(gs);
        st.setUpdatedAt(Instant.now());
        session.merge(st);
        log.fine(() -> String.format("setGlobalSlot: updated userId=%s slot=%d questId=%s", userId, slot, questId));
    }

    /**
     * Clears (deletes) a global save slot entry for a given user and slot index.
     *
     * <p>If the slot exists, it is removed and the parent {@link SaveState}'s
     * {@code updatedAt} timestamp is refreshed.</p>
     *
     * @param userId user identifier
     * @param slot   slot index (0-based)
     */
    @Override
    public void clearGlobalSlot(String userId, int slot) {
        log.info(() -> String.format("clearGlobalSlot: userId=%s slot=%d", userId, slot));
        Session session = sessionFactory.getCurrentSession();
        GlobalSlotId id = new GlobalSlotId(userId, slot);
        GlobalSlot gs = session.get(GlobalSlot.class, id);
        if (gs == null) {
            log.fine(() -> String.format("clearGlobalSlot: nothing to clear userId=%s slot=%d", userId, slot));
            return;
        }
        session.remove(gs);
        SaveState st = session.get(SaveState.class, userId);
        if (st != null) {
            st.setUpdatedAt(Instant.now());
            session.merge(st);
            log.fine(() -> String.format("clearGlobalSlot: removed and updated state userId=%s", userId));
        } else {
            log.warning(() -> String.format("clearGlobalSlot: SaveState missing for userId=%s", userId));
        }
    }
}