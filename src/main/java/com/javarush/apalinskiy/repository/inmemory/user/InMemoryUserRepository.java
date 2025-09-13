package com.javarush.apalinskiy.repository.inmemory.user;

import com.javarush.apalinskiy.repository.user.UserRepository;
import com.javarush.apalinskiy.exceptions.DuplicateIdException;
import com.javarush.apalinskiy.exceptions.DuplicateLoginException;
import com.javarush.apalinskiy.domain.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of {@link UserRepository}.
 * <p>
 * Stores users in concurrent hash maps, indexed by both {@code userId} and {@code userLogin}.
 * This implementation is thread-safe at the method level (using {@code synchronized} blocks
 * where necessary), but all data is stored only in memory and is lost when the JVM stops.
 * </p>
 *
 * <h3>Storage</h3>
 * <ul>
 *   <li>{@link #byId} – mapping of userId → {@link User}.</li>
 *   <li>{@link #idByLogin} – mapping of login → userId (for quick lookup by login).</li>
 * </ul>
 *
 * <h3>Exceptions</h3>
 * <ul>
 *   <li>{@link DuplicateLoginException} – thrown when a login is already taken.</li>
 *   <li>{@link DuplicateIdException} – thrown when a userId is already used.</li>
 *   <li>{@link NoSuchElementException} – thrown when updating a non-existent user.</li>
 * </ul>
 *
 * <h3>Logging</h3>
 * <ul>
 *   <li>DEBUG: successful operations.</li>
 *   <li>WARN: failed or invalid operations (e.g., duplicate login, missing user).</li>
 * </ul>
 *
 * <h3>Limitations</h3>
 * <ul>
 *   <li>Not suitable for production: no persistence, all data is volatile.</li>
 *   <li>Concurrent access is limited to method-level synchronization.</li>
 * </ul>
 */
public class InMemoryUserRepository implements UserRepository {

    private static final Logger log = LoggerFactory.getLogger(InMemoryUserRepository.class);

    /** Users indexed by ID. */
    private final ConcurrentHashMap<String, User> byId = new ConcurrentHashMap<>();
    /** Mapping of login → userId. */
    private final ConcurrentHashMap<String, String> idByLogin = new ConcurrentHashMap<>();

    /**
     * Finds a user by login (case-insensitive, trimmed).
     *
     * @param userLogin login of the user
     * @return optional containing the user if found
     */
    @Override
    public Optional<User> findByLogin(String userLogin) {
        final String key = userLogin.trim().toLowerCase(Locale.ROOT);
        String id = idByLogin.get(key);
        Optional<User> res = (id == null) ? Optional.empty() : Optional.ofNullable(byId.get(id));
        log.debug("findByLogin login='{}' found={}", key, res.isPresent());
        return res;
    }

    /**
     * Finds a user by ID.
     *
     * @param id user ID
     * @return optional containing the user if found
     */
    @Override
    public Optional<User> findById(String id) {
        Optional<User> res = Optional.ofNullable(byId.get(id));
        log.debug("findById id={} found={}", id, res.isPresent());
        return res;
    }

    /**
     * Saves a new user.
     * <p>
     * Enforces uniqueness of both {@code userId} and {@code userLogin}.
     * </p>
     *
     * @param user user to save
     * @throws DuplicateLoginException if login is already taken
     * @throws DuplicateIdException    if ID is already taken
     */
    @Override
    public void save(User user) {
        final String id = user.getUserId();
        final String login = user.getUserLogin();
        synchronized (this) {
            if (idByLogin.containsKey(login)) {
                log.warn("save denied: duplicate login login='{}'", login);
                throw new DuplicateLoginException("Login already exists: " + login);
            }
            if (byId.containsKey(id)) {
                log.warn("save denied: duplicate id id={}", id);
                throw new DuplicateIdException("UserId already exists: " + id);
            }
            idByLogin.put(login, id);
            byId.put(id, user);
        }
        log.debug("save ok id={} login='{}'", id, login);
    }

    /**
     * Updates an existing user.
     * <p>
     * If the login is changed, ensures the new login is not already taken.
     * </p>
     *
     * @param user user with updated fields
     * @throws NoSuchElementException  if user does not exist
     * @throws DuplicateLoginException if new login is already taken by another user
     */
    @Override
    public void update(User user) {
        final String id = user.getUserId();
        final String newLogin = user.getUserLogin();
        synchronized (this) {
            User existing = byId.get(id);
            if (existing == null) {
                log.warn("update denied: user not found id={}", id);
                throw new NoSuchElementException("User not found: " + id);
            }
            String oldLogin = existing.getUserLogin();
            if (!oldLogin.equals(newLogin)) {
                String occupiedBy = idByLogin.get(newLogin);
                if (occupiedBy != null && !occupiedBy.equals(id)) {
                    log.warn("update denied: duplicate login newLogin='{}' occupiedBy={}", newLogin, occupiedBy);
                    throw new DuplicateLoginException("Login already exists: " + newLogin);
                }
                idByLogin.remove(oldLogin);
                idByLogin.put(newLogin, id);
                log.debug("update login changed id={} from='{}' to='{}'", id, oldLogin, newLogin);
            }
            byId.put(id, user);
        }
        log.debug("update ok id={} login='{}'", id, newLogin);
    }

    /**
     * Returns all users sorted by creation time (newest first),
     * then by login.
     *
     * @return unmodifiable list of all users
     */
    @Override
    public List<User> findAll() {
        List<User> list = new ArrayList<>(byId.values());
        list.sort(Comparator
                .comparing(User::getCreatedAt).reversed()
                .thenComparing(User::getUserLogin));
        log.debug("findAll size={}", list.size());
        return Collections.unmodifiableList(list);
    }
}