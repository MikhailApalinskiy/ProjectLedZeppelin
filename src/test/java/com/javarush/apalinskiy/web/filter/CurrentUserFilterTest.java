package com.javarush.apalinskiy.web.filter;

import com.javarush.apalinskiy.app.WebConst;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.service.user.UserService;
import com.javarush.apalinskiy.web.util.Web;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CurrentUserFilter")
class CurrentUserFilterTest {

    @Mock
    FilterConfig cfg;
    @Mock
    ServletContext ctx;
    @Mock
    UserService userService;
    @Mock
    HttpServletRequest req;
    @Mock
    ServletResponse resp;
    @Mock
    HttpSession session;
    @Mock
    FilterChain chain;
    @Mock
    User user;
    @Mock User freshUser;

    private static <T> T getField(Object target) {
        try {
            Field f = target.getClass().getDeclaredField("userService");
            f.setAccessible(true);
            //noinspection unchecked
            return ((Class<T>) UserService.class).cast(f.get(target));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    private static void setField(Object target, Object value) {
        try {
            Field f = target.getClass().getDeclaredField("userService");
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("init(...)")
    class InitBlock {
        @Test
        @DisplayName("given USER_SERVICE in context when init then stores UserService and calls Web.ctxBean")
        void givenCtxHasUserService_whenInit_thenStoresIt() throws UnavailableException {
            // Given
            CurrentUserFilter filter = new CurrentUserFilter();
            when(cfg.getServletContext()).thenReturn(ctx);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_SERVICE, UserService.class))
                        .thenReturn(userService);
                // When
                filter.init(cfg);
                // Then
                web.verify(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_SERVICE, UserService.class));
                assertSame(userService, getField(filter));
            }
        }
    }

    @Nested
    @DisplayName("doFilter(...)")
    class DoFilterBlock {

        @Test
        @DisplayName("given no session when doFilter then just chain.doFilter and no lookups")
        void givenNoSession_whenDoFilter_thenChainOnly() throws IOException, ServletException {
            // Given
            CurrentUserFilter filter = new CurrentUserFilter();
            setField(filter, userService);
            when(req.getSession(false)).thenReturn(null);
            // When
            filter.doFilter(req, resp, chain);
            // Then
            verifyNoInteractions(userService);
            verify(chain).doFilter(req, resp);
        }

        @Test
        @DisplayName("given session with User and user found when doFilter then refresh in request & session and chain")
        void givenUserInSession_andFound_whenDoFilter_thenRefreshBothAndChain() throws IOException, ServletException {
            // Given
            CurrentUserFilter filter = new CurrentUserFilter();
            setField(filter, userService);
            when(req.getSession(false)).thenReturn(session);
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(user);
            when(user.getUserId()).thenReturn("u-1");
            when(userService.findById("u-1")).thenReturn(Optional.of(freshUser));
            // When
            filter.doFilter(req, resp, chain);
            // Then
            verify(userService).findById("u-1");
            verify(req).setAttribute(WebConst.Attr.USER, freshUser);
            verify(session).setAttribute(WebConst.Attr.USER, freshUser);
            verify(chain).doFilter(req, resp);
        }

        @Test
        @DisplayName("given session with User but user not found when doFilter then no attr changes and chain")
        void givenUserInSession_andNotFound_whenDoFilter_thenNoAttrChangeAndChain() throws IOException, ServletException {
            // Given
            CurrentUserFilter filter = new CurrentUserFilter();
            setField(filter, userService);
            when(req.getSession(false)).thenReturn(session);
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(user);
            when(user.getUserId()).thenReturn("u-missing");
            when(userService.findById("u-missing")).thenReturn(Optional.empty());
            // When
            filter.doFilter(req, resp, chain);
            // Then
            verify(userService).findById("u-missing");
            verify(req, never()).setAttribute(eq(WebConst.Attr.USER), any());
            verify(session, never()).setAttribute(eq(WebConst.Attr.USER), any());
            verify(chain).doFilter(req, resp);
        }

        @Test
        @DisplayName("given session with String userId and user found when doFilter then set request attr only and chain")
        void givenStringUserIdInSession_andFound_whenDoFilter_thenSetReqOnlyAndChain() throws IOException, ServletException {
            // Given
            CurrentUserFilter filter = new CurrentUserFilter();
            setField(filter, userService);
            when(req.getSession(false)).thenReturn(session);
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn("uid-42");
            when(userService.findById("uid-42")).thenReturn(Optional.of(freshUser));
            // When
            filter.doFilter(req, resp, chain);
            // Then
            verify(userService).findById("uid-42");
            verify(req).setAttribute(WebConst.Attr.USER, freshUser);
            verify(session, never()).setAttribute(eq(WebConst.Attr.USER), any());
            verify(chain).doFilter(req, resp);
        }

        @Test
        @DisplayName("given session with String userId but not found when doFilter then no attr changes and chain")
        void givenStringUserIdInSession_andNotFound_whenDoFilter_thenNoAttrChangeAndChain() throws IOException, ServletException {
            // Given
            CurrentUserFilter filter = new CurrentUserFilter();
            setField(filter, userService);
            when(req.getSession(false)).thenReturn(session);
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn("ghost");
            when(userService.findById("ghost")).thenReturn(Optional.empty());
            // When
            filter.doFilter(req, resp, chain);
            // Then
            verify(userService).findById("ghost");
            verify(req, never()).setAttribute(eq(WebConst.Attr.USER), any());
            verify(session, never()).setAttribute(eq(WebConst.Attr.USER), any());
            verify(chain).doFilter(req, resp);
        }

        @Test
        @DisplayName("given session with unknown type in attr when doFilter then skip lookups and chain")
        void givenUnknownTypeInSession_whenDoFilter_thenSkipAndChain() throws IOException, ServletException {
            // Given
            CurrentUserFilter filter = new CurrentUserFilter();
            setField(filter, userService);
            when(req.getSession(false)).thenReturn(session);
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(12345);
            // When
            filter.doFilter(req, resp, chain);
            // Then
            verifyNoInteractions(userService);
            verify(chain).doFilter(req, resp);
        }
    }
}