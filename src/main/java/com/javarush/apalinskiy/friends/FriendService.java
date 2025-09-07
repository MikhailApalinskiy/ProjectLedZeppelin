package com.javarush.apalinskiy.friends;

import com.javarush.apalinskiy.domain.user.User;

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
}
