package com.javarush.apalinskiy.service.impl.social;

import com.javarush.apalinskiy.utils.HibernateUtil;
import com.javarush.apalinskiy.repository.social.FriendRepository;
import com.javarush.apalinskiy.domain.social.FriendRequest;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.domain.notify.NotificationEvent;
import com.javarush.apalinskiy.service.impl.user.DefaultUserService;
import com.javarush.apalinskiy.service.social.FriendService;
import com.javarush.apalinskiy.service.notify.NotificationService;
import com.javarush.apalinskiy.domain.notify.NotificationType;
import com.javarush.apalinskiy.service.user.UserService;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Default Hibernate-based implementation of {@link FriendService}.
 *
 * <p>This service encapsulates the complete workflow for managing user friendships
 * and friend requests, including:</p>
 *
 * <ul>
 *     <li>Sending, accepting, declining, and canceling friend requests</li>
 *     <li>Establishing and removing mutual friendships</li>
 *     <li>Fetching lists of friends and pending requests</li>
 *     <li>Triggering {@link NotificationEvent}s for all major actions</li>
 * </ul>
 *
 * <p>All database operations are executed within Hibernate-managed transactions.
 * If no active transaction exists, the service starts and commits its own, ensuring:</p>
 *
 * <ul>
 *     <li>Automatic rollback on errors</li>
 *     <li>Safe read-only mode for listing queries</li>
 *     <li>Consistent session handling across repository and notification layers</li>
 * </ul>
 *
 * <p>Friendship is symmetric — both users become linked in each other’s collections.
 * Pending friend requests are modeled via {@link FriendRequest} entities.</p>
 *
 * @see FriendRepository
 * @see NotificationService
 * @see NotificationType
 */
public class DefaultFriendService implements FriendService {

    private static final Logger log = LoggerFactory.getLogger(DefaultFriendService.class);

    private final SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
    private final FriendRepository repo;
    private final UserService users;
    private final NotificationService notify;

    /**
     * Constructs a new {@code DefaultFriendService} using the specified components.
     *
     * @param repo   the underlying {@link FriendRepository} implementation
     * @param users  {@link UserService} used to resolve and validate user identities
     * @param notify {@link NotificationService} used to send system notifications
     */
    public DefaultFriendService(FriendRepository repo, UserService users, NotificationService notify) {
        this.repo = repo;
        this.users = users;
        this.notify = notify;
    }

    /**
     * Sends a friend request from one user to another.
     *
     * <p>If a reverse pending request exists, it is automatically accepted.
     * The sender and receiver then become friends, and both are notified.</p>
     *
     * @param fromUserId sender’s user ID
     * @param toUserId   recipient’s user ID
     * @throws IllegalArgumentException if IDs are null, identical, or the target does not exist
     * @throws IllegalStateException    if a request already exists or users are already friends
     */
    @Override
    public void sendRequest(String fromUserId, String toUserId) {
        log.info("sendRequest: from={} to={}", fromUserId, toUserId);
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        log.debug("sendRequest: tx started");
        try {
            if (fromUserId == null || toUserId == null) {
                log.warn("sendRequest denied: null ids from={} to={}", fromUserId, toUserId);
                throw new IllegalArgumentException("User IDs must not be null");
            }
            if (fromUserId.equals(toUserId)) {
                log.warn("sendRequest denied: self-add from={}", fromUserId);
                throw new IllegalArgumentException("You can't add yourself as a friend");
            }
            users.findById(toUserId).orElseThrow(() -> {
                log.warn("sendRequest denied: target not found from={} to={}", fromUserId, toUserId);
                return new IllegalArgumentException("The user was not found");
            });
            if (repo.areFriends(fromUserId, toUserId)) {
                log.warn("sendRequest denied: already friends from={} to={}", fromUserId, toUserId);
                throw new IllegalStateException("You are already friends");
            }
            if (repo.findPending(toUserId, fromUserId).isPresent()) {
                log.debug("sendRequest: reverse pending found -> auto-accept");
                repo.removeRequest(toUserId, fromUserId);
                repo.addFriendship(fromUserId, toUserId);
                notify.notify(NotificationEvent.of(
                        NotificationType.FRIEND_ACCEPTED, fromUserId, toUserId, null));
                log.info("Friendship auto-accepted between={} and={}", fromUserId, toUserId);
                tx.commit();
                log.debug("sendRequest: tx committed");
                return;
            }
            if (repo.findPending(fromUserId, toUserId).isPresent()) {
                log.warn("sendRequest denied: already pending from={} to={}", fromUserId, toUserId);
                throw new IllegalStateException("The application has already been submitted");
            }
            User from = users.findById(fromUserId).orElseThrow();
            User to = users.findById(toUserId).orElseThrow();
            FriendRequest req = FriendRequest.of(from, to);
            repo.saveRequest(req);
            notify.notify(NotificationEvent.of(
                    NotificationType.FRIEND_REQUEST, fromUserId, toUserId, null));
            log.info("Friend request sent from={} to={}", fromUserId, toUserId);
            tx.commit();
            log.debug("sendRequest: tx committed");
        } catch (RuntimeException e) {
            tx.rollback();
            log.debug("sendRequest: tx rolled back");
            log.error("sendRequest failed from={} to={}", fromUserId, toUserId, e);
            throw e;
        }
    }

