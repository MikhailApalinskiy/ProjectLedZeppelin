package com.javarush.apalinskiy.service.impl.user;

import com.javarush.apalinskiy.utils.HibernateUtil;
import com.javarush.apalinskiy.exceptions.DuplicateIdException;
import com.javarush.apalinskiy.exceptions.DuplicateLoginException;
import com.javarush.apalinskiy.repository.user.UserRepository;
import com.javarush.apalinskiy.domain.user.Role;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.service.user.UserService;
import com.javarush.apalinskiy.web.util.Web;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Default implementation of {@link UserService} backed by Hibernate and a {@link UserRepository}.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>User registration with unique {@code userId} retry</li>
 *   <li>Login (credentials check), profile and password updates</li>
 *   <li>Admin-side updates (role, login, name, password)</li>
 *   <li>Paginated listing and lookups by id/login</li>
 * </ul>
 *
 * <p>Transaction model:</p>
 * <ul>
 *   <li>Read operations run in read-only sessions for safety/perf</li>
 *   <li>Write operations open and commit their own transactions</li>
 *   <li>Rollback on any runtime exception</li>
 * </ul>
 *
 * <p><b>Security notes:</b> It is strongly recommended to store passwords hashed
 * (e.g. BCrypt/Argon2) and compare using a time-constant method.</p>
 */
public class DefaultUserService implements UserService {

    private static final Logger log = LoggerFactory.getLogger(DefaultUserService.class);
    private static final int MAX_ID_RETRIES = 3;

    private final SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
    private final UserRepository users;

    public DefaultUserService(UserRepository users) {
        this.users = users;
    }

    /**
     * Returns a page of users with total count.
     *
     * <p>Executes a read-only transaction. Page and size are sanitized to be &ge; 1.</p>
     *
     * @param page 1-based page index
     * @param size page size
     * @return paginated result containing items, total, page and size
     * @throws RuntimeException on database errors
     */
    @Override
    public PagedResult<User> findPage(int page, int size) {
        log.debug("findPage: page={} size={}", page, size);
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.getTransaction();
        boolean started = false;
        boolean touchedRO = false;
        boolean prevRO = false;
        try {
            if (!tx.isActive()) {
                tx = session.beginTransaction();
                started = true;
                prevRO = session.isDefaultReadOnly();
                session.setDefaultReadOnly(true);
                touchedRO = true;
                log.debug("findPage: tx started; defaultReadOnly {} -> true", prevRO);
            }
            long total = users.countAll();
            List<User> items = users.findPage(page, size);
            if (started) {
                tx.commit();
                log.debug("findPage: tx committed");
            }
            log.debug("findPage: total={} items={}", total, items.size());
            return new PagedResult<>(items, total, Math.max(1, page), Math.max(1, size));
        } catch (RuntimeException e) {
            if (started) {
                tx.rollback();
                log.debug("findPage: tx rolled back");
            }
            throw e;
        } finally {
            if (touchedRO) {
                session.setDefaultReadOnly(prevRO);
                log.trace("findPage: defaultReadOnly restored to {}", prevRO);
            }
        }
    }

    /**
     * Registers a new user.
     *
     * <p>Generates a UUID, persists the user and, in case of a primary key collision,
     * retries up to {@code MAX_ID_RETRIES}. Duplicate login is reported as
     * {@link DuplicateLoginException}. On success returns the persisted {@link User}.</p>
     *
     * <p><b>Note:</b> Password should be provided already hashed or will be stored as-is.</p>
     *
     * @param role        user role (defaults to {@link Role#USER} if null inside {@link User})
     * @param name        display name (non-blank, length limits apply)
     * @param login       unique login (normalized to lower case)
     * @param rawPassword password string (should be hashed in production)
     * @return newly created user
     * @throws DuplicateLoginException if login already exists
     * @throws IllegalStateException   if unique userId couldn't be generated after retries
     * @throws RuntimeException        on database errors
     */
    @Override
    public User register(Role role, String name, String login, String rawPassword) throws DuplicateLoginException {
        log.info("register: login='{}' role={}", login, role);
        User seed = User.of(role, name, login, rawPassword);
        for (int attempt = 0; attempt < MAX_ID_RETRIES; attempt++) {
            Session session = sessionFactory.getCurrentSession();
            Transaction tx = session.beginTransaction();
            log.debug("register: attempt {}/{} - tx started", attempt + 1, MAX_ID_RETRIES);
            User candidate = (attempt == 0) ? seed : seed.withId(UUID.randomUUID().toString());
            try {
                users.save(candidate);
                tx.commit();
                log.info("User registered id={} login='{}' role={}",
                        candidate.getUserId(), candidate.getUserLogin(), role);
                log.debug("register: tx committed");
                return candidate;
            } catch (DuplicateLoginException e) {
                tx.rollback();
                log.debug("register: tx rolled back");
                log.warn("Register denied: duplicate login '{}'", candidate.getUserLogin());
                throw e;
            } catch (DuplicateIdException e) {
                tx.rollback();
                log.warn("Duplicate userId={} on register, retrying... (attempt {}/{})",
                        candidate.getUserId(), attempt + 1, MAX_ID_RETRIES);
            } catch (RuntimeException e) {
                tx.rollback();
                log.error("register failed login='{}'", login, e);
                throw e;
            }
        }
        log.error("Failed to generate unique userId for login='{}' after {} retries",
                login, MAX_ID_RETRIES);
        throw new IllegalStateException("Failed to generate unique userId after retries");
    }

