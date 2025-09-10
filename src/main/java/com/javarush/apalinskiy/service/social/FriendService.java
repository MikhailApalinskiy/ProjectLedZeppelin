package com.javarush.apalinskiy.service.social;

import com.javarush.apalinskiy.domain.social.FriendRequest;
import com.javarush.apalinskiy.domain.user.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public interface FriendService {

    void sendRequest(String fromUserId, String toUserId);

    void accept(String toUserId, String fromUserId);

    void decline(String toUserId, String fromUserId);

    void cancel(String fromUserId, String toUserId);

    void remove(String userId, String friendId);

    List<User> listFriends(String userId);

    List<FriendRequest> incoming(String userId);

    List<FriendRequest> outgoing(String userId);

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