    /**
     * Accepts a pending friend request.
     *
     * <p>Removes the pending {@link FriendRequest} and creates a bidirectional
     * friendship between the involved users. Sends a {@link NotificationType#FRIEND_ACCEPTED}
     * notification to the requester.</p>
     *
     * @param toUserId   the user accepting the request
     * @param fromUserId the original requester
     * @throws IllegalStateException if no pending request exists
     */
    @Override
    public void accept(String toUserId, String fromUserId) {
        log.info("accept: from={} to={}", fromUserId, toUserId);
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        log.debug("accept: tx started");
        try {
            if (repo.findPending(fromUserId, toUserId).isEmpty()) {
                log.warn("accept denied: no pending request from={} to={}", fromUserId, toUserId);
                throw new IllegalStateException("The application was not found");
            }
            if (repo.areFriends(fromUserId, toUserId)) {
                repo.removeRequest(fromUserId, toUserId);
                log.info("accept no-op: already friends from={} to={}", fromUserId, toUserId);
                tx.commit();
                log.debug("accept: tx committed");
                return;
            }
            repo.removeRequest(fromUserId, toUserId);
            repo.addFriendship(fromUserId, toUserId);
            notify.notify(NotificationEvent.of(
                    NotificationType.FRIEND_ACCEPTED, toUserId, fromUserId, null));
            log.info("Friend request accepted from={} to={}", fromUserId, toUserId);
            tx.commit();
            log.debug("accept: tx committed");
        } catch (RuntimeException e) {
            tx.rollback();
            log.debug("accept: tx rolled back");
            log.error("accept failed from={} to={}", fromUserId, toUserId, e);
            throw e;
        }
    }

    /**
     * Declines a pending friend request.
     *
     * <p>Removes the corresponding {@link FriendRequest} without creating
     * a friendship link.</p>
     *
     * @param toUserId   recipient’s user ID
     * @param fromUserId sender’s user ID
     * @throws IllegalStateException if the request does not exist
     */
    @Override
    public void decline(String toUserId, String fromUserId) {
        log.info("decline: from={} to={}", fromUserId, toUserId);
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        log.debug("decline: tx started");
        try {
            if (repo.findPending(fromUserId, toUserId).isEmpty()) {
                log.warn("decline denied: no pending request from={} to={}", fromUserId, toUserId);
                throw new IllegalStateException("The application was not found");
            }
            repo.removeRequest(fromUserId, toUserId);
            log.info("Friend request declined from={} to={}", fromUserId, toUserId);
            tx.commit();
            log.debug("decline: tx committed");
        } catch (RuntimeException e) {
            tx.rollback();
            log.debug("decline: tx rolled back");
            log.error("decline failed from={} to={}", fromUserId, toUserId, e);
            throw e;
        }
    }

    /**
     * Cancels a previously sent friend request.
     *
     * @param fromUserId sender’s user ID
     * @param toUserId   recipient’s user ID
     * @throws IllegalStateException if no pending request exists
     */
    @Override
    public void cancel(String fromUserId, String toUserId) {
        log.info("cancel: from={} to={}", fromUserId, toUserId);
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        log.debug("cancel: tx started");
        try {
            if (repo.findPending(fromUserId, toUserId).isEmpty()) {
                log.warn("cancel denied: no pending request from={} to={}", fromUserId, toUserId);
                throw new IllegalStateException("The application was not found");
            }
            repo.removeRequest(fromUserId, toUserId);
            log.info("Friend request canceled from={} to={}", fromUserId, toUserId);
            tx.commit();
            log.debug("cancel: tx committed");
        } catch (RuntimeException e) {
            tx.rollback();
            log.debug("cancel: tx rolled back");
            log.error("cancel failed from={} to={}", fromUserId, toUserId, e);
            throw e;
        }
    }

