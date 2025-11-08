package com.javarush.apalinskiy.web.filter;

import com.javarush.apalinskiy.app.WebConst;
import com.javarush.apalinskiy.domain.user.Role;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.repository.notify.NotificationRepository;
import com.javarush.apalinskiy.web.util.Web;
import jakarta.servlet.*;
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

import static org.mockito.Mockito.*;

@DisplayName("NotificationsBadgeFilter")
@ExtendWith(MockitoExtension.class)
class NotificationsBadgeFilterTest {

    private NotificationsBadgeFilter sut;

    @Mock
    FilterConfig filterConfig;
    @Mock
    ServletContext servletContext;
    @Mock
    NotificationRepository repo;
    @Mock
    HttpServletRequest req;
    @Mock
    HttpServletResponse resp;
    @Mock
    FilterChain chain;
    @Mock
    HttpSession session;

    private MockedStatic<Web> WEB;

    @BeforeEach
    void setUp() {
        sut = new NotificationsBadgeFilter();
        when(filterConfig.getServletContext()).thenReturn(servletContext);
        when(req.getContextPath()).thenReturn("/app");
        when(req.getRequestURI()).thenReturn("/app/home");
        WEB = Mockito.mockStatic(Web.class);
        WEB.when(() -> Web.ctxBean(eq(servletContext), eq(WebConst.Ctx.NOTIFY_REPO), eq(NotificationRepository.class)))
                .thenReturn(repo);
        sut.init(filterConfig);
    }

    @AfterEach
    void tearDown() {
        if (WEB != null) {
            WEB.close();
        }
    }

    @Nested
    @DisplayName("doFilter")
    class DoFilter {

        @Test
        @DisplayName("Given /assets/* request — When doFilter — Then skip attribute and just pass through")
        void assets_areSkipped() throws IOException, ServletException {
            // Given
            when(req.getRequestURI()).thenReturn("/app/assets/css/main.css");
            // When
            sut.doFilter(req, resp, chain);
            // Then
            verify(req, never()).setAttribute(eq("unreadCount"), any());
            verify(chain, times(1)).doFilter(eq(req), eq(resp));
            verifyNoInteractions(repo);
        }

        @Test
        @DisplayName("Given no session — When doFilter — Then unreadCount=0 and pass through")
        void noSession_setsZero() throws IOException, ServletException {
            // Given
            when(req.getSession(false)).thenReturn(null);
            // When
            sut.doFilter(req, resp, chain);
            // Then
            verify(req).setAttribute("unreadCount", 0);
            verify(chain).doFilter(eq(req), eq(resp));
            verifyNoInteractions(repo);
        }

        @Test
        @DisplayName("Given session without user — When doFilter — Then unreadCount=0 and pass through (no repo calls)")
        void sessionWithoutUser_setsZero() throws IOException, ServletException {
            // Given
            when(req.getSession(false)).thenReturn(session);
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(null);
            // When
            sut.doFilter(req, resp, chain);
            // Then
            verify(req).setAttribute("unreadCount", 0);
            verify(chain).doFilter(eq(req), eq(resp));
            verifyNoInteractions(repo);
        }

        @Test
        @DisplayName("Given logged-in user — When doFilter — Then unreadCount from repo is attached and passed through")
        void loggedInUser_fetchesCount() throws IOException, ServletException {
            // Given
            User u = new User();
            u.setRole(Role.USER);
            u.setUserId("u-1");
            when(req.getSession(false)).thenReturn(session);
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(u);
            when(repo.unreadCount("u-1")).thenReturn(5);
            // When
            sut.doFilter(req, resp, chain);
            // Then
            verify(repo, times(1)).unreadCount("u-1");
            verify(req).setAttribute("unreadCount", 5);
            verify(chain).doFilter(eq(req), eq(resp));
        }

        @Test
        @DisplayName("Given repo returns 0 — When doFilter — Then unreadCount=0 attached explicitly")
        void repoReturnsZero_attachesZero() throws IOException, ServletException {
            // Given
            User u = new User();
            u.setUserId("u-2");
            when(req.getSession(false)).thenReturn(session);
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(u);
            when(repo.unreadCount("u-2")).thenReturn(0);
            // When
            sut.doFilter(req, resp, chain);
            // Then
            verify(req).setAttribute("unreadCount", 0);
            verify(chain).doFilter(eq(req), eq(resp));
        }
    }
}