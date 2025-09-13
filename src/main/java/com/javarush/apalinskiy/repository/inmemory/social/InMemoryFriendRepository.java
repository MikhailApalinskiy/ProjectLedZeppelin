package com.javarush.apalinskiy.repository.inmemory.social;

import com.javarush.apalinskiy.repository.social.FriendRepository;
import com.javarush.apalinskiy.domain.social.FriendRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of {@link FriendRepository}.
 * <p>
 * Stores friendships and friend requests using concurrent maps.
 * Designed for testing, prototyping, or applications without persistent storage,
 * as all data is lost when the JVM stops.
 * </p>
 *
 * <h3>Storage</h3>
 * <ul>
 *   <li>{@link #friends} – mapping of userId → set of friend userIds.</li>
 *   <li>{@link #pending} – mapping of target userId → (fromUserId → {@link FriendRequest}).</li>
 * </ul>
 *
 * <h3>Supported operations</h3>
 * <ul>
 *   <li>{@link #areFriends(String, String)} – checks if two users are friends.</li>
 *   <li>{@link #addFriendship(String, String)} – establishes a mutual friendship.</li>
 *   <li>{@link #removeFriendship(String, String)} – removes a friendship.</li>
 *   <li>{@link #friendsOf(String)} – retrieves the friend list of a user.</li>
 *   <li>{@link #findPending(String, String)} – finds a pending friend request between two users.</li>
 *   <li>{@link #saveRequest(FriendRequest)} – saves (or overwrites) a friend request.</li>
 *   <li>{@link #removeRequest(String, String)} – removes a friend request.</li>
 *   <li>{@link #incoming(String)} – lists friend requests received by a user.</li>
 *   <li>{@link #outgoing(String)} – lists friend requests sent by a user.</li>
 * </ul>
 *
 * <h3>Logging</h3>
 * <ul>
 *   <li>DEBUG: normal operations (e.g. friendship added, request saved).</li>
 *   <li>WARN: invalid or skipped operations (e.g. removing non-existent friendship).</li>
 * </ul>
 */
public class InMemoryFriendRepository implements FriendRepository {

    private static final Logger log = LoggerFactory.getLogger(InMemoryFriendRepository.class);

    /**
     * Friendships: userId → set of friend userIds.
     */
    private final ConcurrentHashMap<String, Set<String>> friends = new ConcurrentHashMap<>();
    /**
     * Pending requests: targetUserId → (fromUserId → request).
     */
    private final ConcurrentHashMap<String, Map<String, FriendRequest>> pending = new ConcurrentHashMap<>();

    /**
     * Checks if two users are friends.
     *
     * @param a first user ID
     * @param b second user ID
     * @return true if {@code a} and {@code b} are friends
     */
    @Override
    public boolean areFriends(String a, String b) {
        boolean ok = friends.getOrDefault(a, Set.of()).contains(b);
        log.debug("areFriends a={} b={} -> {}", a, b, ok);
        return ok;
    }

    /**
     * Adds a mutual friendship between two users.
     *
     * @param a first user ID
     * @param b second user ID
     */
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

    /**
     * Removes a mutual friendship between two users.
     * <p>
     * Logs a warning if the users are not friends.
     * </p>
     *
     * @param a first user ID
     * @param b second user ID
     */
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

    /**
     * Returns all friends of the given user.
     *
     * @param userId user ID
     * @return unmodifiable set of friends (never null)
     */
    @Override
    public Set<String> friendsOf(String userId) {
        Set<String> res = Collections.unmodifiableSet(friends.getOrDefault(userId, Set.of()));
        log.debug("friendsOf userId={} size={}", userId, res.size());
        return res;
    }

    /**
     * Finds a pending friend request from one user to another.
     *
     * @param from sender user ID
     * @param to   recipient user ID
     * @return optional containing the request if found
     */
    @Override
    public Optional<FriendRequest> findPending(String from, String to) {
        Map<String, FriendRequest> m = pending.getOrDefault(to, Map.of());
        Optional<FriendRequest> r = Optional.ofNullable(m.get(from));
        log.debug("findPending from={} to={} found={}", from, to, r.isPresent());
        return r;
    }

    /**
     * Saves a friend request (overwrites existing if present).
     *
     * @param req friend request to save
     */
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

    /**
     * Removes a pending friend request.
     *
     * @param from sender user ID
     * @param to   recipient user ID
     */
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

    /**
     * Returns all incoming friend requests for the given user, sorted by time (newest first).
     *
     * @param userId user ID
     * @return list of incoming friend requests
     */
    @Override
    public List<FriendRequest> incoming(String userId) {
        Map<String, FriendRequest> m = pending.getOrDefault(userId, Map.of());
        List<FriendRequest> list = new ArrayList<>(m.values());
        list.sort(Comparator.comparing(FriendRequest::getCreatedAt).reversed());
        log.debug("incoming userId={} size={}", userId, list.size());
        return list;
    }

    /**
     * Returns all outgoing friend requests made by the given user, sorted by time (newest first).
     *
     * @param userId user ID
     * @return list of outgoing friend requests
     */
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
