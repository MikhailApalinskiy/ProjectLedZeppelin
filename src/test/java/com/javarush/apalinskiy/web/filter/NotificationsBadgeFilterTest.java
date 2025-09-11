package com.javarush.apalinskiy.web.filter;

import com.javarush.apalinskiy.app.WebConst;
import com.javarush.apalinskiy.domain.user.Role;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.repository.notify.NotificationRepository;
import com.javarush.apalinskiy.web.util.Web;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationsBadgeFilter")
class NotificationsBadgeFilterTest {

    NotificationsBadgeFilter sut;

    @Mock
    FilterConfig cfg;
    @Mock
    ServletContext appCtx;
    @Mock
    NotificationRepository repo;
    @Mock
    HttpServletRequest req;
    @Mock
    ServletResponse resp;
    @Mock
    FilterChain chain;
    @Mock
    HttpSession session;

    @BeforeEach
    void setUp() {
        sut = new NotificationsBadgeFilter();
    }

    @Nested
    @DisplayName("init()")
    class InitMethod {
        @Test
        @DisplayName("resolves repo via Web.ctxBean")
        void resolvesRepo() {
            // Given
            when(cfg.getServletContext()).thenReturn(appCtx);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.ctxBean(appCtx, WebConst.Ctx.NOTIFY_REPO, NotificationRepository.class))
                        .thenReturn(repo);
                // When / Then
                assertDoesNotThrow(() -> sut.init(cfg));
                web.verify(() -> Web.ctxBean(appCtx, WebConst.Ctx.NOTIFY_REPO, NotificationRepository.class));
            }
        }
    }

    @Nested
    @DisplayName("doFilter()")
    class DoFilter {

        @Test
        @DisplayName("bypasses for /assets/* -> delegates, no unreadCount calculation")
        void bypassesAssets() throws IOException, ServletException {
            // Given
            when(cfg.getServletContext()).thenReturn(appCtx);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.ctxBean(appCtx, WebConst.Ctx.NOTIFY_REPO, NotificationRepository.class))
                        .thenReturn(repo);
                sut.init(cfg);
            }
            when(req.getContextPath()).thenReturn("/app");
            when(req.getRequestURI()).thenReturn("/app/assets/logo.png");
            // When
            sut.doFilter(req, resp, chain);
            // Then
            verify(chain).doFilter(req, resp);
            verify(req, never()).getSession(false);
            verify(req, never()).setAttribute(eq("unreadCount"), any());
            verifyNoInteractions(repo);
        }

        @Test
        @DisplayName("session=null -> sets unreadCount=0 and delegates")
        void sessionNullSetsZero() throws IOException, ServletException {
            // Given
            when(cfg.getServletContext()).thenReturn(appCtx);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.ctxBean(appCtx, WebConst.Ctx.NOTIFY_REPO, NotificationRepository.class))
                        .thenReturn(repo);
                sut.init(cfg);
            }
            when(req.getContextPath()).thenReturn("/app");
            when(req.getRequestURI()).thenReturn("/app/home");
            when(req.getSession(false)).thenReturn(null);
            // When
            sut.doFilter(req, resp, chain);
            // Then
            InOrder in = inOrder(req, chain);
            in.verify(req).setAttribute("unreadCount", 0);
            in.verify(chain).doFilter(req, resp);
            verifyNoInteractions(repo);
        }

        @Test
        @DisplayName("user=null -> sets unreadCount=0 and delegates (no repo call)")
        void userNullSetsZero() throws IOException, ServletException {
            // Given
            when(cfg.getServletContext()).thenReturn(appCtx);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.ctxBean(appCtx, WebConst.Ctx.NOTIFY_REPO, NotificationRepository.class))
                        .thenReturn(repo);
                sut.init(cfg);
            }
            when(req.getContextPath()).thenReturn("/app");
            when(req.getRequestURI()).thenReturn("/app/page");
            when(req.getSession(false)).thenReturn(session);
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(null);
            // When
            sut.doFilter(req, resp, chain);
            // Then
            verify(req).setAttribute("unreadCount", 0);
            verify(chain).doFilter(req, resp);
            verifyNoInteractions(repo);
        }

        @Test
        @DisplayName("user present -> sets unreadCount from repo and delegates")
        void userPresentSetsFromRepo() throws IOException, ServletException {
            // Given
            when(cfg.getServletContext()).thenReturn(appCtx);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.ctxBean(appCtx, WebConst.Ctx.NOTIFY_REPO, NotificationRepository.class))
                        .thenReturn(repo);
                sut.init(cfg);
            }
            User u = User.of(Role.USER, "A", "a", "p").withId("id1");
            when(req.getContextPath()).thenReturn("/app");
            when(req.getRequestURI()).thenReturn("/app/any");
            when(req.getSession(false)).thenReturn(session);
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(u);
            when(repo.unreadCount("id1")).thenReturn(7);
            // When
            sut.doFilter(req, resp, chain);
            // Then
            verify(repo).unreadCount("id1");
            verify(req).setAttribute("unreadCount", 7);
            verify(chain).doFilter(req, resp);
        }
    }
}