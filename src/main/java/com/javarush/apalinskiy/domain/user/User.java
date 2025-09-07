package com.javarush.apalinskiy.domain.user;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Getter
public class User {
    private final Role role;
    private final String userId;
    private final String userName;
    private final String userLogin;
    private final String password;
    private final Instant createdAt;

    private User(Role role, String userName, String userLogin, String password, Instant createdAt, String userId) {
        if (StringUtils.isBlank(userName) || StringUtils.isBlank(userLogin) || StringUtils.isBlank(password)) {
            throw new IllegalArgumentException("Username or login or password are required");
        }
        this.role = (role == null) ? Role.USER : role;
        this.userId = userId;
        this.userName = userName.trim();
        this.userLogin = userLogin.trim().toLowerCase(Locale.ROOT);
        this.password = password;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public static User of(Role role, String userName, String userLogin, String password) {
        return new User(role, userName, userLogin, password, null, UUID.randomUUID().toString());
    }

    public User withId(String id) {
        return new User(this.getRole(), this.getUserName(), this.getUserLogin(),
                this.getPassword(), this.getCreatedAt(), id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return userId.equals(other.userId);
    }

    @Override
    public int hashCode() {
        return userId.hashCode();
    }
}
