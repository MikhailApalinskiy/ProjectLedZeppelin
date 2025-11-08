package com.javarush.apalinskiy.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CurrentUserContext (unit)")
class CurrentUserContextTest {

    @BeforeEach
    void beforeEach() {
        CurrentUserContext.clear();
    }

    @AfterEach
    void afterEach() {
        CurrentUserContext.clear();
    }

    @Test
    @DisplayName("Given userId set — When get() — Then returns same value")
    void getReturnsValueAfterSet() {
        // Given
        String userId = "u123";
        // When
        CurrentUserContext.set(userId);
        String actual = CurrentUserContext.get();
        // Then
        assertEquals(userId, actual);
    }

    @Test
    @DisplayName("Given nothing set — When get() — Then returns null")
    void getReturnsNullIfNotSet() {
        assertNull(CurrentUserContext.get());
    }

    @Test
    @DisplayName("Given userId set — When clear() — Then get() returns null")
    void clearRemovesValue() {
        // Given
        CurrentUserContext.set("u42");
        assertNotNull(CurrentUserContext.get());
        // When
        CurrentUserContext.clear();
        // Then
        assertNull(CurrentUserContext.get());
    }

    @Test
    @DisplayName("Given userId set — When require() — Then returns same value")
    void requireReturnsValueIfSet() {
        // Given
        CurrentUserContext.set("x1");
        // When
        String id = CurrentUserContext.require();
        // Then
        assertEquals("x1", id);
    }

    @Test
    @DisplayName("Given not set — When require() — Then throws IllegalStateException")
    void requireThrowsIfNotSet() {
        // Given nothing
        // When / Then
        IllegalStateException ex = assertThrows(IllegalStateException.class, CurrentUserContext::require);
        assertEquals("No current user in context", ex.getMessage());
    }

    @Test
    @DisplayName("Given blank userId set — When require() — Then throws IllegalStateException")
    void requireThrowsIfBlank() {
        // Given
        CurrentUserContext.set("   ");
        // When / Then
        assertThrows(IllegalStateException.class, CurrentUserContext::require);
    }

    @Test
    @DisplayName("Given two threads — When each sets different userId — Then values isolated per thread")
    void threadIsolation() throws InterruptedException {
        // Given
        CurrentUserContext.set("main-user");
        final StringBuilder otherThreadValue = new StringBuilder();
        Thread t = new Thread(() -> {
            CurrentUserContext.set("worker-user");
            otherThreadValue.append(CurrentUserContext.get());
            CurrentUserContext.clear();
        });
        // When
        t.start();
        t.join();
        // Then
        assertEquals("worker-user", otherThreadValue.toString());
        assertEquals("main-user", CurrentUserContext.get(), "main thread value must remain intact");
    }
}