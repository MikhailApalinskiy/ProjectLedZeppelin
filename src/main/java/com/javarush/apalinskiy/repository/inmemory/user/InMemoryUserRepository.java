package com.javarush.apalinskiy.repository.inmemory.user;

import com.javarush.apalinskiy.repository.user.UserRepository;
import com.javarush.apalinskiy.exceptions.DuplicateIdException;
import com.javarush.apalinskiy.exceptions.DuplicateLoginException;
import com.javarush.apalinskiy.domain.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryUserRepository implements UserRepository {

    private static final Logger log = LoggerFactory.getLogger(InMemoryUserRepository.class);

    private final ConcurrentHashMap<String, User> byId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> idByLogin = new ConcurrentHashMap<>();

    @Override
    public Optional<User> findByLogin(String userLogin) {
        final String key = userLogin.trim().toLowerCase(Locale.ROOT);
        String id = idByLogin.get(key);
        Optional<User> res = (id == null) ? Optional.empty() : Optional.ofNullable(byId.get(id));
        log.debug("findByLogin login='{}' found={}", key, res.isPresent());
        return res;
    }

    @Override
    public Optional<User> findById(String id) {
        Optional<User> res = Optional.ofNullable(byId.get(id));
        log.debug("findById id={} found={}", id, res.isPresent());
        return res;
    }

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