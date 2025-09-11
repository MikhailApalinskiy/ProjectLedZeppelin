package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.app.WebConst;
import com.javarush.apalinskiy.domain.user.Role;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.exceptions.DuplicateLoginException;
import com.javarush.apalinskiy.service.user.UserService;
import com.javarush.apalinskiy.web.util.Web;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("AuthServlet")
@ExtendWith(MockitoExtension.class)
class AuthServletTest {

    @Mock
    ServletConfig config;
    @Mock
    ServletContext ctx;
    @Mock
    HttpServletRequest req;
    @Mock
    HttpServletResponse resp;
    @Mock
    UserService userService;
    @Mock
    User user;

    private static void setField(Object target, Object value) {
        try {
            Field f;
            try {
                f = target.getClass().getDeclaredField("userService");
            } catch (NoSuchFieldException e) {
                f = target.getClass().getSuperclass().getDeclaredField("userService");
            }
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("init")
    class Init {

        @Test
        @DisplayName("given ctx has UserService when init then ok")
        void init_ok() throws Exception {
            // given
            AuthServlet s = new AuthServlet();
            when(config.getServletContext()).thenReturn(ctx);
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_SERVICE, UserService.class))
                        .thenReturn(userService);
                // when
                s.init(config);
                // then
                web.verify(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_SERVICE, UserService.class));
            }
        }

        @Test
        @DisplayName("given no UserService in ctx when init then UnavailableException")
        void init_missing_service() {
            // given
            AuthServlet s = new AuthServlet();
            when(config.getServletContext()).thenReturn(ctx);
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_SERVICE, UserService.class))
                        .thenThrow(new IllegalStateException("no bean"));
                // when / then
                assertThrows(UnavailableException.class, () -> s.init(config));
            }
        }
    }

    @Nested
    @DisplayName("doGet")
    class DoGet {

        @Test
        @DisplayName("given path=/login when doGet then forward to login jsp")
        void get_login() throws Exception {
            // given
            AuthServlet s = new AuthServlet();
            when(req.getServletPath()).thenReturn(WebConst.Path.LOGIN);
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.LOGIN))).then(inv -> null);
                // when
                s.doGet(req, resp);
                // then
                web.verify(() -> Web.forward(req, resp, WebConst.Jsp.LOGIN));
            }
        }

        @Test
        @DisplayName("given path=/register when doGet then forward to register jsp")
        void get_register() throws Exception {
            // given
            AuthServlet s = new AuthServlet();
            when(req.getServletPath()).thenReturn(WebConst.Path.REGISTER);
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.REGISTER))).then(inv -> null);
                // when
                s.doGet(req, resp);
                // then
                web.verify(() -> Web.forward(req, resp, WebConst.Jsp.REGISTER));
            }
        }

        @Test
        @DisplayName("given unknown path when doGet then 404")
        void get_unknown() throws Exception {
            // given
            AuthServlet s = new AuthServlet();
            when(req.getServletPath()).thenReturn("/unknown");
            // when
            s.doGet(req, resp);
            // then
            verify(resp).sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("doPost /login")
    class PostLogin {

        @Test
        @DisplayName("given correct creds when login then renew session and redirect to safe next")
        void login_ok_redirect() throws Exception {
            // given
            AuthServlet s = new AuthServlet();
            setField(s, userService);
            when(req.getServletPath()).thenReturn(WebConst.Path.LOGIN);
            when(req.getParameter(WebConst.Param.USER_LOGIN)).thenReturn("john");
            when(req.getParameter(WebConst.Param.PASSWORD)).thenReturn("pw");
            when(req.getParameter(WebConst.Param.NEXT)).thenReturn("/ctx/quest");
            when(req.getContextPath()).thenReturn("/ctx");
            when(resp.encodeRedirectURL(anyString())).thenAnswer(a -> a.getArgument(0));
            when(userService.login("john", "pw")).thenReturn(Optional.of(user));
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.renewSessionAndPut(eq(req), eq(WebConst.Attr.USER), eq(user))).then(inv -> null);
                // when
                s.doPost(req, resp);
                // then
                web.verify(() -> Web.renewSessionAndPut(eq(req), eq(WebConst.Attr.USER), eq(user)));
                verify(resp).sendRedirect("/ctx/quest");
            }
        }

        @Test
        @DisplayName("given bad creds when login then set error, keep login and forward to login jsp")
        void login_bad_creds() throws Exception {
            // given
            AuthServlet s = new AuthServlet();
            setField(s, userService);
            when(req.getServletPath()).thenReturn(WebConst.Path.LOGIN);
            when(req.getParameter(WebConst.Param.USER_LOGIN)).thenReturn("john");
            when(req.getParameter(WebConst.Param.PASSWORD)).thenReturn("bad");
            when(userService.login("john", "bad")).thenReturn(Optional.empty());
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.LOGIN))).then(inv -> null);
                // when
                s.doPost(req, resp);
                // then
                verify(req).setAttribute(WebConst.Attr.ERROR, WebConst.Msg.BAD_CREDENTIALS);
                verify(req).setAttribute("userLogin", "john");
                web.verify(() -> Web.forward(req, resp, WebConst.Jsp.LOGIN));
                web.verify(() -> Web.renewSessionAndPut(any(), anyString(), any()), never());
            }
        }
    }

    @Nested
    @DisplayName("doPost /register")
    class PostRegister {

        @Test
        @DisplayName("given correct data when register then renew session and redirect to home if next not safe")
        void register_ok_redirect_home() throws Exception {
            // given
            AuthServlet s = new AuthServlet();
            setField(s, userService);
            when(req.getServletPath()).thenReturn(WebConst.Path.REGISTER);
            when(req.getParameter(WebConst.Param.USER_NAME)).thenReturn("John");
            when(req.getParameter(WebConst.Param.USER_LOGIN)).thenReturn("john");
            when(req.getParameter(WebConst.Param.PASSWORD)).thenReturn("pw");
            when(req.getParameter(WebConst.Param.NEXT)).thenReturn(null);
            when(req.getContextPath()).thenReturn("/ctx");
            when(resp.encodeRedirectURL(anyString())).thenAnswer(a -> a.getArgument(0));
            when(userService.register(Role.USER, "John", "john", "pw")).thenReturn(user);
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.renewSessionAndPut(eq(req), eq(WebConst.Attr.USER), eq(user))).then(inv -> null);
                // when
                s.doPost(req, resp);
                // then
                web.verify(() -> Web.renewSessionAndPut(eq(req), eq(WebConst.Attr.USER), eq(user)));
                verify(resp).sendRedirect("/ctx/");
            }
        }

        @Test
        @DisplayName("given duplicate login when register then set error and forward to register jsp")
        void register_duplicate() throws Exception {
            // given
            AuthServlet s = new AuthServlet();
            setField(s, userService);
            when(req.getServletPath()).thenReturn(WebConst.Path.REGISTER);
            when(req.getParameter(WebConst.Param.USER_NAME)).thenReturn("John");
            when(req.getParameter(WebConst.Param.USER_LOGIN)).thenReturn("john");
            when(req.getParameter(WebConst.Param.PASSWORD)).thenReturn("pw");
            when(userService.register(Role.USER, "John", "john", "pw"))
                    .thenThrow(new DuplicateLoginException("exists"));
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.REGISTER))).then(inv -> null);
                // when
                s.doPost(req, resp);
                // then
                verify(req).setAttribute(WebConst.Attr.ERROR, "exists");
                verify(req).setAttribute("userName", "John");
                verify(req).setAttribute("userLogin", "john");
                web.verify(() -> Web.forward(req, resp, WebConst.Jsp.REGISTER));
            }
        }

        @Test
        @DisplayName("given illegal args when register then set error=ex.getMessage and forward to register jsp")
        void register_illegal_args() throws Exception {
            // given
            AuthServlet s = new AuthServlet();
            setField(s, userService);
            when(req.getServletPath()).thenReturn(WebConst.Path.REGISTER);
            when(req.getParameter(WebConst.Param.USER_NAME)).thenReturn("J");
            when(req.getParameter(WebConst.Param.USER_LOGIN)).thenReturn("j");
            when(req.getParameter(WebConst.Param.PASSWORD)).thenReturn("p");
            when(userService.register(Role.USER, "J", "j", "p"))
                    .thenThrow(new IllegalArgumentException("bad"));
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.REGISTER))).then(inv -> null);
                // when
                s.doPost(req, resp);
                // then
                verify(req).setAttribute(WebConst.Attr.ERROR, "bad");
                verify(req).setAttribute("userName", "J");
                verify(req).setAttribute("userLogin", "j");
                web.verify(() -> Web.forward(req, resp, WebConst.Jsp.REGISTER));
            }
        }

        @Test
        @DisplayName("given internal state error when register then set generic error and forward to register jsp")
        void register_internal_error() throws Exception {
            // given
            AuthServlet s = new AuthServlet();
            setField(s, userService);
            when(req.getServletPath()).thenReturn(WebConst.Path.REGISTER);
            when(req.getParameter(WebConst.Param.USER_NAME)).thenReturn("U");
            when(req.getParameter(WebConst.Param.USER_LOGIN)).thenReturn("u");
            when(req.getParameter(WebConst.Param.PASSWORD)).thenReturn("p");
            when(userService.register(Role.USER, "U", "u", "p"))
                    .thenThrow(new IllegalStateException("boom"));
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.REGISTER))).then(inv -> null);
                // when
                s.doPost(req, resp);
                // then
                verify(req).setAttribute(WebConst.Attr.ERROR, WebConst.Msg.INTERNAL_ERROR);
                verify(req).setAttribute("userName", "U");
                verify(req).setAttribute("userLogin", "u");
                web.verify(() -> Web.forward(req, resp, WebConst.Jsp.REGISTER));
            }
        }
    }

    @Test
    @DisplayName("given unknown path when doPost then 404")
    void post_unknown() throws Exception {
        // given
        AuthServlet s = new AuthServlet();
        when(req.getServletPath()).thenReturn("/nope");
        // when
        s.doPost(req, resp);
        // then
        verify(resp).sendError(HttpServletResponse.SC_NOT_FOUND);
    }
}