package com.javarush.apalinskiy.repository.social;

import com.javarush.apalinskiy.domain.social.FriendRequest;
import com.javarush.apalinskiy.domain.user.User;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repository interface for managing user friendships and friend requests.
 *
 * <p>This repository encapsulates all persistence operations related to the
 * {@link com.javarush.apalinskiy.domain.user.User} social graph — including
 * friendship creation, removal, search, and pending friend request management.</p>
 *
 * <p>It provides both relationship-level operations ({@code areFriends}, {@code addFriendship})
 * and {@link com.javarush.apalinskiy.domain.social.FriendRequest}–level methods for
 * handling pending requests.</p>
 */
public interface FriendRepository {

    /**
     * Checks whether two users are friends with each other.
     *
     * @param a first user ID
     * @param b second user ID
     * @return {@code true} if they are friends; {@code false} otherwise
     */
    boolean areFriends(String a, String b);

    /**
     * Establishes a bidirectional friendship link between two users.
     * <p>If one or both users do not exist, the operation is ignored.</p>
     *
     * @param a first user ID
     * @param b second user ID
     */
    void addFriendship(String a, String b);

    /**
     * Removes a bidirectional friendship link between two users.
     * <p>If they are not currently friends, the call has no effect.</p>
     *
     * @param a first user ID
     * @param b second user ID
     */
    void removeFriendship(String a, String b);

    /**
     * Returns the set of all user IDs that are friends with the given user.
     *
     * @param userId user identifier
     * @return a set of user IDs representing friends; never {@code null}
     */
    Set<String> friendsOf(String userId);

    /**
     * Finds a pending friend request from one user to another.
     *
     * @param from sender user ID
     * @param to   receiver user ID
     * @return optional pending {@link FriendRequest}, empty if none exists
     */
    Optional<FriendRequest> findPending(String from, String to);

    /**
     * Persists a new {@link FriendRequest}.
     *
     * @param req the request to save; must not be {@code null}
     */
    void saveRequest(FriendRequest req);

    /**
     * Removes an existing pending friend request between two users.
     *
     * @param from sender user ID
     * @param to   receiver user ID
     */
    void removeRequest(String from, String to);

    /**
     * Returns a list of all pending incoming friend requests for a user.
     *
     * @param userId receiver user ID
     * @return list of pending {@link FriendRequest}s
     */
    List<FriendRequest> incoming(String userId);

    /**
     * Returns a list of all pending outgoing friend requests for a user.
     *
     * @param userId sender user ID
     * @return list of pending {@link FriendRequest}s
     */
    List<FriendRequest> outgoing(String userId);

    /**
     * Counts the total number of friends for a user,
     * optionally filtered by search query (username, login, or ID).
     *
     * @param userId user identifier
     * @param q      optional case-insensitive search string; may be {@code null} or blank
     * @return total number of matching friends
     */
    long countFriends(String userId, String q);

    /**
     * Retrieves a paginated list of a user's friends, optionally filtered by search query.
     * <p>Ordering is typically ascending by username, then by user ID.</p>
     *
     * @param userId user identifier
     * @param page   page number (1-based)
     * @param size   number of records per page
     * @param q      optional case-insensitive search string; may be {@code null} or blank
     * @return paginated list of {@link User} entities
     */
    List<User> pageFriends(String userId, int page, int size, String q);
}
