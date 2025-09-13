package com.javarush.apalinskiy.service.impl.social;

import com.javarush.apalinskiy.repository.social.FriendRepository;
import com.javarush.apalinskiy.domain.social.FriendRequest;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.domain.notify.NotificationEvent;
import com.javarush.apalinskiy.service.social.FriendService;
import com.javarush.apalinskiy.service.notify.NotificationService;
import com.javarush.apalinskiy.domain.notify.NotificationType;
import com.javarush.apalinskiy.service.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of {@link FriendService}.
 * <p>
 * Provides friend request management and friendship operations backed by a {@link FriendRepository}.
 * Also integrates with {@link NotificationService} to notify users of changes
 * (requests, acceptances, removals, etc.).
 * </p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Send, accept, decline, and cancel friend requests.</li>
 *   <li>Add and remove friendships in the repository.</li>
 *   <li>Provide access to lists of friends and pending requests.</li>
 *   <li>Generate appropriate {@link NotificationEvent} entries on state changes.</li>
 * </ul>
 *
 * <p><b>Note:</b> All data is stored in the provided repository implementation
 * and may be in-memory or persistent depending on the repository used.</p>
 */
public class DefaultFriendService implements FriendService {

    private static final Logger log = LoggerFactory.getLogger(DefaultFriendService.class);

    private final FriendRepository repo;
    private final UserService users;
    private final NotificationService notify;

    /**
     * Creates a new friend service.
     *
     * @param repo   the repository for friend data
     * @param users  the user service used to validate user existence
     * @param notify the notification service for generating user notifications
     */
    public DefaultFriendService(FriendRepository repo, UserService users, NotificationService notify) {
        this.repo = repo;
        this.users = users;
        this.notify = notify;
    }

    /**
     * Send a friend request from one user to another.
     * <ul>
     *   <li>Throws if the target user does not exist, if users are already friends,
     *       or if a request is already pending.</li>
     *   <li>If the reverse request already exists, the friendship is auto-accepted.</li>
     * </ul>
     *
     * @param fromUserId the ID of the requesting user
     * @param toUserId   the ID of the target user
     */
    @Override
    public void sendRequest(String fromUserId, String toUserId) {
        if (fromUserId.equals(toUserId)) {
            log.warn("sendRequest denied: user tried to add self userId={}", fromUserId);
            throw new IllegalArgumentException("You can't add yourself as a friend");
        }
        users.findById(toUserId).orElseThrow(() -> {
            log.warn("sendRequest denied: target user not found from={} to={}", fromUserId, toUserId);
            return new IllegalArgumentException("The user was not found");
        });
        if (repo.areFriends(fromUserId, toUserId)) {
            log.warn("sendRequest denied: already friends from={} to={}", fromUserId, toUserId);
            throw new IllegalStateException("You are already friends");
        }
        if (repo.findPending(toUserId, fromUserId).isPresent()) {
            repo.removeRequest(toUserId, fromUserId);
            repo.addFriendship(fromUserId, toUserId);
            notify.notify(NotificationEvent.of(
                    NotificationType.FRIEND_ACCEPTED, fromUserId, toUserId, null));
            log.info("Friendship auto-accepted between={} and={}", fromUserId, toUserId);
            return;
        }
        if (repo.findPending(fromUserId, toUserId).isPresent()) {
            log.warn("sendRequest denied: already pending from={} to={}", fromUserId, toUserId);
            throw new IllegalStateException("The application has already been submitted");
        }
        repo.saveRequest(FriendRequest.of(fromUserId, toUserId));
        notify.notify(NotificationEvent.of(
                NotificationType.FRIEND_REQUEST, fromUserId, toUserId, null));
        log.info("Friend request sent from={} to={}", fromUserId, toUserId);
    }

    /**
     * Accept a pending friend request.
     *
     * @param toUserId   the recipient of the request
     * @param fromUserId the sender of the request
     * @throws IllegalStateException if no pending request exists
     */
    @Override
    public void accept(String toUserId, String fromUserId) {
        if (repo.findPending(fromUserId, toUserId).isEmpty()) {
            log.warn("accept denied: no pending request from={} to={}", fromUserId, toUserId);
            throw new IllegalStateException("The application was not found");
        }
        repo.removeRequest(fromUserId, toUserId);
        repo.addFriendship(fromUserId, toUserId);
        notify.notify(NotificationEvent.of(
                NotificationType.FRIEND_ACCEPTED, toUserId, fromUserId, null));
        log.info("Friend request accepted from={} to={}", fromUserId, toUserId);
    }

    /**
     * Decline a pending friend request.
     *
     * @param toUserId   the recipient of the request
     * @param fromUserId the sender of the request
     * @throws IllegalStateException if no pending request exists
     */
    @Override
    public void decline(String toUserId, String fromUserId) {
        if (repo.findPending(fromUserId, toUserId).isEmpty()) {
            log.warn("decline denied: no pending request from={} to={}", fromUserId, toUserId);
            throw new IllegalStateException("The application was not found");
        }
        repo.removeRequest(fromUserId, toUserId);
        log.info("Friend request declined from={} to={}", fromUserId, toUserId);
    }

    /**
     * Cancel a pending outgoing friend request.
     *
     * @param fromUserId the sender of the request
     * @param toUserId   the recipient of the request
     * @throws IllegalStateException if no pending request exists
     */
    @Override
    public void cancel(String fromUserId, String toUserId) {
        if (repo.findPending(fromUserId, toUserId).isEmpty()) {
            log.warn("cancel denied: no pending request from={} to={}", fromUserId, toUserId);
            throw new IllegalStateException("The application was not found");
        }
        repo.removeRequest(fromUserId, toUserId);
        log.info("Friend request canceled from={} to={}", fromUserId, toUserId);
    }

    /**
     * Remove an existing friendship between two users.
     *
     * @param userId   the ID of one user
     * @param friendId the ID of the other user
     * @throws IllegalStateException if the users are not friends
     */
    @Override
    public void remove(String userId, String friendId) {
        if (!repo.areFriends(userId, friendId)) {
            log.warn("remove denied: not friends user={} friend={}", userId, friendId);
            throw new IllegalStateException("You are not friends");
        }
        repo.removeFriendship(userId, friendId);
        log.info("Friendship removed between={} and={}", userId, friendId);
    }

    /**
     * List all friends of a given user.
     *
     * @param userId the user ID
     * @return a list of {@link User} objects representing the user's friends
     */
    @Override
    public List<User> listFriends(String userId) {
        List<User> res = new ArrayList<>();
        for (String fid : repo.friendsOf(userId)) {
            users.findById(fid).ifPresent(res::add);
        }
        log.debug("listFriends userId={} size={}", userId, res.size());
        return res;
    }

    /**
     * List all incoming friend requests for a given user.
     *
     * @param userId the user ID
     * @return list of pending {@link FriendRequest}s received
     */
    @Override
    public List<FriendRequest> incoming(String userId) {
        List<FriendRequest> list = repo.incoming(userId);
        log.debug("incoming requests userId={} size={}", userId, list.size());
        return list;
    }

    /**
     * List all outgoing friend requests for a given user.
     *
     * @param userId the user ID
     * @return list of pending {@link FriendRequest}s sent
     */
    @Override
    public List<FriendRequest> outgoing(String userId) {
        List<FriendRequest> list = repo.outgoing(userId);
        log.debug("outgoing requests userId={} size={}", userId, list.size());
        return list;
    }
}
