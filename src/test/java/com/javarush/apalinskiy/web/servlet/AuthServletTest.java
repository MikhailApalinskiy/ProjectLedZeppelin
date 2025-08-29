package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.exceptions.DuplicateLoginException;
import com.javarush.apalinskiy.service.UserService;

import com.javarush.apalinskiy.user.Role;
import com.javarush.apalinskiy.user.User;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServletTest {

    @Mock
    private HttpServletRequest req;
    @Mock
    private HttpServletResponse resp;
    @Mock
    private RequestDispatcher dispatcher;
    @Mock
    private ServletContext context;
    @Mock
    private HttpSession oldSession;
    @Mock
    private HttpSession newSession;
    @Mock
    private ServletConfig config;
    @Mock
    private UserService userService;
    private AuthServlet servlet;


    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        servlet = new AuthServlet();
        Field f = AuthServlet.class.getDeclaredField("userService");
        f.setAccessible(true);
        f.set(servlet, userService);
    }

    @Nested
    class Init {

        @Test
        void whenUserServicePresentSetsFieldAndCallsGettersTest() throws Exception {
            // given
            when(config.getServletContext()).thenReturn(context);
            when(context.getAttribute("userService")).thenReturn(userService);
            // when
            servlet.init(config);
            // then
            verify(config, atLeastOnce()).getServletContext();
            verify(context, times(1)).getAttribute("userService");
            verifyNoMoreInteractions(context);
            Field f = AuthServlet.class.getDeclaredField("userService");
            f.setAccessible(true);
            assertSame(userService, f.get(servlet));
        }

        @Test
        void whenUserServiceMissingThrowsUnavailableExceptionTest() {
            // given
            when(config.getServletContext()).thenReturn(context);
            when(context.getAttribute("userService")).thenReturn(null);
            // when / then
            UnavailableException ex = assertThrows(UnavailableException.class, () -> servlet.init(config));
            assertEquals("UserService not found in ServletContext (attribute 'userService').", ex.getMessage());
        }
    }

    @Nested
    class DoGet {

        @Test
        void loginForwardsToLoginJspWithoutRedirectOrErrorOrSessionTest() throws Exception {
            // given
            when(req.getServletPath()).thenReturn("/login");
            when(req.getRequestDispatcher("/WEB-INF/jsp/login.jsp")).thenReturn(dispatcher);
            // when
            servlet.doGet(req, resp);
            // then
            verify(req).getRequestDispatcher("/WEB-INF/jsp/login.jsp");
            verify(dispatcher).forward(req, resp);
            verify(resp, never()).sendRedirect(anyString());
            verify(resp, never()).sendError(anyInt());
            verify(req, never()).getSession();
        }

        @Test
        void registerForwardsToRegisterJspWithoutRedirectOrErrorOrSessionTest() throws Exception {
            // given
            when(req.getServletPath()).thenReturn("/register");
            when(req.getRequestDispatcher("/WEB-INF/jsp/register.jsp")).thenReturn(dispatcher);
            // when
            servlet.doGet(req, resp);
            // then
            verify(req).getRequestDispatcher("/WEB-INF/jsp/register.jsp");
            verify(dispatcher).forward(req, resp);
            verify(resp, never()).sendRedirect(anyString());
            verify(resp, never()).sendError(anyInt());
            verify(req, never()).getSession();
        }

        @Test
        void unknownPathReturns404Test() throws Exception {
            // given
            when(req.getServletPath()).thenReturn("/");
            // when
            servlet.doGet(req, resp);
            // then
            verify(resp).sendError(HttpServletResponse.SC_NOT_FOUND);
            verify(req, never()).getRequestDispatcher(anyString());
            verifyNoInteractions(userService);
        }

        @Test
        void forwardThrowsServletExceptionIsPropagatedTest() throws Exception {
            // given
            when(req.getServletPath()).thenReturn("/register");
            when(req.getRequestDispatcher("/WEB-INF/jsp/register.jsp")).thenReturn(dispatcher);
            doThrow(new ServletException()).when(dispatcher).forward(any(), any());
            // when / then
            assertThrows(ServletException.class, () -> servlet.doGet(req, resp));
        }

        @Test
        void forwardThrowsIOExceptionIsPropagatedTest() throws Exception {
            // given
            when(req.getServletPath()).thenReturn("/register");
            when(req.getRequestDispatcher("/WEB-INF/jsp/register.jsp")).thenReturn(dispatcher);
            doThrow(new IOException()).when(dispatcher).forward(any(), any());
            // when / then
            assertThrows(IOException.class, () -> servlet.doGet(req, resp));
        }
    }

    @Nested
    class Login {

        @Nested
        class SuccessOldSession {

            @BeforeEach
            void arrange() {
                when(req.getServletPath()).thenReturn("/login");
                when(req.getParameter("userLogin")).thenReturn("alice");
                when(req.getParameter("password")).thenReturn("pwd");
                when(req.getSession(false)).thenReturn(oldSession);
                when(req.getSession(true)).thenReturn(newSession);
            }

            @Test
            void callsServiceOnceWithSameParamsTest() throws Exception {
                // given
                User u = User.of(Role.USER, "Alice", "alice", "pwd");
                when(userService.login("alice", "pwd")).thenReturn(Optional.of(u));
                when(req.getContextPath()).thenReturn("");
                // when
                servlet.doPost(req, resp);
                // then
                verify(userService, times(1)).login("alice", "pwd");
            }

            @Test
            void invalidatesOldSessionCreatesNewSetsAttrAndRedirectsInOrderTest() throws Exception {
                // given
                User u = User.of(Role.USER, "Alice", "alice", "pwd");
                when(userService.login("alice", "pwd")).thenReturn(Optional.of(u));
                when(req.getContextPath()).thenReturn("");
                // when
                servlet.doPost(req, resp);
                // then
                InOrder inOrder = inOrder(oldSession, req, newSession, resp);
                inOrder.verify(oldSession).invalidate();
                inOrder.verify(req).getSession(true);
                inOrder.verify(newSession).setAttribute("user", u);
                inOrder.verify(resp).sendRedirect("/");
                verify(resp, never()).sendError(anyInt());
            }

            @ParameterizedTest(name = "contextPath=\"{0}\" -> redirect \"{1}\"")
            @CsvSource({"'', /", "/app, /app/", "/root, /root/"})
            void redirectsToContextRootTest(String ctx, String expected) throws Exception {
                // given
                User u = User.of(Role.USER, "Alice", "alice", "pwd");
                when(userService.login("alice", "pwd")).thenReturn(Optional.of(u));
                when(req.getContextPath()).thenReturn(ctx);
                // when
                servlet.doPost(req, resp);
                // then
                verify(resp).sendRedirect(expected);
                verify(req, never()).getRequestDispatcher(anyString());
            }
        }

        @Nested
        class SuccessNoOldSession {

            @BeforeEach
            void arrange() {
                when(req.getServletPath()).thenReturn("/login");
                when(req.getParameter("userLogin")).thenReturn("alice");
                when(req.getParameter("password")).thenReturn("pwd");
                when(req.getSession(false)).thenReturn(null);
                when(req.getSession(true)).thenReturn(newSession);
                when(req.getContextPath()).thenReturn("/");
            }

            @Test
            void createsNewSessionSetsUserNoInvalidateTest() throws Exception {
                // given
                User u = User.of(Role.USER, "Alice", "alice", "pwd");
                when(userService.login("alice", "pwd")).thenReturn(Optional.of(u));
                // when
                servlet.doPost(req, resp);
                // then
                verify(req).getSession(true);
                verify(newSession).setAttribute("user", u);
                verify(oldSession, never()).invalidate();
                verify(req, never()).getRequestDispatcher(anyString());
            }
        }

        @Nested
        class Failure {

            @BeforeEach
            void arrange() {
                when(req.getServletPath()).thenReturn("/login");
                when(req.getParameter("userLogin")).thenReturn("alice");
                when(req.getParameter("password")).thenReturn("bad");
                when(req.getRequestDispatcher("/WEB-INF/jsp/login.jsp")).thenReturn(dispatcher);
                when(userService.login("alice", "bad")).thenReturn(Optional.empty());
            }

            @Test
            void setsErrorAndLoginThenForwardsNoSessionNoRedirectOrErrorTest() throws Exception {
                // when
                servlet.doPost(req, resp);
                // then
                InOrder inOrder = inOrder(req, dispatcher);
                inOrder.verify(req).setAttribute("error", "Incorrect login or password");
                inOrder.verify(req).setAttribute("userLogin", "alice");
                inOrder.verify(dispatcher).forward(req, resp);
                verify(resp, never()).sendRedirect(anyString());
                verify(resp, never()).sendError(anyInt());
                verify(req, never()).getSession(anyBoolean());
                verify(req, never()).getSession();
            }
        }

        @Test
        void runtimeFromServiceIsPropagatedAndNoRedirectOrForwardTest() throws Exception {
            // given
            when(req.getServletPath()).thenReturn("/login");
            when(req.getParameter("userLogin")).thenReturn("alice");
            when(req.getParameter("password")).thenReturn("pwd");
            when(userService.login("alice", "pwd")).thenThrow(new RuntimeException());
            // when / then
            assertThrows(RuntimeException.class, () -> servlet.doPost(req, resp));
            verify(resp, never()).sendRedirect(anyString());
            verify(resp, never()).sendError(anyInt());
        }
    }

    @Nested
    class Register {

        @Nested
        class SuccessOldSession {

            @BeforeEach
            void arrange() {
                when(req.getServletPath()).thenReturn("/register");
                when(req.getParameter("userName")).thenReturn("Alice");
                when(req.getParameter("userLogin")).thenReturn("alice");
                when(req.getParameter("password")).thenReturn("pwd");
                when(req.getSession(false)).thenReturn(oldSession);
                when(req.getSession(true)).thenReturn(newSession);
            }

            @Test
            void callsServiceOnceWithRoleUserTest() throws Exception {
                // given
                User u = User.of(Role.USER, "Alice", "alice", "pwd");
                when(userService.register(Role.USER, "Alice", "alice", "pwd")).thenReturn(u);
                when(req.getContextPath()).thenReturn("");
                // when
                servlet.doPost(req, resp);
                // then
                verify(userService).register(Role.USER, "Alice", "alice", "pwd");
            }

            @Test
            void invalidatesOldCreatesNewSetsUserAndRedirectsTest() throws Exception {
                // given
                User u = User.of(Role.USER, "Alice", "alice", "pwd");
                when(userService.register(Role.USER, "Alice", "alice", "pwd")).thenReturn(u);
                when(req.getContextPath()).thenReturn("");
                // when
                servlet.doPost(req, resp);
                // then
                InOrder inOrder = inOrder(oldSession, req, newSession, resp);
                inOrder.verify(oldSession).invalidate();
                inOrder.verify(req).getSession(true);
                inOrder.verify(newSession).setAttribute("user", u);
                inOrder.verify(resp).sendRedirect("/");
                verify(resp, never()).sendError(anyInt());
            }

            @ParameterizedTest(name = "contextPath=\"{0}\" -> redirect \"{1}\"")
            @CsvSource({"'', /", "/app, /app/", "/root, /root/"})
            void redirectsToContextRootTest(String ctx, String expected) throws Exception {
                // given
                User u = User.of(Role.USER, "Alice", "alice", "pwd");
                when(userService.register(Role.USER, "Alice", "alice", "pwd")).thenReturn(u);
                when(req.getContextPath()).thenReturn(ctx);
                // when
                servlet.doPost(req, resp);
                // then
                verify(resp).sendRedirect(expected);
                verify(req, never()).getRequestDispatcher(anyString());
            }
        }

        @Nested
        class UserErrorDuplicateLogin {

            @BeforeEach
            void arrange() {
                when(req.getServletPath()).thenReturn("/register");
                when(req.getParameter("userName")).thenReturn("Alice");
                when(req.getParameter("userLogin")).thenReturn("alice");
                when(req.getParameter("password")).thenReturn("pwd");
                when(req.getRequestDispatcher("/WEB-INF/jsp/register.jsp")).thenReturn(dispatcher);
            }

            @Test
            void forwardsWithMessageNoSessionNoRedirectTest() throws Exception {
                // given
                when(userService.register(Role.USER, "Alice", "alice", "pwd"))
                        .thenThrow(new DuplicateLoginException("Login already exists"));
                // when
                servlet.doPost(req, resp);
                // then
                InOrder inOrder = inOrder(req, dispatcher);
                inOrder.verify(req).setAttribute("error", "Login already exists");
                inOrder.verify(req).setAttribute("userName", "Alice");
                inOrder.verify(req).setAttribute("userLogin", "alice");
                inOrder.verify(dispatcher).forward(req, resp);
                verify(resp, never()).sendRedirect(anyString());
                verify(req, never()).getSession(anyBoolean());
                verify(resp, never()).sendError(anyInt());
            }
        }

        @Nested
        class UserErrorIllegalArgument {

            @BeforeEach
            void arrange() {
                when(req.getServletPath()).thenReturn("/register");
                when(req.getParameter("userName")).thenReturn(" ");
                when(req.getParameter("userLogin")).thenReturn("alice");
                when(req.getParameter("password")).thenReturn("pwd");
                when(req.getRequestDispatcher("/WEB-INF/jsp/register.jsp")).thenReturn(dispatcher);
            }

            @Test
            void forwardsWithMessageNoSessionNoRedirectTest() throws Exception {
                // given
                when(userService.register(Role.USER, " ", "alice", "pwd"))
                        .thenThrow(new IllegalArgumentException("Username or login or password are required"));
                // when
                servlet.doPost(req, resp);
                // then
                InOrder inOrder = inOrder(req, dispatcher);
                inOrder.verify(req).setAttribute("error", "Username or login or password are required");
                inOrder.verify(req).setAttribute("userName", " ");
                inOrder.verify(req).setAttribute("userLogin", "alice");
                inOrder.verify(dispatcher).forward(req, resp);
                verify(resp, never()).sendRedirect(anyString());
                verify(req, never()).getSession(anyBoolean());
                verify(resp, never()).sendError(anyInt());
            }
        }

        @Nested
        class InternalError {

            @BeforeEach
            void arrange() {
                when(req.getServletPath()).thenReturn("/register");
                when(req.getParameter("userName")).thenReturn("Alice");
                when(req.getParameter("userLogin")).thenReturn("alice");
                when(req.getParameter("password")).thenReturn("pwd");
                when(req.getRequestDispatcher("/WEB-INF/jsp/register.jsp")).thenReturn(dispatcher);
            }

            @Test
            void forwardsWithGenericMessageNoSessionNoRedirectTest() throws Exception {
                // given
                when(userService.register(Role.USER, "Alice", "alice", "pwd"))
                        .thenThrow(new IllegalStateException("boom"));
                // when
                servlet.doPost(req, resp);
                // then
                InOrder inOrder = inOrder(req, dispatcher);
                inOrder.verify(req).setAttribute("error", "Internal error. Please try again.");
                inOrder.verify(req).setAttribute("userName", "Alice");
                inOrder.verify(req).setAttribute("userLogin", "alice");
                inOrder.verify(dispatcher).forward(req, resp);
                verify(resp, never()).sendRedirect(anyString());
                verify(req, never()).getSession(anyBoolean());
                verify(resp, never()).sendError(anyInt());
            }
        }

        @Nested
        class RegisterSuccessWithoutOldSession {

            @BeforeEach
            void arrange() {
                when(req.getServletPath()).thenReturn("/register");
                when(req.getParameter("userName")).thenReturn("Alice");
                when(req.getParameter("userLogin")).thenReturn("alice");
                when(req.getParameter("password")).thenReturn("pwd");
                when(req.getSession(false)).thenReturn(null);
                when(req.getSession(true)).thenReturn(newSession);
            }

            @Test
            void createsNewSessionSetsUserNoInvalidateAndCallsServiceOnceTest() throws Exception {
                // given
                User u = User.of(Role.USER, "Alice", "alice", "pwd");
                when(userService.register(Role.USER, "Alice", "alice", "pwd")).thenReturn(u);
                when(req.getContextPath()).thenReturn("");
                // when
                servlet.doPost(req, resp);
                // then
                verify(userService, times(1)).register(Role.USER, "Alice", "alice", "pwd");
                verify(req, times(1)).getSession(false);
                verify(req, times(1)).getSession(true);
                verify(newSession, times(1)).setAttribute("user", u);
                verify(oldSession, never()).invalidate();              // <- эта ветка и покрывает old == null
                verify(resp, times(1)).sendRedirect("/");
            }

            @ParameterizedTest(name = "contextPath=\"{0}\" -> redirect \"{1}\"")
            @CsvSource({"'', /", "/app, /app/", "/root, /root/"})
            void redirectsToContextRootWithoutOldSessionTest(String ctx, String expected) throws Exception {
                // given
                User u = User.of(Role.USER, "Alice", "alice", "pwd");
                when(userService.register(Role.USER, "Alice", "alice", "pwd")).thenReturn(u);
                when(req.getContextPath()).thenReturn(ctx);
                // when
                servlet.doPost(req, resp);
                // then
                verify(resp).sendRedirect(expected);
                verify(oldSession, never()).invalidate();
                verify(req, never()).getRequestDispatcher(anyString());
            }
        }
    }

    @Nested
    class OtherPath {

        @Test
        void unknownPathReturns404AndDoesNotCallServiceTest() throws Exception {
            // given
            when(req.getServletPath()).thenReturn("/unknown");
            // when
            servlet.doPost(req, resp);
            // then
            verify(resp).sendError(HttpServletResponse.SC_NOT_FOUND);
            verify(req, never()).getRequestDispatcher(anyString());
            verifyNoInteractions(userService);
        }
    }
}