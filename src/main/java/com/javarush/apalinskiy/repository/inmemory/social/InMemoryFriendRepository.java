package com.javarush.apalinskiy.repository.inmemory.social;

import com.javarush.apalinskiy.repository.social.FriendRepository;
import com.javarush.apalinskiy.domain.social.FriendRequest;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryFriendRepository implements FriendRepository {

    private final ConcurrentHashMap<String, Set<String>> friends = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Map<String, FriendRequest>> pending = new ConcurrentHashMap<>();

    @Override
    public boolean areFriends(String a, String b) {
        return friends.getOrDefault(a, Set.of()).contains(b);
    }

    @Override
    public void addFriendship(String a, String b) {
        friends.compute(a, (k, v) -> {
            Set<String> s = (v == null) ? new LinkedHashSet<>() : new LinkedHashSet<>(v);
            s.add(b);
            return s;
        });
        friends.compute(b, (k, v) -> {
            Set<String> s = (v == null) ? new LinkedHashSet<>() : new LinkedHashSet<>(v);
            s.add(a);
            return s;
        });
    }

    @Override
    public void removeFriendship(String a, String b) {
        friends.computeIfPresent(a, (k, v) -> {
            v.remove(b);
            return v;
        });
        friends.computeIfPresent(b, (k, v) -> {
            v.remove(a);
            return v;
        });
    }

    @Override
    public Set<String> friendsOf(String userId) {
        return Collections.unmodifiableSet(friends.getOrDefault(userId, Set.of()));
    }

    @Override
    public Optional<FriendRequest> findPending(String from, String to) {
        Map<String, FriendRequest> m = pending.getOrDefault(to, Map.of());
        return Optional.ofNullable(m.get(from));
    }

    @Override
    public void saveRequest(FriendRequest req) {
        pending.compute(req.getToUserId(), (k, v) -> {
            Map<String, FriendRequest> m = (v == null) ? new LinkedHashMap<>() : new LinkedHashMap<>(v);
            m.put(req.getFromUserId(), req);
            return m;
        });
    }

    @Override
    public void removeRequest(String from, String to) {
        pending.computeIfPresent(to, (k, v) -> {
            v.remove(from);
            return v;
        });
    }

    @Override
    public List<FriendRequest> incoming(String userId) {
        Map<String, FriendRequest> m = pending.getOrDefault(userId, Map.of());
        List<FriendRequest> list = new ArrayList<>(m.values());
        list.sort(Comparator.comparing(FriendRequest::getCreatedAt).reversed());
        return list;
    }

    @Override
    public List<FriendRequest> outgoing(String userId) {
        List<FriendRequest> list = new ArrayList<>();
        for (Map<String, FriendRequest> m : pending.values()) {
            for (FriendRequest r : m.values()) {
                if (r.getFromUserId().equals(userId)) list.add(r);
            }
        }
        list.sort(Comparator.comparing(FriendRequest::getCreatedAt).reversed());
        return list;
    }
}
