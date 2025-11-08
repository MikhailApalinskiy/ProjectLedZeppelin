package com.javarush.apalinskiy.web.filter;

import com.javarush.apalinskiy.app.WebConst;
import com.javarush.apalinskiy.domain.user.Role;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.web.util.Web;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("AdminOnlyFilter")
@ExtendWith(MockitoExtension.class)
class AdminOnlyFilterTest {

    private AdminOnlyFilter sut;

    @Mock
    HttpServletRequest req;
    @Mock
    HttpServletResponse resp;
    @Mock
    FilterChain chain;
    @Mock
    HttpSession session;
    @Mock
    User user;

    private MockedStatic<Web> WEB;

    @BeforeEach
    void setUp() {
        sut = new AdminOnlyFilter();
        when(req.getSession()).thenReturn(session);
        when(req.getRequestURI()).thenReturn("/admin/panel");
        WEB = Mockito.mockStatic(Web.class);
    }

    @AfterEach
    void tearDown() {
        if (WEB != null) WEB.close();
    }

    @Nested
    @DisplayName("doFilter")
    class DoFilter {

        @Test
        @DisplayName("Given no user in session — When doFilter — Then redirect to HOME and stop chain")
        void anonymous_redirectsHome() throws IOException, ServletException {
            // Given
            when(session.getAttribute(eq(WebConst.Attr.USER))).thenReturn(null);
            // When
            sut.doFilter(req, resp, chain);
            // Then
            WEB.verify(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.HOME), anyString()));
            verify(chain, never()).doFilter(any(ServletRequest.class), any(ServletResponse.class));
        }

        @Test
        @DisplayName("Given non-admin user — When doFilter — Then redirect to HOME and stop chain")
        void nonAdmin_redirectsHome() throws IOException, ServletException {
            // Given
            when(user.getRole()).thenReturn(Role.USER);
            when(user.getUserId()).thenReturn("u-1");
            when(user.getUserLogin()).thenReturn("john");
            when(session.getAttribute(eq(WebConst.Attr.USER))).thenReturn(user);
            // When
            sut.doFilter(req, resp, chain);
            // Then
            WEB.verify(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.HOME), anyString()));
            verify(chain, never()).doFilter(any(ServletRequest.class), any(ServletResponse.class));
        }

        @Test
        @DisplayName("Given admin user — When doFilter — Then passes through chain and does not redirect")
        void admin_passesThrough() throws IOException, ServletException {
            // Given
            when(user.getRole()).thenReturn(Role.ADMIN);
            when(user.getUserId()).thenReturn("admin-1");
            when(user.getUserLogin()).thenReturn("admin");
            when(session.getAttribute(eq(WebConst.Attr.USER))).thenReturn(user);
            // When
            sut.doFilter(req, resp, chain);
            // Then
            verify(chain, times(1)).doFilter(eq(req), eq(resp));
            WEB.verifyNoInteractions();
        }
    }
}