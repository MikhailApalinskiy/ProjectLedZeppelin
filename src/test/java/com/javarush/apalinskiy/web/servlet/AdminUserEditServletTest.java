package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.app.WebConst;
import com.javarush.apalinskiy.domain.notify.NotificationEvent;
import com.javarush.apalinskiy.domain.user.Role;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.exceptions.DuplicateLoginException;
import com.javarush.apalinskiy.service.notify.NotificationService;
import com.javarush.apalinskiy.service.user.UserService;
import com.javarush.apalinskiy.web.util.Web;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@DisplayName("AdminUserEditServlet (unit)")
@ExtendWith(MockitoExtension.class)
class AdminUserEditServletTest {

    @Mock
    ServletConfig config;
    @Mock
    ServletContext ctx;
    @Mock
    UserService users;
    @Mock
    NotificationService notifySvc;
    @Mock
    HttpServletRequest req;
    @Mock
    HttpServletResponse resp;
    @Mock
    HttpSession session;

    private AdminUserEditServlet sut;

    private User realUser(String id, String login, String name, Role role) {
        User u = new User();
        u.setUserId(id);
        u.setUserLogin(login);
        u.setUserName(name);
        u.setRole(role);
        return u;
    }

    @Nested
    @DisplayName("init(config)")
    class InitPhase {