    /**
     * Attempts to authenticate a user by login and password.
     *
     * <p>Runs in a read-only transaction. In production, use hashing &amp; time-constant comparison.</p>
     *
     * @param login       user login (case-insensitive)
     * @param rawPassword password to check
     * @return optional {@link User} on success; empty otherwise
     */
    @Override
    public Optional<User> login(String login, String rawPassword) {
        log.info("login: login='{}'", login);
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        log.debug("login: tx started");
        boolean prevRO = session.isDefaultReadOnly();
        try {
            session.setDefaultReadOnly(true);
            Optional<User> res = users.findByLogin(login).filter(u -> u.getPassword().equals(rawPassword));
            if (res.isPresent()) {
                log.info("User login success login='{}' id={}", login, res.get().getUserId());
            } else {
                log.warn("User login failed login='{}'", login);
            }
            tx.commit();
            log.debug("login: tx committed");
            return res;
        } catch (RuntimeException e) {
            tx.rollback();
            log.debug("login: tx rolled back");
            log.error("login failed login='{}'", login, e);
            throw e;
        } finally {
            session.setDefaultReadOnly(prevRO);
            log.trace("login: defaultReadOnly restored to {}", prevRO);
        }
    }

    /**
     * Finds a user by login (case-insensitive).
     *
     * @param login login string
     * @return optional user
     */
    @Override
    public Optional<User> findByLogin(String login) {
        log.debug("findByLogin: login='{}'", login);
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.getTransaction();
        boolean started = false;
        boolean touchedRO = false;
        boolean prevRO = false;
        try {
            if (!tx.isActive()) {
                tx = session.beginTransaction();
                started = true;
                prevRO = session.isDefaultReadOnly();
                session.setDefaultReadOnly(true);
                touchedRO = true;
                log.debug("findByLogin: tx started; defaultReadOnly {} -> true", prevRO);
            }
            Optional<User> res = users.findByLogin(login);
            log.debug("findByLogin login='{}' found={}", login, res.isPresent());
            if (started) {
                tx.commit();
                log.debug("findByLogin: tx committed");
            }
            return res;
        } catch (RuntimeException e) {
            if (started) {
                tx.rollback();
                log.debug("findByLogin: tx rolled back");
            }
            log.error("findByLogin failed login='{}'", login, e);
            throw e;
        } finally {
            if (touchedRO) {
                session.setDefaultReadOnly(prevRO);
                log.trace("findByLogin: defaultReadOnly restored to {}", prevRO);
            }
        }
    }

    /**
     * Finds a user by identifier.
     *
     * @param userId user identifier
     * @return optional user
     */
    @Override
    public Optional<User> findById(String userId) {
        log.debug("findById: id={}", userId);
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.getTransaction();
        boolean started = false;
        boolean touchedRO = false;
        boolean prevRO = false;
        try {
            if (!tx.isActive()) {
                tx = session.beginTransaction();
                started = true;
                prevRO = session.isDefaultReadOnly();
                session.setDefaultReadOnly(true);
                touchedRO = true;
                log.debug("findById: tx started; defaultReadOnly {} -> true", prevRO);
            }
            Optional<User> res = users.findById(userId);
            log.debug("findById id={} found={}", userId, res.isPresent());
            if (started) {
                tx.commit();
                log.debug("findById: tx committed");
            }
            return res;
        } catch (RuntimeException e) {
            if (started) {
                tx.rollback();
                log.debug("findById: tx rolled back");
            }
            log.error("findById failed id={}", userId, e);
            throw e;
        } finally {
            if (touchedRO) {
                session.setDefaultReadOnly(prevRO);
                log.trace("findById: defaultReadOnly restored to {}", prevRO);
            }
        }
    }

    /**
     * Updates the display name of a user.
     *
     * @param userId         user identifier
     * @param newDisplayName new non-blank display name (length limits apply)
     * @return updated user
     * @throws NoSuchElementException   if user not found
     * @throws IllegalArgumentException if name is blank or violates constraints
     */
    @Override
    public User updateProfile(String userId, String newDisplayName) {
        if (StringUtils.isBlank(newDisplayName)) {
            log.warn("updateProfile denied: blank displayName userId={}", userId);
            throw new IllegalArgumentException("Display name must not be blank");
        }
        log.info("updateProfile: userId={} newDisplayName='{}'", userId, newDisplayName);
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        log.debug("updateProfile: tx started");
        try {
            User current = users.findById(userId).orElseThrow(() -> {
                log.warn("updateProfile denied: user not found id={}", userId);
                return new NoSuchElementException("User not found: " + userId);
            });
            if (newDisplayName.equals(current.getUserName())) {
                tx.commit();
                log.debug("updateProfile no-op id={} (same displayName)", userId);
                return current;
            }
            current.setUserName(newDisplayName);
            users.update(current);
            tx.commit();
            log.info("User profile updated id={} newDisplayName='{}'", userId, newDisplayName);
            log.debug("updateProfile: tx committed");
            return current;
        } catch (RuntimeException e) {
            tx.rollback();
            log.debug("updateProfile: tx rolled back");
            log.error("updateProfile failed userId={}", userId, e);
            throw e;
        }
    }