    /**
     * Removes an existing friendship between two users.
     *
     * <p>This action is symmetric — both users are unlinked from each other’s
     * friend lists.</p>
     *
     * @param userId   user performing the action
     * @param friendId friend to remove
     * @throws IllegalStateException if the users are not friends
     */
    @Override
    public void remove(String userId, String friendId) {
        log.info("remove: user={} friend={}", userId, friendId);
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        log.debug("remove: tx started");
        try {
            if (!repo.areFriends(userId, friendId)) {
                log.warn("remove denied: not friends user={} friend={}", userId, friendId);
                throw new IllegalStateException("You are not friends");
            }
            repo.removeFriendship(userId, friendId);
            log.info("Friendship removed between={} and={}", userId, friendId);
            tx.commit();
            log.debug("remove: tx committed");
        } catch (RuntimeException e) {
            tx.rollback();
            log.debug("remove: tx rolled back");
            log.error("remove failed user={} friend={}", userId, friendId, e);
            throw e;
        }
    }

    /**
     * Returns the full list of friends for the specified user.
     *
     * @param userId user identifier
     * @return list of {@link User} friends (may be empty)
     */
    @Override
    public List<User> listFriends(String userId) {
        log.debug("listFriends: userId={}", userId);
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        log.debug("listFriends: tx started");
        try {
            List<User> res = new ArrayList<>();
            for (String fid : repo.friendsOf(userId)) {
                users.findById(fid).ifPresent(res::add);
            }
            log.debug("listFriends userId={} size={}", userId, res.size());
            tx.commit();
            log.debug("listFriends: tx committed");
            return res;
        } catch (RuntimeException e) {
            tx.rollback();
            log.debug("listFriends: tx rolled back");
            log.error("listFriends failed userId={}", userId, e);
            throw e;
        }
    }

    /**
     * Retrieves a paginated list of friends matching an optional search query.
     *
     * <p>Pagination and filtering are handled by the {@link FriendRepository}.
     * Results are wrapped in {@link DefaultUserService.PagedResult}.</p>
     *
     * @param userId user identifier
     * @param page   page number (1-based)
     * @param size   number of entries per page
     * @param q      optional query string (matched against login or name)
     * @return paginated friend list and metadata
     */
    @Override
    public DefaultUserService.PagedResult<User> listFriendsPaged(String userId, int page, int size, String q) {
        log.debug("listFriendsPaged: userId={} page={} size={} q='{}'", userId, page, size, q);
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
                log.debug("listFriendsPaged: tx started; defaultReadOnly {} -> true", prevRO);
            }
            long total = repo.countFriends(userId, q);
            List<User> items = (total == 0) ? List.of() : repo.pageFriends(userId, page, size, q);
            if (started) {
                tx.commit();
                log.debug("listFriendsPaged: tx committed");
            }
            return new DefaultUserService.PagedResult<>(items, total, Math.max(1, page), Math.max(1, size));
        } catch (RuntimeException e) {
            if (started) {
                tx.rollback();
                log.debug("listFriendsPaged: tx rolled back");
            }
            log.error("listFriendsPaged failed userId={} page={} size={}", userId, page, size, e);
            throw e;
        } finally {
            if (touchedRO) {
                session.setDefaultReadOnly(prevRO);
                log.trace("listFriendsPaged: defaultReadOnly restored to {}", prevRO);
            }
        }
    }

    /**
     * Retrieves all incoming friend requests awaiting approval.
     *
     * @param userId target user identifier
     * @return list of incoming {@link FriendRequest}s (may be empty)
     */
    @Override
    public List<FriendRequest> incoming(String userId) {
        log.debug("incoming: userId={}", userId);
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        log.debug("incoming: tx started");
        try {
            List<FriendRequest> list = repo.incoming(userId);
            log.debug("incoming requests userId={} size={}", userId, list.size());
            tx.commit();
            log.debug("incoming: tx committed");
            return list;
        } catch (RuntimeException e) {
            tx.rollback();
            log.debug("incoming: tx rolled back");
            log.error("incoming failed userId={}", userId, e);
            throw e;
        }
    }

    /**
     * Retrieves all outgoing friend requests sent by the specified user.
     *
     * @param userId sender’s user ID
     * @return list of outgoing {@link FriendRequest}s (may be empty)
     */
    @Override
    public List<FriendRequest> outgoing(String userId) {
        log.debug("outgoing: userId={}", userId);
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        log.debug("outgoing: tx started");
        try {
            List<FriendRequest> list = repo.outgoing(userId);
            log.debug("outgoing requests userId={} size={}", userId, list.size());
            tx.commit();
            log.debug("outgoing: tx committed");
            return list;
        } catch (RuntimeException e) {
            tx.rollback();
            log.debug("outgoing: tx rolled back");
            log.error("outgoing failed userId={}", userId, e);
            throw e;
        }
    }
}
