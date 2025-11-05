package com.javarush.apalinskiy.service.impl.save;

import com.javarush.apalinskiy.utils.HibernateUtil;
import com.javarush.apalinskiy.repository.hibernate.save.HSaveStateRepository;
import com.javarush.apalinskiy.repository.save.SaveStateRepository;
import com.javarush.apalinskiy.service.save.SaveStateService;
import com.javarush.apalinskiy.domain.save.SaveState;
import lombok.Getter;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Hibernate-based implementation of {@link SaveStateService}.
 *
 * <p>This service provides transactional operations for managing
 * {@link SaveState} entities and their associated global save slots.
 * It acts as a bridge between the higher-level service layer and the
 * persistence layer, represented by {@link SaveStateRepository}.</p>
 *
 * <p>All methods ensure proper Hibernate transaction management:
 * <ul>
 *     <li>Start a transaction if none is active</li>
 *     <li>Commit after a successful operation</li>
 *     <li>Rollback in case of exceptions</li>
 *     <li>Temporarily switch {@code Session.setDefaultReadOnly}
 *         to optimize read or write operations</li>
 * </ul></p>
 *
 * <p>The repository used is {@link HSaveStateRepository}, which provides
 * Hibernate-mapped access to {@link SaveState} and
 * {@link com.javarush.apalinskiy.domain.save.GlobalSlot} entities.</p>
 */
@Getter
public class HSaveStateService implements SaveStateService {

    private static final Logger log = LoggerFactory.getLogger(HSaveStateService.class);

    private final SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
    private final SaveStateRepository repo = new HSaveStateRepository();
    private final int defaultSlotCount = 10;

    /**
     * Retrieves an existing {@link SaveState} for the given user,
     * or creates a new one if none exists.
     *
     * <p>Ensures that each user has a persistent save state record
     * with a defined number of available slots.</p>
     *
     * @param userId unique user identifier
     * @return an existing or newly created {@link SaveState}
     * @throws RuntimeException if the operation fails or the user does not exist
     */
    @Override
    public SaveState getOrCreate(String userId) {
        log.info("getOrCreate: userId={}", userId);
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.getTransaction();
        boolean started = false, touchedRO = false, prevRO = false;
        try {
            if (!tx.isActive()) {
                tx = session.beginTransaction();
                started = true;
                prevRO = session.isDefaultReadOnly();
                session.setDefaultReadOnly(false);
                touchedRO = true;
                log.debug("getOrCreate: tx started; defaultReadOnly {} -> false", prevRO);
            }
            SaveState st = repo.getOrCreate(userId, defaultSlotCount);
            if (started) {
                tx.commit();
                log.debug("getOrCreate: tx committed");
            }
            log.debug("getOrCreate: userId={} slotCount={}", userId, st.getSlotCount());
            return st;
        } catch (RuntimeException e) {
            if (started) {
                tx.rollback();
                log.debug("getOrCreate: tx rolled back");
            }
            throw e;
        } finally {
            if (touchedRO) {
                session.setDefaultReadOnly(prevRO);
                log.trace("getOrCreate: defaultReadOnly restored to {}", prevRO);
            }
        }
    }

    /**
     * Retrieves a specific global save slot for the given user.
     *
     * <p>This method operates in read-only mode whenever possible.</p>
     *
     * @param userId user identifier
     * @param slot   slot index (typically 0-based or 1-based depending on design)
     * @return an {@link Optional} containing the {@link GlobalSlot} if found,
     * or empty if no slot exists
     * @throws RuntimeException if a Hibernate or repository error occurs
     */
    @Override
    public Optional<GlobalSlot> getGlobalSlot(String userId, int slot) {
        log.info("getGlobalSlot: userId={} slot={}", userId, slot);
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.getTransaction();
        boolean started = false, touchedRO = false, prevRO = false;
        try {
            if (!tx.isActive()) {
                tx = session.beginTransaction();
                started = true;
                prevRO = session.isDefaultReadOnly();
                session.setDefaultReadOnly(true);
                touchedRO = true;
                log.debug("getGlobalSlot: tx started; defaultReadOnly {} -> true", prevRO);
            }
            Optional<SaveStateService.GlobalSlot> res = repo.getGlobalSlot(userId, slot);
            if (started) {
                tx.commit();
                log.debug("getGlobalSlot: tx committed");
            }
            if (res.isPresent()) {
                log.debug("getGlobalSlot: found userId={} slot={} questId={} nodeId={}",
                        userId, slot, res.get().questId(), res.get().nodeId());
            } else {
                log.debug("getGlobalSlot: empty userId={} slot={}", userId, slot);
            }
            return res;
        } catch (RuntimeException e) {
            if (started) {
                tx.rollback();
                log.debug("getGlobalSlot: tx rolled back");
            }
            log.error("getGlobalSlot: failed userId={} slot={}", userId, slot, e);
            throw e;
        } finally {
            if (touchedRO) {
                session.setDefaultReadOnly(prevRO);
                log.trace("getGlobalSlot: defaultReadOnly restored to {}", prevRO);
            }
        }
    }

