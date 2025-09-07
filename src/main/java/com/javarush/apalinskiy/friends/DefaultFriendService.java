package com.javarush.apalinskiy.friends;

import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.mail.NotificationEvent;
import com.javarush.apalinskiy.mail.NotificationService;
import com.javarush.apalinskiy.mail.NotificationType;
import com.javarush.apalinskiy.service.UserService;

import java.util.ArrayList;
import java.util.List;

public class DefaultFriendService implements FriendService {

    private final FriendRepository repo;
    private final UserService users;
    private final NotificationService notify;

    public DefaultFriendService(FriendRepository repo, UserService users, NotificationService notify) {
        this.repo = repo;
        this.users = users;
        this.notify = notify;
    }

    @Override
    public void sendRequest(String fromUserId, String toUserId) {
        if (fromUserId.equals(toUserId)) {
            throw new IllegalArgumentException("You can't add yourself as a friend");
        }
        users.findById(toUserId).orElseThrow(() -> new IllegalArgumentException("The user was not found"));
        if (repo.areFriends(fromUserId, toUserId)) {
            throw new IllegalStateException("You are already friends");
        }
        if (repo.findPending(toUserId, fromUserId).isPresent()) {
            repo.removeRequest(toUserId, fromUserId);
            repo.addFriendship(fromUserId, toUserId);
            notify.notify(NotificationEvent.of(
                    NotificationType.FRIEND_ACCEPTED, fromUserId, toUserId, null));
            return;
        }
        if (repo.findPending(fromUserId, toUserId).isPresent()) {
            throw new IllegalStateException("The application has already been submitted");
        }
        repo.saveRequest(FriendRequest.of(fromUserId, toUserId));
        notify.notify(NotificationEvent.of(
                NotificationType.FRIEND_REQUEST, fromUserId, toUserId, null));
    }

    @Override
    public void accept(String toUserId, String fromUserId) {
        if (repo.findPending(fromUserId, toUserId).isEmpty()) {
            throw new IllegalStateException("The application was not found");
        }
        repo.removeRequest(fromUserId, toUserId);
        repo.addFriendship(fromUserId, toUserId);
        notify.notify(NotificationEvent.of(
                NotificationType.FRIEND_ACCEPTED, toUserId, fromUserId, null));
    }

    @Override
    public void decline(String toUserId, String fromUserId) {
        if (repo.findPending(fromUserId, toUserId).isEmpty()) {
            throw new IllegalStateException("The application was not found");
        }
        repo.removeRequest(fromUserId, toUserId);
    }

    @Override
    public void cancel(String fromUserId, String toUserId) {
        if (repo.findPending(fromUserId, toUserId).isEmpty()) {
            throw new IllegalStateException("The application was not found");
        }
        repo.removeRequest(fromUserId, toUserId);
    }

    @Override
    public void remove(String userId, String friendId) {
        if (!repo.areFriends(userId, friendId)) {
            throw new IllegalStateException("You are not friends");
        }
        repo.removeFriendship(userId, friendId);
    }

    @Override
    public List<User> listFriends(String userId) {
        List<User> res = new ArrayList<>();
        for (String fid : repo.friendsOf(userId)) {
            users.findById(fid).ifPresent(res::add);
        }
        return res;
    }

    @Override
    public List<FriendRequest> incoming(String userId) {
        return repo.incoming(userId);
    }

    @Override
    public List<FriendRequest> outgoing(String userId) {
        return repo.outgoing(userId);
    }

}
