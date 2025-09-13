package com.javarush.apalinskiy.repository.social;

import com.javarush.apalinskiy.domain.social.FriendRequest;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repository interface for managing friendships and friend requests.
 * <p>
 * Provides operations to:
 * <ul>
 *   <li>check and manage established friendships,</li>
 *   <li>store and resolve pending {@link FriendRequest}s,</li>
 *   <li>list incoming and outgoing requests for a user.</li>
 * </ul>
 *
 * <h3>Contract</h3>
 * <ul>
 *   <li>Friendship is symmetric: if A is a friend of B, then B is a friend of A.</li>
 *   <li>Friend requests are directional: from → to.</li>
 *   <li>Implementations should preserve insertion or creation order when listing
 *       requests and return them sorted (typically by {@link FriendRequest#getCreatedAt()}).</li>
 *   <li>Implementations must be thread-safe if used concurrently.</li>
 * </ul>
 */
public interface FriendRepository {

    /**
     * Checks whether two users are friends.
     *
     * @param a first user ID
     * @param b second user ID
     * @return true if they are friends, false otherwise
     */
    boolean areFriends(String a, String b);

    /**
     * Creates a mutual friendship between two users.
     * <p>
     * After this call, {@code areFriends(a, b)} must return {@code true}.
     * </p>
     *
     * @param a first user ID
     * @param b second user ID
     */
    void addFriendship(String a, String b);

    /**
     * Removes an existing friendship between two users.
     *
     * @param a first user ID
     * @param b second user ID
     */
    void removeFriendship(String a, String b);

    /**
     * Returns all friends of the given user.
     *
     * @param userId user ID
     * @return immutable set of friend IDs (never {@code null})
     */
    Set<String> friendsOf(String userId);

    /**
     * Finds a pending friend request from one user to another.
     *
     * @param from requester user ID
     * @param to   target user ID
     * @return optional containing the request if found
     */
    Optional<FriendRequest> findPending(String from, String to);

    /**
     * Saves a new friend request.
     * <p>
     * If a request already exists from the same {@code from → to},
     * the implementation may overwrite or reject it.
     * </p>
     *
     * @param req friend request to save
     */
    void saveRequest(FriendRequest req);

    /**
     * Removes a pending friend request from one user to another.
     *
     * @param from requester user ID
     * @param to   target user ID
     */
    void removeRequest(String from, String to);

    /**
     * Returns a list of pending requests received by the given user.
     *
     * @param userId user ID
     * @return list of requests sorted by submission time (newest first)
     */
    List<FriendRequest> incoming(String userId);

    /**
     * Returns a list of pending requests sent by the given user.
     *
     * @param userId user ID
     * @return list of requests sorted by submission time (newest first)
     */
    List<FriendRequest> outgoing(String userId);
}
