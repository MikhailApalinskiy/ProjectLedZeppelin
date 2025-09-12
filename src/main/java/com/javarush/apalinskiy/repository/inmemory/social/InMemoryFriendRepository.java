package com.javarush.apalinskiy.repository.inmemory.social;

import com.javarush.apalinskiy.repository.social.FriendRepository;
import com.javarush.apalinskiy.domain.social.FriendRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryFriendRepository implements FriendRepository {

    private static final Logger log = LoggerFactory.getLogger(InMemoryFriendRepository.class);

    private final ConcurrentHashMap<String, Set<String>> friends = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Map<String, FriendRequest>> pending = new ConcurrentHashMap<>();

    @Override
    public boolean areFriends(String a, String b) {
        boolean ok = friends.getOrDefault(a, Set.of()).contains(b);
        log.debug("areFriends a={} b={} -> {}", a, b, ok);
        return ok;
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
        log.debug("addFriendship a={} b={} (now friends)", a, b);
    }

    @Override
    public void removeFriendship(String a, String b) {
        boolean[] removed = {false, false};
        friends.computeIfPresent(a, (k, v) -> {
            removed[0] = v.remove(b);
            return v;
        });
        friends.computeIfPresent(b, (k, v) -> {
            removed[1] = v.remove(a);
            return v;
        });
        if (!(removed[0] || removed[1])) {
            log.warn("removeFriendship skipped: not friends a={} b={}", a, b);
        } else {
            log.debug("removeFriendship a={} b={} (removed)", a, b);
        }
    }

    @Override
    public Set<String> friendsOf(String userId) {
        Set<String> res = Collections.unmodifiableSet(friends.getOrDefault(userId, Set.of()));
        log.debug("friendsOf userId={} size={}", userId, res.size());
        return res;
    }

    @Override
    public Optional<FriendRequest> findPending(String from, String to) {
        Map<String, FriendRequest> m = pending.getOrDefault(to, Map.of());
        Optional<FriendRequest> r = Optional.ofNullable(m.get(from));
        log.debug("findPending from={} to={} found={}", from, to, r.isPresent());
        return r;
    }

    @Override
    public void saveRequest(FriendRequest req) {
        pending.compute(req.getToUserId(), (k, v) -> {
            Map<String, FriendRequest> m = (v == null) ? new LinkedHashMap<>() : new LinkedHashMap<>(v);
            boolean existed = m.containsKey(req.getFromUserId());
            m.put(req.getFromUserId(), req);
            if (existed) {
                log.warn("saveRequest overwritten existing request from={} to={}", req.getFromUserId(), req.getToUserId());
            } else {
                log.debug("saveRequest from={} to={}", req.getFromUserId(), req.getToUserId());
            }
            return m;
        });
    }

    @Override
    public void removeRequest(String from, String to) {
        final boolean[] removed = {false};
        pending.computeIfPresent(to, (k, v) -> {
            removed[0] = (v.remove(from) != null);
            return v;
        });
        if (removed[0]) {
            log.debug("removeRequest from={} to={} (removed)", from, to);
        } else {
            log.warn("removeRequest skipped: not found from={} to={}", from, to);
        }
    }

    @Override
    public List<FriendRequest> incoming(String userId) {
        Map<String, FriendRequest> m = pending.getOrDefault(userId, Map.of());
        List<FriendRequest> list = new ArrayList<>(m.values());
        list.sort(Comparator.comparing(FriendRequest::getCreatedAt).reversed());
        log.debug("incoming userId={} size={}", userId, list.size());
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
        log.debug("outgoing userId={} size={}", userId, list.size());
        return list;
    }
}
