package com.javarush.apalinskiy.domain.user;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain model representing an application user.
 * <p>
 * A {@code User} is immutable and contains identity, authentication,
 * and authorization data.
 * </p>
 *
 * <h3>Fields</h3>
 * <ul>
 *   <li>{@link #role} – user's {@link Role} (defaults to {@link Role#USER} if null).</li>
 *   <li>{@link #userId} – globally unique identifier of the user (UUID string).</li>
 *   <li>{@link #userName} – display name of the user.</li>
 *   <li>{@link #userLogin} – normalized login (trimmed, lowercased).</li>
 *   <li>{@link #password} – password hash or raw password (depending on implementation).</li>
 *   <li>{@link #createdAt} – timestamp when the user was created.</li>
 * </ul>
 *
 * <h3>Construction &amp; Immutability</h3>
 * <ul>
 *   <li>Instances are created with {@link #of(Role, String, String, String)}.</li>
 *   <li>Copy-like methods ({@code withX}) return a new {@code User} with one field changed.</li>
 *   <li>Validation: user name, login, and password must not be blank.</li>
 * </ul>
 *
 * <h3>Equality</h3>
 * Equality and hash code are based solely on {@link #userId}.
 */
@Getter
public class User {

    private static final int MAX_LENGTH = 20;
    /**
     * Role of the user (defaults to {@link Role#USER}).
     */
    private final Role role;
    /**
     * Unique identifier of the user.
     */
    private final String userId;
    /**
     * Display name of the user.
     */
    private final String userName;
    /**
     * Normalized login (trimmed, lowercased).
     */
    private final String userLogin;
    /**
     * Password string (usually hashed).
     */
    private final String password;
    /**
     * Creation timestamp of this user.
     */
    private final Instant createdAt;

    /**
     * Constructs a new immutable {@code User}.
     *
     * @param role      role of the user (defaults to USER if null)
     * @param userName  display name (non-blank)
     * @param userLogin login (non-blank, normalized to lowercase)
     * @param password  password (non-blank)
     * @param createdAt creation timestamp (if null, set to {@link Instant#now()})
     * @param userId    unique identifier (non-null)
     * @throws IllegalArgumentException if userName, userLogin, or password are blank
     * @throws NullPointerException     if userId is null
     */
    private User(Role role, String userName, String userLogin, String password, Instant createdAt, String userId) {
        if (StringUtils.isBlank(userName) || StringUtils.isBlank(userLogin) || StringUtils.isBlank(password)) {
            throw new IllegalArgumentException("Username or login or password are required");
        }
        if (userName.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Username is too long");
        }else if (userLogin.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Login is too long");
        }
        this.role = (role == null) ? Role.USER : role;
        this.userId = Objects.requireNonNull(userId, "userId");
        this.userName = userName.trim();
        this.userLogin = userLogin.trim().toLowerCase(Locale.ROOT);
        this.password = password;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    /**
     * Factory method that creates a new user with a random unique ID and current timestamp.
     *
     * @param role      role of the user (may be null, defaults to USER)
     * @param userName  display name (non-blank)
     * @param userLogin login (non-blank)
     * @param password  password (non-blank)
     * @return new {@code User} instance
     */
    public static User of(Role role, String userName, String userLogin, String password) {
        return new User(role, userName, userLogin, password, null, UUID.randomUUID().toString());
    }

    /**
     * Returns a copy of this user with a new ID.
     *
     * @param id new user ID
     * @return new {@code User} instance
     */
    public User withId(String id) {
        return new User(this.getRole(), this.getUserName(), this.getUserLogin(),
                this.getPassword(), this.getCreatedAt(), id);
    }

    /**
     * Returns a copy of this user with a new display name.
     *
     * @param newName new display name
     * @return new {@code User} instance
     */
    public User withUserName(String newName) {
        return new User(this.getRole(), newName, this.getUserLogin(),
                this.getPassword(), this.getCreatedAt(), this.getUserId());
    }

    /**
     * Returns a copy of this user with a new password.
     *
     * @param newPassword new password
     * @return new {@code User} instance
     */
    public User withPassword(String newPassword) {
        return new User(this.getRole(), this.getUserName(), this.getUserLogin(),
                newPassword, this.getCreatedAt(), this.getUserId());
    }

    /**
     * Returns a copy of this user with a new role.
     * <p>
     * If {@code newRole} is null, the current role is preserved.
     * </p>
     *
     * @param newRole new role (nullable)
     * @return new {@code User} instance
     */
    public User withRole(Role newRole) {
        return new User(newRole == null ? this.getRole() : newRole, this.getUserName(), this.getUserLogin(),
                this.getPassword(), this.getCreatedAt(), this.getUserId());
    }

    /**
     * Returns a copy of this user with a new login.
     *
     * @param newLogin new login
     * @return new {@code User} instance
     */
    public User withLogin(String newLogin) {
        return new User(this.getRole(), this.getUserName(), newLogin,
                this.getPassword(), this.getCreatedAt(), this.getUserId());
    }

    /**
     * Returns the creation timestamp as a legacy {@link Date}.
     *
     * @return creation time as {@link Date}
     */
    @SuppressWarnings("unused")
    public Date getCreatedAtDate() {
        return Date.from(createdAt);
    }

    /**
     * Equality is based solely on {@link #userId}.
     *
     * @param o other object
     * @return true if both are {@code User} with same {@code userId}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof User other)) {
            return false;
        }
        return userId.equals(other.userId);
    }

    /**
     * Hash code is based solely on {@link #userId}.
     *
     * @return hash code of user ID
     */
    @Override
    public int hashCode() {
        return userId.hashCode();
    }
}