    /**
     * Changes a user's password.
     *
     * <p>Validates the current password, minimum length, and that the new password differs
     * from the current one. In production, compare hashed passwords.</p>
     *
     * @param userId          user identifier
     * @param currentPassword current password (or its hashed form)
     * @param newPassword     new password (min length 6)
     * @throws SecurityException        if current password does not match
     * @throws IllegalArgumentException if new password is invalid or same as current
     * @throws NoSuchElementException   if user not found
     */
    @Override
    public void changePassword(String userId, String currentPassword, String newPassword) {
        if (StringUtils.isBlank(newPassword) || newPassword.length() < 6) {
            log.warn("changePassword denied: invalid new password userId={}", userId);
            throw new IllegalArgumentException("New password must be at least 6 characters");
        }
        log.info("changePassword: userId={}", userId);
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        log.debug("changePassword: tx started");
        try {
            User current = users.findById(userId).orElseThrow(() -> {
                log.warn("changePassword denied: user not found id={}", userId);
                return new NoSuchElementException("User not found: " + userId);
            });
            if (!current.getPassword().equals(currentPassword)) {
                log.warn("changePassword denied: incorrect current password userId={}", userId);
                throw new SecurityException("Current password is incorrect");
            }
            if (currentPassword.equals(newPassword)) {
                log.warn("changePassword denied: new password equals current userId={}", userId);
                throw new IllegalArgumentException("New password must be different from current password");
            }
            current.setPassword(newPassword);
            users.update(current);
            tx.commit();
            log.info("User password changed id={}", userId);
            log.debug("changePassword: tx committed");
        } catch (RuntimeException e) {
            tx.rollback();
            log.debug("changePassword: tx rolled back");
            log.error("changePassword failed userId={}", userId, e);
            throw e;
        }
    }

    /**
     * Admin-only update for role, name, login and optionally password.
     *
     * <p>Validates inputs; enforces minimal password requirements when provided.
     * May throw {@link DuplicateLoginException} if login conflicts.</p>
     *
     * @param userId            user identifier
     * @param role              new role (null to keep)
     * @param userName          new display name
     * @param userLogin         new login (unique)
     * @param newPasswordOrNull optional new password
     * @return updated user
     */
    @Override
    public User adminUpdate(String userId, Role role, String userName, String userLogin, String newPasswordOrNull) {
        String name = Web.trimOrNull(userName);
        String login = Web.trimOrNull(userLogin);
        if (name == null || login == null) {
            log.warn("adminUpdate denied: blank name/login userId={}", userId);
            throw new IllegalArgumentException("Name and login are required");
        }
        log.info("adminUpdate: userId={} role={} login='{}' name='{}'", userId, role, login, name);
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        log.debug("adminUpdate: tx started");
        try {
            User current = users.findById(userId).orElseThrow(() -> {
                log.warn("adminUpdate denied: user not found id={}", userId);
                return new NoSuchElementException("User not found: " + userId);
            });
            current.setRole(role == null ? current.getRole() : role);
            current.setUserName(name);
            current.setUserLogin(login);
            if (newPasswordOrNull != null && !newPasswordOrNull.isBlank()) {
                if (newPasswordOrNull.length() < 6) {
                    log.warn("adminUpdate denied: new password too short userId={}", userId);
                    throw new IllegalArgumentException("Password too short");
                }
                if (newPasswordOrNull.equals(current.getPassword())) {
                    log.warn("adminUpdate denied: new password same as current userId={}", userId);
                    throw new IllegalArgumentException("New password must differ from current");
                }
                current.setPassword(newPasswordOrNull);
            }
            users.update(current);
            tx.commit();
            log.info("User updated by admin id={} role={} login='{}' name='{}'",
                    userId, current.getRole(), current.getUserLogin(), current.getUserName());
            log.debug("adminUpdate: tx committed");
            return current;
        } catch (RuntimeException e) {
            tx.rollback();
            log.debug("adminUpdate: tx rolled back");
            log.error("adminUpdate failed userId={}", userId, e);
            throw e;
        }
    }

    /**
     * Immutable paginated wrapper.
     *
     * @param <T> element type
     */
    public record PagedResult<T>(List<T> items, long total, int page, int size) {
        public int totalPages() {
            if (size <= 0) {
                return 1;
            }
            return (int) Math.max(1L, (total + size - 1) / size);
        }

        public int offset() {
            return Math.max(0, (page - 1) * size);
        }
    }
}