        @Test
        @DisplayName("Given beans present in ServletContext — When init — Then ctxBean called and fields set")
        void initHappy() throws ServletException {
            // Given
            sut = new AdminUserEditServlet();
            when(config.getServletContext()).thenReturn(ctx);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_SERVICE, UserService.class))
                        .thenReturn(users);
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.NOTIFY_SERVICE, NotificationService.class))
                        .thenReturn(notifySvc);
                // When
                sut.init(config);
                // Then
                web.verify(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_SERVICE, UserService.class));
                web.verify(() -> Web.ctxBean(ctx, WebConst.Ctx.NOTIFY_SERVICE, NotificationService.class));
            }
        }
    }

    private void initServletWithBeans() throws ServletException {
        sut = new AdminUserEditServlet();
        when(config.getServletContext()).thenReturn(ctx);
        try (MockedStatic<Web> web = mockStatic(Web.class)) {
            web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_SERVICE, UserService.class))
                    .thenReturn(users);
            web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.NOTIFY_SERVICE, NotificationService.class))
                    .thenReturn(notifySvc);
            sut.init(config);
        }
    }

    @Nested
    @DisplayName("doGet(req, resp)")
    class DoGet {

        @BeforeEach
        void setUp() throws ServletException {
            initServletWithBeans();
        }

        @Test
        @DisplayName("Given missing id — When doGet — Then redirectErr to USERS with message")
        void missingId() throws Exception {
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // Given
                web.when(() -> Web.pullFlash(req, WebConst.Attr.OK)).thenAnswer(inv -> null);
                web.when(() -> Web.pullFlash(req, WebConst.Attr.ERROR)).thenAnswer(inv -> null);
                when(req.getParameter(WebConst.Param.ID)).thenReturn(null);
                web.when(() -> Web.trimOrNull(null)).thenReturn(null);
                // When
                sut.doGet(req, resp);
                // Then
                web.verify(() -> Web.redirectErr(req, resp, WebConst.Path.USERS, "Missing user id"));
            }
        }

        @Test
        @DisplayName("Given id provided but user not found — When doGet — Then redirectErr to USERS")
        void userNotFound() throws Exception {
            //Given
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.pullFlash(req, WebConst.Attr.OK)).thenAnswer(inv -> null);
                web.when(() -> Web.pullFlash(req, WebConst.Attr.ERROR)).thenAnswer(inv -> null);
                when(req.getParameter(WebConst.Param.ID)).thenReturn("u3");
                web.when(() -> Web.trimOrNull("u3")).thenReturn("u3");
                when(users.findById("u3")).thenReturn(Optional.empty());
                //When
                sut.doGet(req, resp);
                //Then
                web.verify(() -> Web.redirectErr(req, resp, WebConst.Path.USERS, "User not found"));
            }
        }

        @Test
        @DisplayName("Given id and user exists — When doGet — Then set attributes and forward to JSP")
        void happyPath() throws Exception {
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.pullFlash(req, WebConst.Attr.OK)).thenAnswer(inv -> null);
                web.when(() -> Web.pullFlash(req, WebConst.Attr.ERROR)).thenAnswer(inv -> null);
                when(req.getParameter(WebConst.Param.ID)).thenReturn("u1");
                web.when(() -> Web.trimOrNull("u1")).thenReturn("u1");
                User target = realUser("u1", "login1", "Name1", Role.USER);
                when(users.findById("u1")).thenReturn(Optional.of(target));
                // When
                sut.doGet(req, resp);
                // Then
                verify(req).setAttribute(eq("editUser"), same(target));
                @SuppressWarnings("unchecked")
                ArgumentCaptor<List<Role>> cap = ArgumentCaptor.forClass(List.class);
                verify(req).setAttribute(eq("roles"), cap.capture());
                List<Role> roles = cap.getValue();
                assertTrue(roles.contains(Role.USER) && roles.contains(Role.ADMIN));
                web.verify(() -> Web.forward(req, resp, WebConst.Jsp.USER_EDIT));
            }
        }
    }

    @Nested
    @DisplayName("doPost(req, resp)")
    class DoPost {

        @BeforeEach
        void setUp() throws ServletException {
            initServletWithBeans();
        }

        @Test
        @DisplayName("Given missing id — When doPost — Then redirectErr to USERS")
        void missingId() throws Exception {
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                when(req.getParameter(WebConst.Param.ID)).thenReturn(null);
                web.when(() -> Web.trimOrNull(null)).thenReturn(null);
                sut.doPost(req, resp);
                web.verify(() -> Web.redirectErr(req, resp, WebConst.Path.USERS, "Missing user id"));
            }
        }

        @Test
        @DisplayName("Given valid self-update and sensitiveChanged=true — When doPost — Then renewSessionAndPut, notify, redirectOk")
        void selfUpdateSensitiveChanged() throws Exception {
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // Given
                when(req.getParameter(WebConst.Param.ID)).thenReturn("u1");
                when(req.getParameter(WebConst.Param.USER_NAME)).thenReturn("New Name");
                when(req.getParameter(WebConst.Param.USER_LOGIN)).thenReturn("newlogin");
                when(req.getParameter(WebConst.Param.ROLE)).thenReturn("ADMIN");
                when(req.getParameter(WebConst.Param.PASSWORD)).thenReturn("secret");
                web.when(() -> Web.trimOrNull("u1")).thenReturn("u1");
                web.when(() -> Web.trimOrNull("New Name")).thenReturn("New Name");
                web.when(() -> Web.trimOrNull("newlogin")).thenReturn("newlogin");
                web.when(() -> Web.trimOrNull("ADMIN")).thenReturn("ADMIN");
                web.when(() -> Web.trimOrNull("secret")).thenReturn("secret");
                User before = realUser("u1", "oldlogin", "Old Name", Role.USER);
                User updated = realUser("u1", "newlogin", "New Name", Role.ADMIN);
                when(users.findById("u1")).thenReturn(Optional.of(before));
                when(users.adminUpdate("u1", Role.ADMIN, "New Name", "newlogin", "secret")).thenReturn(updated);
                when(req.getSession(false)).thenReturn(session);
                when(session.getAttribute(WebConst.Attr.USER)).thenReturn(updated); // self-update
                web.when(() -> Web.sensitiveChanged(before, updated, true)).thenReturn(true);
                web.when(() -> Web.buildAdminChangeSummary(before, updated, true)).thenReturn("Changed");
                web.when(() -> Web.buildMachineReadableDiff(before, updated, true)).thenReturn(Map.of("k", "v"));
                // When
                sut.doPost(req, resp);
                // Then
                web.verify(() -> Web.renewSessionAndPut(req, WebConst.Attr.USER, updated));
                verify(session, never()).setAttribute(anyString(), any());
                verify(notifySvc).notify(any(NotificationEvent.class));
                web.verify(() -> Web.redirectOk(eq(req), eq(resp),
                        eq(WebConst.Path.USER_EDIT + "?" + WebConst.Param.ID + "=u1"),
                        eq("The user has been updated")));
            }
        }

        @Test
        @DisplayName("Given valid self-update and sensitiveChanged=false — When doPost — Then session.setAttribute, notify, redirectOk")
        void selfUpdateNotSensitive() throws Exception {
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                //Given
                when(req.getParameter(WebConst.Param.ID)).thenReturn("u1");
                when(req.getParameter(WebConst.Param.USER_NAME)).thenReturn("Same Name");
                when(req.getParameter(WebConst.Param.USER_LOGIN)).thenReturn("same");
                when(req.getParameter(WebConst.Param.ROLE)).thenReturn("USER");
                when(req.getParameter(WebConst.Param.PASSWORD)).thenReturn(null);
                web.when(() -> Web.trimOrNull("u1")).thenReturn("u1");
                web.when(() -> Web.trimOrNull("Same Name")).thenReturn("Same Name");
                web.when(() -> Web.trimOrNull("same")).thenReturn("same");
                web.when(() -> Web.trimOrNull("USER")).thenReturn("USER");
                web.when(() -> Web.trimOrNull(null)).thenReturn(null);
                User before = realUser("u1", "same", "Same Name", Role.USER);
                User updated = realUser("u1", "same", "Same Name", Role.USER);
                when(users.findById("u1")).thenReturn(Optional.of(before));
                when(users.adminUpdate("u1", Role.USER, "Same Name", "same", null)).thenReturn(updated);
                when(req.getSession(false)).thenReturn(session);
                when(session.getAttribute(WebConst.Attr.USER)).thenReturn(updated); // self-update
                web.when(() -> Web.sensitiveChanged(before, updated, false)).thenReturn(false);
                web.when(() -> Web.buildAdminChangeSummary(before, updated, false)).thenReturn("Changed");
                web.when(() -> Web.buildMachineReadableDiff(before, updated, false)).thenReturn(Map.of());
                //When
                sut.doPost(req, resp);
                //Then
                verify(session).setAttribute(WebConst.Attr.USER, updated);
                web.verify(() -> Web.renewSessionAndPut(any(), anyString(), any()), times(0));
                verify(notifySvc).notify(any(NotificationEvent.class));
                web.verify(() -> Web.redirectOk(eq(req), eq(resp),
                        eq(WebConst.Path.USER_EDIT + "?" + WebConst.Param.ID + "=u1"),
                        eq("The user has been updated")));
            }
        }

        @Test
        @DisplayName("Given update but 'No visible changes' — When doPost — Then no notify, just redirectOk")
        void noVisibleChangesNoNotify() throws Exception {
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                //Given
                when(req.getParameter(WebConst.Param.ID)).thenReturn("u1");
                when(req.getParameter(WebConst.Param.USER_NAME)).thenReturn("A");
                when(req.getParameter(WebConst.Param.USER_LOGIN)).thenReturn("a");
                when(req.getParameter(WebConst.Param.ROLE)).thenReturn("USER");
                when(req.getParameter(WebConst.Param.PASSWORD)).thenReturn(null);
                web.when(() -> Web.trimOrNull("u1")).thenReturn("u1");
                web.when(() -> Web.trimOrNull("A")).thenReturn("A");
                web.when(() -> Web.trimOrNull("a")).thenReturn("a");
                web.when(() -> Web.trimOrNull("USER")).thenReturn("USER");
                web.when(() -> Web.trimOrNull(null)).thenReturn(null);
                User before = realUser("u1", "a", "A", Role.USER);
                User updated = realUser("u1", "a", "A", Role.USER);
                when(users.findById("u1")).thenReturn(Optional.of(before));
                when(users.adminUpdate("u1", Role.USER, "A", "a", null)).thenReturn(updated);
                when(req.getSession(false)).thenReturn(null);
                web.when(() -> Web.buildAdminChangeSummary(before, updated, false)).thenReturn("No visible changes");
                web.when(() -> Web.buildMachineReadableDiff(before, updated, false)).thenReturn(Map.of());
                //When
                sut.doPost(req, resp);
                //Then
                verify(notifySvc, never()).notify(any());
                web.verify(() -> Web.redirectOk(eq(req), eq(resp),
                        eq(WebConst.Path.USER_EDIT + "?" + WebConst.Param.ID + "=u1"),
                        eq("The user has been updated")));
            }
        }

        @Test
        @DisplayName("Given DuplicateLoginException — When doPost — Then redirectErr back to edit with message")
        void duplicateLogin() throws Exception {
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                //Given
                when(req.getParameter(WebConst.Param.ID)).thenReturn("u3");
                when(req.getParameter(WebConst.Param.USER_NAME)).thenReturn("N");
                when(req.getParameter(WebConst.Param.USER_LOGIN)).thenReturn("exists");
                when(req.getParameter(WebConst.Param.ROLE)).thenReturn("ADMIN");
                when(req.getParameter(WebConst.Param.PASSWORD)).thenReturn(null);
                web.when(() -> Web.trimOrNull("u3")).thenReturn("u3");
                web.when(() -> Web.trimOrNull("N")).thenReturn("N");
                web.when(() -> Web.trimOrNull("exists")).thenReturn("exists");
                web.when(() -> Web.trimOrNull("ADMIN")).thenReturn("ADMIN");
                web.when(() -> Web.trimOrNull(null)).thenReturn(null);
                when(users.findById("u3")).thenReturn(Optional.of(realUser("u3", "old", "Old", Role.USER)));
                when(users.adminUpdate(eq("u3"), eq(Role.ADMIN), any(), any(), isNull()))
                        .thenThrow(new DuplicateLoginException("dup"));
                //When
                sut.doPost(req, resp);
                //Then
                web.verify(() -> Web.redirectErr(eq(req), eq(resp),
                        eq(WebConst.Path.USER_EDIT + "?" + WebConst.Param.ID + "=u3"),
                        eq("The login is already occupied")));
            }
        }

        @Test
        @DisplayName("Given IllegalArgumentException — When doPost — Then redirectErr back with exception message")
        void illegalArgs() throws Exception {
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                //Given
                when(req.getParameter(WebConst.Param.ID)).thenReturn("bad");
                when(req.getParameter(WebConst.Param.USER_NAME)).thenReturn("X");
                when(req.getParameter(WebConst.Param.USER_LOGIN)).thenReturn("x");
                when(req.getParameter(WebConst.Param.ROLE)).thenReturn("USER");
                when(req.getParameter(WebConst.Param.PASSWORD)).thenReturn(null);
                web.when(() -> Web.trimOrNull("bad")).thenReturn("bad");
                web.when(() -> Web.trimOrNull("X")).thenReturn("X");
                web.when(() -> Web.trimOrNull("x")).thenReturn("x");
                web.when(() -> Web.trimOrNull("USER")).thenReturn("USER");
                web.when(() -> Web.trimOrNull(null)).thenReturn(null);
                when(users.findById("bad")).thenReturn(Optional.of(realUser("bad", "x", "X", Role.USER)));
                when(users.adminUpdate(eq("bad"), any(), any(), any(), isNull()))
                        .thenThrow(new IllegalArgumentException("Bad input"));
                //When
                sut.doPost(req, resp);
                //Then
                web.verify(() -> Web.redirectErr(eq(req), eq(resp),
                        eq(WebConst.Path.USER_EDIT + "?" + WebConst.Param.ID + "=bad"),
                        eq("Bad input")));
            }
        }
    }
}