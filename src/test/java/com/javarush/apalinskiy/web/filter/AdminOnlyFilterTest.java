package com.javarush.apalinskiy.web.filter;

import com.javarush.apalinskiy.app.WebConst;
import com.javarush.apalinskiy.domain.user.Role;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.web.util.Web;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminOnlyFilter")
class AdminOnlyFilterTest {

    AdminOnlyFilter sut;

    @Mock
    HttpServletRequest req;
    @Mock
    HttpServletResponse resp;
    @Mock
    FilterChain chain;
    @Mock
    HttpSession session;

    @BeforeEach
    void setUp() {
        sut = new AdminOnlyFilter();
        when(req.getSession()).thenReturn(session);
    }

    @Nested
    @DisplayName("doFilter")
    class DoFilter {

        @Test
        @DisplayName("no user -> redirectErr(HOME,'Access denied'); цепочка не вызывается")
        void noUser_redirectsAndStopsChain() throws IOException, ServletException {
            // Given
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(null);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // When
                sut.doFilter(req, resp, chain);
                // Then
                web.verify(() -> Web.redirectErr(req, resp, WebConst.Path.HOME, "Access denied"));
                verifyNoInteractions(chain);
            }
        }

        @Test
        @DisplayName("non-admin user -> redirectErr(HOME,'Access denied'); цепочка не вызывается")
        void nonAdmin_redirectsAndStopsChain() throws IOException, ServletException {
            // Given
            User u = User.of(Role.USER, "U", "u", "p");
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(u);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // When
                sut.doFilter(req, resp, chain);
                // Then
                web.verify(() -> Web.redirectErr(req, resp, WebConst.Path.HOME, "Access denied"));
                verifyNoInteractions(chain);
            }
        }

        @Test
        @DisplayName("admin user -> пропускает дальше (chain.doFilter), без redirectErr")
        void admin_passesThrough() {
            // Given
            User admin = User.of(Role.ADMIN, "A", "a", "p");
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(admin);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // When / Then
                assertDoesNotThrow(() -> sut.doFilter(req, resp, chain));
                verify(chain).doFilter(req, resp);
                web.verify(() -> Web.redirectErr(any(), any(), any(), any()), never());
            } catch (IOException | ServletException e) {
                throw new RuntimeException(e);
            }
        }
    }
}