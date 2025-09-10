package com.javarush.apalinskiy.repository.social;

import com.javarush.apalinskiy.domain.social.FriendRequest;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface FriendRepository {

    boolean areFriends(String a, String b);

    void addFriendship(String a, String b);

    void removeFriendship(String a, String b);

    Set<String> friendsOf(String userId);

    Optional<FriendRequest> findPending(String from, String to);

    void saveRequest(FriendRequest req);

    void removeRequest(String from, String to);

    List<FriendRequest> incoming(String userId);

    List<FriendRequest> outgoing(String userId);
}
