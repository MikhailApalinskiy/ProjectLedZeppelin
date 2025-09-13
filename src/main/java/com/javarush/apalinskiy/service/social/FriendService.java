package com.javarush.apalinskiy.service.social;

import com.javarush.apalinskiy.domain.social.FriendRequest;
import com.javarush.apalinskiy.domain.user.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service for managing friendships and friend requests between users.
 * <p>
 * Provides operations for sending, accepting, declining, and canceling
 * friend requests, as well as removing existing friendships. Also
 * exposes methods to list friends and pending requests.
 * </p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Initiate and manage friend requests between users.</li>
 *   <li>Establish and remove friendships.</li>
 *   <li>Provide access to lists of friends, incoming, and outgoing requests.</li>
 *   <li>Offer convenience utilities for retrieving friend identifiers.</li>
 * </ul>
 */
public interface FriendService {

    /**
     * Sends a friend request from one user to another.
     *
     * @param fromUserId ID of the user initiating the request
     * @param toUserId   ID of the target user
     * @throws IllegalArgumentException if the request is invalid
     * @throws IllegalStateException    if a friendship or pending request already exists
     */
    void sendRequest(String fromUserId, String toUserId);

    /**
     * Accepts a pending friend request.
     *
     * @param toUserId   ID of the user receiving the request
     * @param fromUserId ID of the user who sent the request
     * @throws IllegalStateException if no matching request exists
     */
    void accept(String toUserId, String fromUserId);

    /**
     * Declines a pending friend request.
     *
     * @param toUserId   ID of the user receiving the request
     * @param fromUserId ID of the user who sent the request
     * @throws IllegalStateException if no matching request exists
     */
    void decline(String toUserId, String fromUserId);

    /**
     * Cancels an outgoing friend request.
     *
     * @param fromUserId ID of the user who sent the request
     * @param toUserId   ID of the target user
     * @throws IllegalStateException if no matching request exists
     */
    void cancel(String fromUserId, String toUserId);

    /**
     * Removes an existing friendship between two users.
     *
     * @param userId   ID of the user performing the removal
     * @param friendId ID of the friend to be removed
     * @throws IllegalStateException if the users are not friends
     */
    void remove(String userId, String friendId);

    /**
     * Returns a list of the user's current friends.
     *
     * @param userId user identifier
     * @return list of users who are friends with the given user
     */
    List<User> listFriends(String userId);

    /**
     * Returns a list of incoming friend requests for the user.
     *
     * @param userId user identifier
     * @return list of pending friend requests received by the user
     */
    List<FriendRequest> incoming(String userId);

    /**
     * Returns a list of outgoing friend requests for the user.
     *
     * @param userId user identifier
     * @return list of friend requests sent by the user
     */
    List<FriendRequest> outgoing(String userId);

    /**
     * Convenience method to extract friend IDs instead of full {@link User} objects.
     *
     * @param userId user identifier
     * @return list of friend user IDs, never {@code null}
     */
    default List<String> listFriendIds(String userId) {
        List<User> friends = listFriends(userId);
        if (friends == null || friends.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> ids = new ArrayList<>(friends.size());
        for (User u : friends) {
            if (u != null && u.getUserId() != null && !u.getUserId().isBlank()) {
                ids.add(u.getUserId());
            }
        }
        return ids;
    }
}
