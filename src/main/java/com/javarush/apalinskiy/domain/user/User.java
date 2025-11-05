package com.javarush.apalinskiy.domain.user;

import com.javarush.apalinskiy.domain.notify.Notification;
import com.javarush.apalinskiy.domain.quest.custom.CustomQuest;
import com.javarush.apalinskiy.domain.save.SaveState;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.*;

/**
 * Represents an application user within the TextQuest platform.
 *
 * <p>Each {@code User} has authentication credentials, role-based access control
 * (defined by {@link Role}), and owns related entities such as {@link CustomQuest},
 * {@link SaveState}, {@link Notification}, and {@link UserStats}.</p>
 *
 * <p>The entity is mapped to the {@code users} table and identified by a UUID string.</p>
 *
 * <h2>Validation rules</h2>
 * <ul>
 *   <li>{@code userName}, {@code userLogin}, and {@code password} are required</li>
 *   <li>Maximum length for {@code userName} and {@code userLogin}: 20 characters</li>
 *   <li>Minimum length for {@code password}: 6 characters</li>
 *   <li>Login is stored in lowercase</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {

    /**
     * Maximum allowed length for username or login.
     */
    private static final int MAX_LENGTH = 50;

    /**
     * Minimum allowed password length.
     */
    private static final int MIN_LENGTH = 6;

    /**
     * User role defining access privileges.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 10)
    private Role role;

    /**
     * Unique user identifier (UUID string).
     */
    @Id
    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    /**
     * Display name visible to other users.
     */
    @Column(name = "user_name", length = MAX_LENGTH, nullable = false)
    private String userName;

    /**
     * Unique login name used for authentication.
     */
    @Column(name = "user_login", length = MAX_LENGTH, unique = true, nullable = false)
    private String userLogin;

    /**
     * Encrypted user password.
     */
    @Column(name = "password", nullable = false)
    private String password;

    /**
     * Timestamp when the user account was created.
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * One-to-one relation to user statistics.
     */
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserStats stats;

    /**
     * Quests authored by this user.
     */
    @OneToMany(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<CustomQuest> quests;

    /**
     * List of friends added by this user.
     */
    @ManyToMany
    @JoinTable(
            name = "user_friends",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "friend_id")
    )
    private Set<User> friends = new LinkedHashSet<>();

    /**
     * Reverse mapping for users who have this user as a friend.
     */
    @ManyToMany(mappedBy = "friends")
    private Set<User> friendOf = new LinkedHashSet<>();

    /**
     * Notifications belonging to this user.
     */
    @OneToMany(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<Notification> notifications = new ArrayList<>();

    /**
     * One-to-one relation to the user's global save state.
     */
    @OneToOne(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private SaveState saveState;

    /**
     * Constructs a validated {@code User} instance.
     *
     * @param role      user role (defaults to {@link Role#USER} if {@code null})
     * @param userName  display name
     * @param userLogin unique login
     * @param password  password (min {@value #MIN_LENGTH} characters)
     * @param createdAt creation timestamp (defaults to now)
     * @param userId    unique user identifier (UUID)
     * @throws IllegalArgumentException if validation fails
     * @throws NullPointerException     if {@code userId} is {@code null}
     */
    private User(Role role, String userName, String userLogin, String password, Instant createdAt, String userId) {
        if (StringUtils.isBlank(userName) || StringUtils.isBlank(userLogin) || StringUtils.isBlank(password)) {
            throw new IllegalArgumentException("Username or login or password are required");
        }
        if (userName.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Username is too long, maximum length is 50 characters");
        } else if (userLogin.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Login is too long, maximum length is 50 characters");
        } else if (password.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("Password is too short, minimum length is 6 characters");
        }
        this.role = (role == null) ? Role.USER : role;
        this.userId = Objects.requireNonNull(userId, "userId");
        this.userName = userName.trim();
        this.userLogin = userLogin.trim().toLowerCase(Locale.ROOT);
        this.password = password;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    /**
     * Factory method that creates a new {@code User} with a generated UUID.
     *
     * @param role      user role
     * @param userName  display name
     * @param userLogin login
     * @param password  password
     * @return a new {@code User} instance
     */
    public static User of(Role role, String userName, String userLogin, String password) {
        return new User(role, userName, userLogin, password, null, UUID.randomUUID().toString());
    }

    /**
     * Returns a copy of this user with a new ID.
     *
     * @param id new user ID
     * @return new {@code User} instance with the provided ID
     */
    public User withId(String id) {
        return new User(this.getRole(), this.getUserName(), this.getUserLogin(),
                this.getPassword(), this.getCreatedAt(), id);
    }

    /**
     * Sets the user login ensuring it is lowercase and within length limits.
     *
     * @param userLogin new login string
     * @throws IllegalArgumentException if too long
     */
    public void setUserLogin(String userLogin) {
        String v = userLogin.trim().toLowerCase(Locale.ROOT);
        if (v.length() > MAX_LENGTH) throw new IllegalArgumentException("Login is too long");
        this.userLogin = v;
    }

    /**
     * Sets the display name ensuring it does not exceed length limits.
     *
     * @param userName new display name
     * @throws IllegalArgumentException if too long
     */
    public void setUserName(String userName) {
        if (userName.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Username is too long");
        }
        this.userName = userName;
    }

    /**
     * Returns the creation timestamp as a legacy {@link Date} object.
     *
     * @return {@link java.util.Date} representation of {@link #createdAt}
     */
    @SuppressWarnings("unused")
    public Date getCreatedAtDate() {
        return Date.from(createdAt);
    }

    /**
     * Equality is based solely on the unique {@link #userId}.
     *
     * @param o another object
     * @return {@code true} if the same user ID
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
     * Hash code based on {@link #userId}.
     *
     * @return hash of {@code userId}
     */
    @Override
    public int hashCode() {
        return userId.hashCode();
    }
}
