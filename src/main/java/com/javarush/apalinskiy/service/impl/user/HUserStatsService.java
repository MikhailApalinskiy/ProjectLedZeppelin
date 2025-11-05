package com.javarush.apalinskiy.service.impl.user;

import com.javarush.apalinskiy.utils.HibernateUtil;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.domain.user.UserStats;
import com.javarush.apalinskiy.service.user.UserStatsService;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hibernate-based implementation of {@link UserStatsService}.
 *
 * <p>This service maintains aggregated statistics for each user, including:
 * <ul>
 *     <li>Total number of created quests</li>
 *     <li>Total number of completed quests</li>
 *     <li>Number of unique endings unlocked in the main quest</li>
 * </ul>
 *
 * <p>Each modifying operation runs within a Hibernate-managed transaction.
 * Read operations are executed in read-only mode for performance and safety.
 * If no active transaction exists, the service starts and commits its own.</p>
 *
 * <p>All methods are idempotent for missing users (they will create an empty stats record if needed).</p>
 */
public class HUserStatsService implements UserStatsService {

    private static final Logger log = LoggerFactory.getLogger(HUserStatsService.class.getName());

    private final SessionFactory sessionFactory = HibernateUtil.getSessionFactory();

    /**
     * Increments the number of quests created by the specified user.
     *
     * <p>If the user does not yet have a {@link UserStats} record,
     * a new one is created and initialized with zero values.</p>
     *
     * @param userId user identifier (ignored if null or blank)
     * @throws RuntimeException if the transaction fails
     */
    @Override
    public void incCreated(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        log.info("incCreated: userId={}", userId);
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        log.debug("incCreated: tx started");
        try {
            UserStats st = getOrCreateStats(session, userId);
            st.setQuestsCreated(st.getQuestsCreated() + 1);
            tx.commit();
            log.debug("incCreated: tx committed (questsCreated={})", st.getQuestsCreated());
        } catch (RuntimeException e) {
            tx.rollback();
            log.debug("incCreated: tx rolled back");
            log.error("incCreated failed userId={}", userId, e);
            throw e;
        }
    }

    /**
     * Updates statistics when a user completes a quest.
     *
     * <p>Increments {@code questsCompleted}, and if the quest key equals {@code "main"}
     * adds the specified {@code finalNodeId} to the set of unlocked endings.
     * Each unique ending is counted only once.</p>
     *
     * @param userId      user identifier (ignored if null or blank)
     * @param questKey    key of the quest (used to detect the main storyline)
     * @param finalNodeId identifier of the final node (used to track endings)
     * @throws RuntimeException if the transaction fails
     */
    @Override
    public void onQuestCompleted(String userId, String questKey, Integer finalNodeId) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        log.info("onQuestCompleted: userId={} questKey={} finalNodeId={}", userId, questKey, finalNodeId);
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        log.debug("onQuestCompleted: tx started");
        try {
            UserStats st = getOrCreateStats(session, userId);
            st.setQuestsCompleted(st.getQuestsCompleted() + 1);
            if ("main".equals(questKey) && finalNodeId != null) {
                if (st.getMainFinals().add(finalNodeId)) {
                    st.setEndingsUnlocked(st.getEndingsUnlocked() + 1);
                    log.debug("onQuestCompleted: new main ending unlocked userId={} finalNodeId={}", userId, finalNodeId);
                } else {
                    log.debug("onQuestCompleted: main ending already unlocked userId={} finalNodeId={}", userId, finalNodeId);
                }
            }
            tx.commit();
            log.debug("onQuestCompleted: tx committed (questsCompleted={}, endingsUnlocked={})",
                    st.getQuestsCompleted(), st.getEndingsUnlocked());
        } catch (RuntimeException e) {
            tx.rollback();
            log.debug("onQuestCompleted: tx rolled back");
            log.error("onQuestCompleted failed userId={} questKey={}", userId, questKey, e);
            throw e;
        }
    }

    /**
     * Retrieves the statistics for the specified user.
     *
     * <p>Runs in read-only mode. If no statistics exist for this user,
     * returns a transient {@link UserStats} instance with all values set to zero.</p>
     *
     * @param userId user identifier
     * @return {@link UserStats} entity or zero-filled placeholder if none exists
     * @throws RuntimeException if the transaction fails
     */
    @Override
    public UserStats statsOf(String userId) {
        log.debug("statsOf: userId={}", userId);
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        log.debug("statsOf: tx started");
        boolean prev = session.isDefaultReadOnly();
        session.setDefaultReadOnly(true);
        try {
            UserStats st = session.createQuery(
                            "from UserStats us where us.user.userId = :uid", UserStats.class)
                    .setParameter("uid", userId)
                    .setMaxResults(1)
                    .uniqueResult();
            tx.commit();
            log.debug("statsOf: tx committed; found={}", st != null);
            return st != null ? st : new UserStats(0L, 0L, 0L);
        } catch (RuntimeException e) {
            tx.rollback();
            log.debug("statsOf: tx rolled back");
            log.error("statsOf failed userId={}", userId, e);
            throw e;
        } finally {
            session.setDefaultReadOnly(prev);
            log.trace("statsOf: defaultReadOnly restored to {}", prev);
        }
    }

    /**
     * Loads an existing {@link UserStats} record for a user,
     * or creates and persists a new one if not found.
     *
     * <p>This helper method is used internally by {@link #incCreated(String)}
     * and {@link #onQuestCompleted(String, String, Integer)}.</p>
     *
     * @param s      active {@link Session}
     * @param userId user identifier
     * @return existing or newly created {@link UserStats} instance
     * @throws IllegalArgumentException if the user does not exist
     */
    private UserStats getOrCreateStats(Session s, String userId) {
        log.debug("getOrCreateStats: userId={}", userId);
        UserStats st = s.createQuery(
                        "from UserStats us where us.user.userId = :uid", UserStats.class)
                .setParameter("uid", userId)
                .setMaxResults(1)
                .uniqueResult();
        if (st != null) {
            log.debug("getOrCreateStats: found existing stats userId={}", userId);
            return st;
        }
        User u = s.byId(User.class).load(userId);
        st = new UserStats(0L, 0L, 0L);
        st.setUser(u);
        s.persist(st);
        log.info("getOrCreateStats: created new stats record userId={}", userId);
        return st;
    }
}
