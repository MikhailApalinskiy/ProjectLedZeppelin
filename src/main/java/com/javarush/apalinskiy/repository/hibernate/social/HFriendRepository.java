package com.javarush.apalinskiy.repository.hibernate.social;

import com.javarush.apalinskiy.utils.HibernateUtil;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.repository.social.FriendRepository;
import com.javarush.apalinskiy.domain.social.FriendRequest;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Hibernate-backed implementation of {@link FriendRepository}.
 *
 * <p>Manages user friendship relations and {@link FriendRequest} entities.
 * Provides operations for friendship checking, creation, removal, search, and pagination.
 * All queries use Hibernate HQL and assume the current transaction is already active
 * (handled externally by the service layer).</p>
 *
 * <p>Friendships are represented via a symmetric {@link User#getFriends()} ↔ {@link User#getFriendOf()}
 * many-to-many association. The repository ensures both sides are updated consistently.</p>
 */
public class HFriendRepository implements FriendRepository {

    private static final Logger log = LoggerFactory.getLogger(HFriendRepository.class);

    /**
     * Shared Hibernate session factory.
     */
    private final SessionFactory sessionFactory = HibernateUtil.getSessionFactory();

    /**
     * Checks whether two users are mutual friends.
     *
     * @param a first user ID
     * @param b second user ID
     * @return {@code true} if friendship exists in either direction
     */
    @Override
    public boolean areFriends(String a, String b) {
        log.debug("areFriends: a={} b={}", a, b);
        Session session = sessionFactory.getCurrentSession();
        Long cnt = session.createQuery("""
                        select count(u)
                        from User u join u.friends f
                        where (u.userId = :a and f.userId = :b)
                           or (u.userId = :b and f.userId = :a)
                        """, Long.class)
                .setParameter("a", a)
                .setParameter("b", b)
                .uniqueResult();
        boolean result = (cnt != null && cnt > 0);
        log.debug("areFriends: a={} b={} -> {}", a, b, result);
        return result;
    }

    /**
     * Creates a bidirectional friendship link between two users, if both exist.
     *
     * <p>If the users are already connected or one is missing, the method logs a warning
     * and performs no changes.</p>
     *
     * @param a first user ID
     * @param b second user ID
     */
    @Override
    public void addFriendship(String a, String b) {
        if (a.equals(b)) {
            log.warn("addFriendship: same ids a==b={}", a);
            return;
        }
        log.info("addFriendship: a={} b={}", a, b);
        Session session = sessionFactory.getCurrentSession();
        String left = (a.compareTo(b) <= 0) ? a : b;
        String right = (a.compareTo(b) <= 0) ? b : a;
        User uLeft = session.get(User.class, left);
        User uRight = session.get(User.class, right);
        if (uLeft == null || uRight == null) {
            log.warn("addFriendship: user not found left={} right={} leftNull={} rightNull={}",
                    left, right, uLeft == null, uRight == null);
            return;
        }
        uLeft.getFriends().add(uRight);
        uRight.getFriendOf().add(uLeft);
        log.debug("addFriendship: linked left={} right={}", left, right);
    }

    /**
     * Removes a friendship link between two users if present.
     *
     * <p>The association is deleted from both {@code friends} and {@code friendOf} collections.</p>
     *
     * @param a first user ID
     * @param b second user ID
     */
    @Override
    public void removeFriendship(String a, String b) {
        log.info("removeFriendship: a={} b={}", a, b);
        Session s = sessionFactory.getCurrentSession();
        String left = (a.compareTo(b) <= 0) ? a : b;
        String right = (a.compareTo(b) <= 0) ? b : a;
        User owner = s.createQuery("from User u where u.userId = :id", User.class)
                .setParameter("id", left)
                .uniqueResult();
        User friend = s.createQuery("from User u where u.userId = :id", User.class)
                .setParameter("id", right)
                .uniqueResult();
        if (owner == null || friend == null) {
            log.warn("removeFriendship: user not found left={} right={} ownerNull={} friendNull={}",
                    left, right, owner == null, friend == null);
            return;
        }
        if (!owner.getFriends().contains(friend)) {
            log.warn("removeFriendship: no link to remove left={} right={}", left, right);
            return;
        }
        owner.getFriends().remove(friend);
        if (owner.getFriendOf() != null) {
            friend.getFriendOf().remove(owner);
        }
        log.debug("removeFriendship: unlinked left={} right={}", left, right);
    }

    /**
     * Returns all friend IDs associated with a user (both directions).
     *
     * @param userId user identifier
     * @return combined set of friend user IDs
     */
    @Override
    public Set<String> friendsOf(String userId) {
        log.debug("friendsOf: userId={}", userId);
        Session session = sessionFactory.getCurrentSession();
        List<String> left = session.createQuery(
                "select f.userId from User u join u.friends f where u.userId = :id",
                String.class
        ).setParameter("id", userId).getResultList();
        List<String> right = session.createQuery(
                "select u.userId from User u join u.friends f where f.userId = :id",
                String.class
        ).setParameter("id", userId).getResultList();
        Set<String> all = new LinkedHashSet<>(left.size() + right.size());
        all.addAll(left);
        all.addAll(right);
        log.debug("friendsOf: userId={} left={} right={} total={}", userId, left.size(), right.size(), all.size());
        return all;
    }

    /**
     * Searches for a pending friend request from one user to another.
     *
     * @param from sender user ID
     * @param to   recipient user ID
     * @return optional {@link FriendRequest} if found
     */
    @Override
    public Optional<FriendRequest> findPending(String from, String to) {
        Session session = sessionFactory.getCurrentSession();
        String hql = """
                    from FriendRequest fr
                    where fr.fromUser.id = :from
                      and fr.toUser.id = :to
                      and fr.status = 'PENDING'
                """;
        FriendRequest req = session.createQuery(hql, FriendRequest.class)
                .setParameter("from", from)
                .setParameter("to", to)
                .uniqueResult();
        log.debug("findPending from={} to={} found={}", from, to, req != null);
        return Optional.ofNullable(req);
    }

    /**
     * Persists a new friend request in PENDING state.
     *
     * @param req request entity to save
     */
    @Override
    public void saveRequest(FriendRequest req) {
        Session session = sessionFactory.getCurrentSession();
        session.persist(req);
        log.debug("saveRequest from={} to={}", req.getFromUser().getUserId(), req.getToUser().getUserId());
    }

    /**
     * Deletes a pending friend request between two users.
     *
     * @param from sender user ID
     * @param to   recipient user ID
     */
    @Override
    public void removeRequest(String from, String to) {
        Session session = sessionFactory.getCurrentSession();
        String hql = """
                    delete from FriendRequest fr
                    where fr.fromUser.id = :from
                      and fr.toUser.id = :to
                      and fr.status = 'PENDING'
                """;
        int deleted = session.createMutationQuery(hql)
                .setParameter("from", from)
                .setParameter("to", to)
                .executeUpdate();
        log.debug("removeRequest from={} to={} removed={}", from, to, deleted);
    }

    /**
     * Lists all incoming friend requests for a user (status = {@code PENDING}).
     *
     * @param userId recipient user ID
     * @return list of pending incoming requests, sorted by creation date (desc)
     */
    @Override
    public List<FriendRequest> incoming(String userId) {
        Session session = sessionFactory.getCurrentSession();
        List<FriendRequest> list = session.createQuery("""
                            from FriendRequest fr
                            where fr.toUser.id = :uid
                              and fr.status = 'PENDING'
                            order by fr.createdAt desc
                        """, FriendRequest.class)
                .setParameter("uid", userId)
                .getResultList();
        log.debug("incoming userId={} size={}", userId, list.size());
        return list;
    }

    /**
     * Lists all outgoing friend requests for a user (status = {@code PENDING}).
     *
     * @param userId sender user ID
     * @return list of pending outgoing requests, sorted by creation date (desc)
     */
    @Override
    public List<FriendRequest> outgoing(String userId) {
        Session session = sessionFactory.getCurrentSession();
        List<FriendRequest> list = session.createQuery("""
                            from FriendRequest fr
                            where fr.fromUser.id = :uid
                              and fr.status = 'PENDING'
                            order by fr.createdAt desc
                        """, FriendRequest.class)
                .setParameter("uid", userId)
                .getResultList();
        log.debug("outgoing userId={} size={}", userId, list.size());
        return list;
    }

    /**
     * Counts the total number of friends for a user, with optional name/login filter.
     *
     * <p>Performs a case-insensitive match against {@code userLogin} and {@code userName}.
     * If the filter resembles a UUID, it is also matched directly against {@code userId}.</p>
     *
     * @param userId user identifier
     * @param q      optional search string (ignored if blank)
     * @return total count of matching friends
     */
    @Override
    public long countFriends(String userId, String q) {
        Session session = sessionFactory.getCurrentSession();
        String jq = (q == null || q.isBlank()) ? null : q.trim().toLowerCase();
        boolean looksLikeId = jq != null && jq.matches("^[0-9a-f\\-]{32,36}$");
        log.debug("countFriends: userId={} qPresent={} looksLikeId={}", userId, jq != null, looksLikeId);
        final String hql = (jq == null)
                ? """
                 select count(distinct u)
                 from User u
                 where (
                     u in (select f from User x join x.friends f where x.userId = :uid)
                     or
                     u in (select o from User o join o.friends f where f.userId = :uid)
                 )
                """
                : (looksLikeId
                ? """
                 select count(distinct u)
                 from User u
                 where (
                     u in (select f from User x join x.friends f where x.userId = :uid)
                     or
                     u in (select o from User o join o.friends f where f.userId = :uid)
                 )
                   and (
                       lower(u.userLogin) like :pat
                       or lower(u.userName) like :pat
                       or u.userId = :qid
                   )
                """
                : """
                 select count(distinct u)
                 from User u
                 where (
                     u in (select f from User x join x.friends f where x.userId = :uid)
                     or
                     u in (select o from User o join o.friends f where f.userId = :uid)
                 )
                   and (
                       lower(u.userLogin) like :pat
                       or lower(u.userName) like :pat
                   )
                """
        );
        Query<Long> qh = session.createQuery(hql, Long.class)
                .setParameter("uid", userId);
        if (jq != null) {
            qh.setParameter("pat", "%" + jq + "%");
            if (looksLikeId) qh.setParameter("qid", q);
        }
        Long cnt = qh.uniqueResult();
        long out = (cnt == null ? 0 : cnt);
        log.debug("countFriends: userId={} result={}", userId, out);
        return out;
    }

    /**
     * Retrieves a paginated list of friends for a user, ordered alphabetically.
     *
     * <p>Filtering logic mirrors {@link #countFriends(String, String)} —
     * it matches login, name, or ID (if query resembles UUID).</p>
     *
     * @param userId user identifier
     * @param page   page number (1-based)
     * @param size   number of records per page
     * @param q      optional case-insensitive filter
     * @return list of {@link User} friends
     */
    @Override
    public List<User> pageFriends(String userId, int page, int size, String q) {
        Session session = sessionFactory.getCurrentSession();
        String jq = (q == null || q.isBlank()) ? null : q.trim().toLowerCase();
        boolean looksLikeId = jq != null && jq.matches("^[0-9a-f\\-]{32,36}$");
        int offset = Math.max(0, (page - 1) * size);
        log.debug("pageFriends: userId={} page={} size={} qPresent={} looksLikeId={}",
                userId, page, size, jq != null, looksLikeId);
        final String hql = (jq == null)
                ? """
                 select distinct u
                 from User u
                 where (
                     u in (select f from User x join x.friends f where x.userId = :uid)
                     or
                     u in (select o from User o join o.friends f where f.userId = :uid)
                 )
                 order by u.userName asc, u.userId asc
                """
                : (looksLikeId
                ? """
                 select distinct u
                 from User u
                 where (
                     u in (select f from User x join x.friends f where x.userId = :uid)
                     or
                     u in (select o from User o join o.friends f where f.userId = :uid)
                 )
                   and (
                       lower(u.userLogin) like :pat
                       or lower(u.userName) like :pat
                       or u.userId = :qid
                   )
                 order by u.userName asc, u.userId asc
                """
                : """
                 select distinct u
                 from User u
                 where (
                     u in (select f from User x join x.friends f where x.userId = :uid)
                     or
                     u in (select o from User o join o.friends f where f.userId = :uid)
                 )
                   and (
                       lower(u.userLogin) like :pat
                       or lower(u.userName) like :pat
                   )
                 order by u.userName asc, u.userId asc
                """
        );
        Query<User> qh = session.createQuery(hql, User.class)
                .setParameter("uid", userId)
                .setFirstResult(offset)
                .setMaxResults(size);
        if (jq != null) {
            qh.setParameter("pat", "%" + jq + "%");
            if (looksLikeId) qh.setParameter("qid", q);
        }
        List<User> list = qh.getResultList();
        log.debug("pageFriends: userId={} page={} size={} -> result={}", userId, page, size, list.size());
        return list;
    }
}