    /**
     * Creates or updates a global save slot for a given user.
     *
     * <p>If the user has no {@link SaveState}, a new one will be created automatically.
     * The global slot entry is then persisted or updated accordingly.</p>
     *
     * <p>Also updates the parent {@link SaveState}'s {@code updatedAt} timestamp
     * to reflect the most recent save.</p>
     *
     * @param userId    user identifier
     * @param slot      save slot index
     * @param questId   quest identifier
     * @param questName quest display name
     * @param nodeId    ID of the quest node being saved
     * @param title     custom slot title or checkpoint name
     */
    @Override
    public void setGlobalSlot(String userId, int slot, String questId, String questName, int nodeId, String title) {
        log.info("setGlobalSlot: userId={} slot={} questId={} nodeId={} title='{}'", userId, slot, questId, nodeId, title);
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.getTransaction();
        boolean started = false, touchedRO = false, prevRO = false;
        try {
            if (!tx.isActive()) {
                tx = session.beginTransaction();
                started = true;
                prevRO = session.isDefaultReadOnly();
                session.setDefaultReadOnly(false);
                touchedRO = true;
                log.debug("setGlobalSlot: tx started; defaultReadOnly {} -> false", prevRO);
            }
            repo.setGlobalSlot(userId, slot, questId, questName, nodeId, title);
            if (started) {
                tx.commit();
                log.debug("setGlobalSlot: tx committed");
            }
            log.debug("setGlobalSlot userId={} slot={} questId={} nodeId={}", userId, slot, questId, nodeId);
        } catch (RuntimeException e) {
            if (started) {
                tx.rollback();
                log.debug("setGlobalSlot: tx rolled back");
            }
            log.error("setGlobalSlot: failed userId={} slot={} questId={} nodeId={}", userId, slot, questId, nodeId, e);
            throw e;
        } finally {
            if (touchedRO) {
                session.setDefaultReadOnly(prevRO);
                log.trace("setGlobalSlot: defaultReadOnly restored to {}", prevRO);
            }
        }
    }

    /**
     * Clears the specified global save slot for the user.
     *
     * <p>If the slot does not exist, this method performs no operation.
     * If a corresponding {@link SaveState} exists, its
     * {@code updatedAt} field is refreshed.</p>
     *
     * @param userId user identifier
     * @param slot   slot index to clear
     */
    @Override
    public void clearGlobalSlot(String userId, int slot) {
        log.info("clearGlobalSlot: userId={} slot={}", userId, slot);
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.getTransaction();
        boolean started = false, touchedRO = false, prevRO = false;
        try {
            if (!tx.isActive()) {
                tx = session.beginTransaction();
                started = true;
                prevRO = session.isDefaultReadOnly();
                session.setDefaultReadOnly(false);
                touchedRO = true;
                log.debug("clearGlobalSlot: tx started; defaultReadOnly {} -> false", prevRO);
            }
            repo.clearGlobalSlot(userId, slot);
            if (started) {
                tx.commit();
                log.debug("clearGlobalSlot: tx committed");
            }
            log.debug("clearGlobalSlot userId={} slot={}", userId, slot);
        } catch (RuntimeException e) {
            if (started) {
                tx.rollback();
                log.debug("clearGlobalSlot: tx rolled back");
            }
            log.error("clearGlobalSlot: failed userId={} slot={}", userId, slot, e);
            throw e;
        } finally {
            if (touchedRO) {
                session.setDefaultReadOnly(prevRO);
                log.trace("clearGlobalSlot: defaultReadOnly restored to {}", prevRO);
            }
        }
    }
}
