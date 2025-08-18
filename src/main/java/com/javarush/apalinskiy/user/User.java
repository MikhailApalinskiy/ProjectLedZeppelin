package com.javarush.apalinskiy.user;

import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

@Getter
@ToString(exclude = "password")
public class User {
    private static final AtomicLong SEQ = new AtomicLong(0);
    private final Role role;
    private final long userId;
    private final String userName;
    private final String userLogin;
    private final String password;
    private final Instant createdAt;

    public User(Role role, String userName, String userLogin, String password, Instant createdAt) {
        this.role = role == null ? Role.USER : role;
        this.userId = SEQ.incrementAndGet();
        this.userName = userName;
        this.userLogin = normalize(userLogin);
        this.password = password;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public static User of(Role role, String userName, String userLogin, String password) {
        return new User(role, userName, userLogin, password, Instant.now());
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof User user)) return false;
        return userId == user.userId;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(userId);
    }
}
