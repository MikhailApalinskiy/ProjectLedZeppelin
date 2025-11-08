package com.javarush.apalinskiy.repository.hibernate.user;

import com.javarush.apalinskiy.utils.HibernateUtil;
import com.javarush.apalinskiy.exceptions.DuplicateIdException;
import com.javarush.apalinskiy.exceptions.DuplicateLoginException;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.repository.user.UserRepository;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Hibernate-backed implementation of {@link UserRepository}.
 *
 * <p>Provides persistence and lookup operations for {@link User} entities.
 * Supports CRUD-style access and pagination, with detailed logging and
 * custom handling of unique constraint violations.</p>
 *
 * <p>Constraint names such as {@code users_pk_2} (login) and {@code users_pkey}
 * (primary key) are mapped to domain-specific exceptions:
 * {@link DuplicateLoginException} and {@link DuplicateIdException}.</p>
 *
 * <p>All methods operate within the current Hibernate session context
 * (managed externally by the service or transaction layer).</p>
 */
public class HUserRepository implements UserRepository {

    private static final Logger log = LoggerFactory.getLogger(HUserRepository.class);

    /**
     * Shared Hibernate {@link SessionFactory} instance.
     */
    private final SessionFactory sessionFactory = HibernateUtil.getSessionFactory();

    /**
     * Retrieves a {@link User} by login name (case-insensitive).
     *
     * <p>Trims and lowercases the login before querying.</p>
     *
     * @param userLogin unique login string
     * @return optional user if found
     */
    @Override
    public Optional<User> findByLogin(String userLogin) {
        Session session = sessionFactory.getCurrentSession();
        log.debug("findByLogin login='{}'", userLogin);
        Optional<User> res = session.createQuery("from User u where u.userLogin = :login", User.class)
                .setParameter("login", userLogin.trim().toLowerCase(Locale.ROOT))
                .uniqueResultOptional();
        log.debug("findByLogin login='{}' found={}", userLogin, res.isPresent());
        return res;
    }

    /**
     * Retrieves a {@link User} by its identifier.
     *
     * @param id user ID
     * @return optional user if found
     */
    @Override
    public Optional<User> findById(String id) {
        Session session = sessionFactory.getCurrentSession();
        Optional<User> res = session.byId(User.class).loadOptional(id);
        log.debug("findById id={} found={}", id, res.isPresent());
        return res;
    }

    /**
     * Persists a new {@link User} entity in the database.
     *
     * <p>Flushes immediately to detect constraint violations.</p>
     *
     * @param user user entity to persist
     * @throws DuplicateLoginException      if login already exists
     * @throws DuplicateIdException         if user ID already exists
     * @throws ConstraintViolationException if another database constraint is violated
     */
    @Override
    public void save(User user) {
        Session session = sessionFactory.getCurrentSession();
        log.info("save: id={} login='{}'", user.getUserId(), user.getUserLogin());
        try {
            session.persist(user);
            session.flush();
            log.info("save: success id={} login='{}'", user.getUserId(), user.getUserLogin());
        } catch (ConstraintViolationException cve) {
            String c = cve.getConstraintName();
            log.warn("save: constraint violation id={} login='{}' constraint='{}'",
                    user.getUserId(), user.getUserLogin(), c, cve);
            if ("users_pk_2".equalsIgnoreCase(c)) {
                throw new DuplicateLoginException("Login already exists: " + user.getUserLogin());
            }
            if ("users_pk".equalsIgnoreCase(c) || "pk_users".equalsIgnoreCase(c)) {
                throw new DuplicateIdException("UserId already exists: " + user.getUserId());
            }
            throw cve;
        }
    }

    /**
     * Updates an existing {@link User} record in the database.
     *
     * <p>Performs a merge and flush, and checks for duplicate login conflicts.</p>
     *
     * @param user user entity to update
     * @throws DuplicateLoginException      if new login duplicates another user
     * @throws ConstraintViolationException if another constraint fails
     */
    @Override
    public void update(User user) {
        Session session = sessionFactory.getCurrentSession();
        log.info("update: id={} login='{}'", user.getUserId(), user.getUserLogin());
        try {
            session.merge(user);
            session.flush();
            log.debug("update ok id={} login='{}'", user.getUserId(), user.getUserLogin());
        } catch (ConstraintViolationException cve) {
            String constraint = cve.getConstraintName();
            log.warn("update: constraint violation id={} login='{}' constraint='{}'",
                    user.getUserId(), user.getUserLogin(), constraint, cve);
            if ("users_pk_2".equalsIgnoreCase(constraint)) {
                log.warn("update denied on flush: duplicate login newLogin='{}'", user.getUserLogin());
                throw new DuplicateLoginException("Login already exists: " + user.getUserLogin());
            }
            throw cve;
        }
    }

    /**
     * Retrieves a paginated list of users ordered by creation time descending
     * and then by login name ascending.
     *
     * @param page page number (1-based)
     * @param size page size (minimum = 1)
     * @return immutable list of users for the requested page
     */
    @Override
    public List<User> findPage(int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, size);
        int offset = (safePage - 1) * safeSize;
        log.debug("findPage page={} size={} offset={}", safePage, safeSize, offset);
        Session session = sessionFactory.getCurrentSession();
        List<User> list = session.createQuery(
                        "from User u order by u.createdAt desc, u.userLogin asc",
                        User.class)
                .setReadOnly(true)
                .setFirstResult(offset)
                .setMaxResults(safeSize)
                .getResultList();
        log.debug("findPage resultSize={}", list.size());
        return List.copyOf(list);
    }

    /**
     * Counts all {@link User} entities in the system.
     *
     * @return total user count
     */
    @Override
    public long countAll() {
        Session session = sessionFactory.getCurrentSession();
        log.debug("countAll()");
        Long cnt = session.createQuery("select count(u) from User u", Long.class)
                .setReadOnly(true)
                .uniqueResult();
        long out = (cnt == null ? 0L : cnt);
        log.debug("countAll -> {}", out);
        return out;
    }
}