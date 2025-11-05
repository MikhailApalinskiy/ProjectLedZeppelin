package com.javarush.apalinskiy.service.social;

import com.javarush.apalinskiy.domain.social.FriendRequest;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.service.impl.user.DefaultUserService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service interface for managing user friendships and friend requests.
 *
 * <p>This interface defines operations for sending, accepting, declining,
 * and canceling friend requests, as well as managing established friendships.
 * Implementations are responsible for ensuring proper validation and access control.</p>
 */
public interface FriendService {

    /**
     * Sends a new friend request from one user to another.
     *
     * @param fromUserId ID of the user initiating the request
     * @param toUserId   ID of the recipient user
     */
    void sendRequest(String fromUserId, String toUserId);

    /**
     * Accepts a pending friend request.
     *
     * @param toUserId   ID of the user accepting the request
     * @param fromUserId ID of the user who originally sent the request
     */
    void accept(String toUserId, String fromUserId);

    /**
     * Declines a pending friend request.
     *
     * @param toUserId   ID of the user declining the request
     * @param fromUserId ID of the user who originally sent the request
     */
    void decline(String toUserId, String fromUserId);

    /**
     * Cancels an outgoing friend request.
     *
     * @param fromUserId ID of the user canceling the request
     * @param toUserId   ID of the target user who received the request
     */
    void cancel(String fromUserId, String toUserId);

    /**
     * Removes an existing friendship between two users.
     *
     * @param userId   ID of the user performing the removal
     * @param friendId ID of the friend to remove
     */
    void remove(String userId, String friendId);

    /**
     * Returns the list of confirmed friends for the given user.
     *
     * @param userId ID of the user whose friends are being listed
     * @return list of {@link User} objects representing friends
     */
    List<User> listFriends(String userId);

    /**
     * Returns the list of incoming friend requests (awaiting user approval).
     *
     * @param userId ID of the user whose incoming requests are being retrieved
     * @return list of {@link FriendRequest} objects representing received requests
     */
    List<FriendRequest> incoming(String userId);

    /**
     * Returns the list of outgoing friend requests (sent by the user).
     *
     * @param userId ID of the user whose outgoing requests are being retrieved
     * @return list of {@link FriendRequest} objects representing sent requests
     */
    List<FriendRequest> outgoing(String userId);

    /**
     * Returns a paginated list of the user’s friends.
     *
     * <p>This method supports optional search filtering and pagination.
     * It can be used to efficiently display friends lists in UIs or APIs.</p>
     *
     * @param userId user identifier
     * @param page   current page number (1-based)
     * @param size   number of results per page
     * @param q      optional search query for filtering by name or ID
     * @return paged result of {@link User} objects
     */
    DefaultUserService.PagedResult<User> listFriendsPaged(String userId, int page, int size, String q);

    /**
     * Returns a list of friend IDs for the given user.
     *
     * <p>This is a convenience default implementation based on {@link #listFriends(String)}.
     * It extracts user IDs from the full friend objects, filtering out invalid entries.</p>
     *
     * @param userId user identifier
     * @return list of user IDs corresponding to confirmed friends
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
