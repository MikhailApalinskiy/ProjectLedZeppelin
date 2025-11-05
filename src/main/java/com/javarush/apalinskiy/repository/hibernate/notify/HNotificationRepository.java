package com.javarush.apalinskiy.repository.hibernate.notify;

import com.javarush.apalinskiy.utils.HibernateUtil;
import com.javarush.apalinskiy.domain.notify.Notification;
import com.javarush.apalinskiy.repository.notify.NotificationRepository;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Hibernate-based implementation of {@link NotificationRepository}.
 *
 * <p>This repository provides CRUD and batch operations for {@link Notification}
 * entities, including saving, retrieving, marking as read, and deleting user notifications.
 * It uses manual transaction management through Hibernate {@link Session} and {@link Transaction}.</p>
 *
 * <p>All methods log detailed diagnostic information for easier debugging and monitoring.</p>
 */
public class HNotificationRepository implements NotificationRepository {

    /**
     * Logger for repository-level diagnostics.
     */
    private static final Logger log = LoggerFactory.getLogger(HNotificationRepository.class);

    /**
     * Shared Hibernate {@link SessionFactory} used to manage persistence sessions.
     */
    private final SessionFactory sessionFactory = HibernateUtil.getSessionFactory();

    /**
     * Persists or updates a {@link Notification} entity.
     *
     * <p>Uses {@link Session#merge(Object)} to handle both transient and detached instances.</p>
     *
     * @param n notification entity to save
     */
    @Override
    public void save(Notification n) {
        Session session = sessionFactory.getCurrentSession();
        log.info("save: userId={} id={}",
                n.getUser() != null ? n.getUser().getUserId() : "<null>",
                n.getId());
        session.merge(n);
        log.info("save: success id={}", n.getId());
    }

    /**
     * Retrieves a paginated list of notifications for a given user.
     *
     * @param userId user identifier
     * @param limit  maximum number of results to return
     * @param offset starting position for pagination
     * @return list of notifications ordered by {@code createdAt} descending
     */
    @Override
    public List<Notification> list(String userId, int limit, int offset) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            log.debug("list: userId={} limit={} offset={}", userId, limit, offset);
            Query<Notification> q = session.createQuery(
                    "from Notification n where n.user.userId = :uid order by n.createdAt desc",
                    Notification.class
            );
            q.setParameter("uid", userId);
            q.setFirstResult(Math.max(0, offset));
            q.setMaxResults(limit > 0 ? limit : 50);
            List<Notification> result = q.list();
            tx.commit();
            log.debug("Listed notifications userId={} size={} limit={} offset={}",
                    userId, result.size(), limit, offset);
            return result;
        } catch (RuntimeException e) {
            tx.rollback();
            log.error("Error listing notifications for userId={} limit={} offset={}",
                    userId, limit, offset, e);
            throw e;
        }
    }

    /**
     * Counts the number of unread notifications for a given user.
     *
     * @param userId user identifier
     * @return number of unread notifications
     */
    @Override
    public int unreadCount(String userId) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            log.debug("unreadCount: userId={}", userId);
            Long cnt = session.createQuery(
                    "select count(n) from Notification n " +
                            "where n.user.userId = :uid and n.read = false",
                    Long.class
            ).setParameter("uid", userId).uniqueResult();
            int result = cnt == null ? 0 : cnt.intValue();
            tx.commit();
            log.debug("Unread count userId={} count={}", userId, result);
            return result;
        } catch (RuntimeException e) {
            tx.rollback();
            log.error("Error getting unread count for userId={}", userId, e);
            throw e;
        }
    }

    /**
     * Marks all unread notifications for the specified user as read.
     *
     * @param userId user identifier
     */
    @Override
    public void markAllRead(String userId) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            log.info("markAllRead: userId={}", userId);
            int updated = session.createMutationQuery(
                    "update Notification n set n.read = true " +
                            "where n.user.userId = :uid and n.read = false"
            ).setParameter("uid", userId).executeUpdate();
            tx.commit();
            log.debug("All notifications marked as read userId={} updated={}", userId, updated);
        } catch (RuntimeException e) {
            tx.rollback();
            log.error("Error marking all notifications as read for userId={}", userId, e);
            throw e;
        }
    }

    /**
     * Deletes all notifications associated with a given user.
     *
     * @param userId user identifier
     */
    @Override
    public void clearAll(String userId) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            log.info("clearAll: userId={}", userId);
            int deleted = session.createMutationQuery(
                    "delete from Notification n where n.user.userId = :uid"
            ).setParameter("uid", userId).executeUpdate();
            tx.commit();
            log.debug("Cleared all notifications userId={} deleted={}", userId, deleted);
        } catch (RuntimeException e) {
            tx.rollback();
            log.error("Error clearing notifications for userId={}", userId, e);
            throw e;
        }
    }

    /**
     * Marks a specific notification as read for the given user.
     *
     * @param userId user identifier
     * @param id     notification ID
     */
    @Override
    public void markRead(String userId, String id) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            log.info("markRead: userId={} id={}", userId, id);
            int updated = session.createMutationQuery(
                            "update Notification n set n.read = true " +
                                    "where n.user.userId = :uid and n.id = :id and n.read = false"
                    ).setParameter("uid", userId)
                    .setParameter("id", id)
                    .executeUpdate();
            tx.commit();
            if (updated > 0) {
                log.debug("Notification marked read userId={} id={}", userId, id);
            } else {
                log.warn("markRead: not found or already read userId={} id={}", userId, id);
            }
        } catch (RuntimeException e) {
            tx.rollback();
            log.error("Error marking notification as read userId={} id={}", userId, id, e);
            throw e;
        }
    }
}
