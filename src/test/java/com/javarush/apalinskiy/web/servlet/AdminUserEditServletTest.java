package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.app.WebConst;
import com.javarush.apalinskiy.domain.user.Role;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.exceptions.DuplicateLoginException;
import com.javarush.apalinskiy.service.notify.NotificationService;
import com.javarush.apalinskiy.service.user.UserService;
import com.javarush.apalinskiy.web.util.Web;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
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

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.mockito.Mockito.*;

@DisplayName("AdminUserEditServlet")
@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class AdminUserEditServletTest {

    @Mock
    ServletConfig config;
    @Mock
    ServletContext ctx;
    @Mock
    HttpServletRequest req;
    @Mock
    HttpServletResponse resp;
    @Mock
    HttpSession session;
    @Mock
    UserService users;
    @Mock
    NotificationService notify;
    @Mock
    User userBefore;
    @Mock
    User userUpdated;
    @Mock
    User me;

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field f;
            try {
                f = target.getClass().getDeclaredField(fieldName);
            } catch (NoSuchFieldException ex) {
                f = target.getClass().getSuperclass().getDeclaredField(fieldName);
            }
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private AdminUserEditServlet newServletWithDeps() {
        AdminUserEditServlet s = spy(new AdminUserEditServlet());
        setField(s, "users", users);
        setField(s, "notify", notify);
        return s;
    }

    @Test
    @DisplayName("given ctx beans when init then services are resolved")
    void init_ok() throws Exception {
        // given
        AdminUserEditServlet s = new AdminUserEditServlet();
        when(config.getServletContext()).thenReturn(ctx);
        try (MockedStatic<Web> web = mockStatic(Web.class)) {
            web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_SERVICE, UserService.class)).thenReturn(users);
            web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.NOTIFY_SERVICE, NotificationService.class)).thenReturn(notify);
            // when
            s.init(config);
            // then
            web.verify(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_SERVICE, UserService.class));
            web.verify(() -> Web.ctxBean(ctx, WebConst.Ctx.NOTIFY_SERVICE, NotificationService.class));
        }
    }

    @Nested
    @DisplayName("doGet")
    class DoGet {

        @Test
        @DisplayName("given no id when doGet then redirectErr to USERS")
        void missingId() throws Exception {
            // given
            AdminUserEditServlet s = newServletWithDeps();
            when(req.getParameter(WebConst.Param.ID)).thenReturn(null);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.USERS), anyString())).then(inv -> null);
                // when
                s.doGet(req, resp);
                // then
                web.verify(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.USERS), eq("Missing user id")));
            }
        }

        @Test
        @DisplayName("given unknown id when doGet then redirectErr to USERS")
        void userNotFound() throws Exception {
            // given
            AdminUserEditServlet s = newServletWithDeps();
            when(req.getParameter(WebConst.Param.ID)).thenReturn("u1");
            when(users.findById("u1")).thenReturn(Optional.empty());
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.USERS), anyString())).then(inv -> null);
                // when
                s.doGet(req, resp);
                // then
                web.verify(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.USERS), eq("User not found")));
            }
        }

        @Test
        @DisplayName("given existing user when doGet then set attributes and forward to edit JSP")
        void okForwards() throws Exception {
            // given
            AdminUserEditServlet s = newServletWithDeps();
            User target = mock(User.class);
            when(req.getParameter(WebConst.Param.ID)).thenReturn("id");
            when(users.findById("id")).thenReturn(Optional.of(target));
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.USER_EDIT))).then(inv -> null);
                // when
                s.doGet(req, resp);
                // then
                verify(req).setAttribute("editUser", target);
                verify(req).setAttribute("roles", List.of(Role.USER, Role.ADMIN));
                web.verify(() -> Web.forward(req, resp, WebConst.Jsp.USER_EDIT));
            }
        }
    }

    @Nested
    @DisplayName("doPost validations")
    class DoPostValidations {

        @Test
        @DisplayName("given id=null when doPost then redirectErr to USERS")
        void missingId() throws Exception {
            // given
            AdminUserEditServlet s = newServletWithDeps();
            when(req.getParameter(WebConst.Param.ID)).thenReturn(null);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.USERS), anyString())).then(inv -> null);
                // when
                s.doPost(req, resp);
                // then
                web.verify(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.USERS), eq("Missing user id")));
            }
        }
    }

    @Nested
    @DisplayName("doPost updates")
    class DoPostUpdates {

        @BeforeEach
        void commonInput() {
            when(req.getParameter(WebConst.Param.ID)).thenReturn("u1");
            when(req.getParameter(WebConst.Param.USER_NAME)).thenReturn("New Name");
            when(req.getParameter(WebConst.Param.USER_LOGIN)).thenReturn("newlogin");
            when(req.getParameter(WebConst.Param.ROLE)).thenReturn("ADMIN");
            when(req.getParameter(WebConst.Param.PASSWORD)).thenReturn(null);
            when(users.findById("u1")).thenReturn(Optional.of(userBefore));
        }

        @Test
        @DisplayName("given updating another user when doPost then no session touch, notify (visible), redirectOk")
        void updateOtherUser_visibleChanges() throws Exception {
            // given
            AdminUserEditServlet s = newServletWithDeps();
            when(users.adminUpdate("u1", Role.ADMIN, "New Name", "newlogin", null)).thenReturn(userUpdated);
            when(req.getSession(false)).thenReturn(session);
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(me);
            when(me.getUserId()).thenReturn("admin");
            when(userUpdated.getUserId()).thenReturn("u1");
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.buildAdminChangeSummary(userBefore, userUpdated, false))
                        .thenReturn("role: USER → ADMIN");
                web.when(() -> Web.buildMachineReadableDiff(userBefore, userUpdated, false))
                        .thenReturn(Map.of("what", "role: USER → ADMIN"));
                web.when(() -> Web.redirectOk(eq(req), eq(resp), anyString(), anyString())).then(inv -> null);
                // when
                s.doPost(req, resp);
                // then
                verify(session, never()).setAttribute(eq(WebConst.Attr.USER), any());
                web.verify(() -> Web.renewSessionAndPut(any(), anyString(), any()), never());
                verify(notify, atLeastOnce()).notify(any());
                web.verify(() -> Web.redirectOk(eq(req), eq(resp),
                        eq(WebConst.Path.USER_EDIT + "?id=u1"),
                        eq("The user has been updated")));
            }
        }

        @Test
        @DisplayName("given updating myself and sensitiveChanged=true when doPost then renewSessionAndPut, notify, redirectOk")
        void updateSelf_sensitiveChanged() throws Exception {
            // given
            AdminUserEditServlet s = newServletWithDeps();
            when(users.adminUpdate("u1", Role.ADMIN, "New Name", "newlogin", null)).thenReturn(userUpdated);
            when(req.getSession(false)).thenReturn(session);
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(me);
            when(me.getUserId()).thenReturn("u1");
            when(userUpdated.getUserId()).thenReturn("u1");
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.sensitiveChanged(userBefore, userUpdated, false)).thenReturn(true);
                web.when(() -> Web.renewSessionAndPut(eq(req), eq(WebConst.Attr.USER), eq(userUpdated))).then(inv -> null);
                web.when(() -> Web.buildAdminChangeSummary(userBefore, userUpdated, false))
                        .thenReturn("login: old → new");
                web.when(() -> Web.buildMachineReadableDiff(userBefore, userUpdated, false))
                        .thenReturn(Map.of("what", "login: old → new"));
                web.when(() -> Web.redirectOk(eq(req), eq(resp), anyString(), anyString())).then(inv -> null);
                // when
                s.doPost(req, resp);
                // then
                web.verify(() -> Web.renewSessionAndPut(eq(req), eq(WebConst.Attr.USER), eq(userUpdated)));
                verify(session, never()).setAttribute(eq(WebConst.Attr.USER), any());
                verify(notify, atLeastOnce()).notify(any());
                web.verify(() -> Web.redirectOk(eq(req), eq(resp),
                        eq(WebConst.Path.USER_EDIT + "?id=u1"),
                        eq("The user has been updated")));
            }
        }

        @Test
        @DisplayName("given updating myself and sensitiveChanged=false & no visible changes when doPost then setAttribute only, no notify, redirectOk")
        void updateSelf_noSensitive_noVisible() throws Exception {
            // given
            AdminUserEditServlet s = newServletWithDeps();
            when(users.adminUpdate("u1", Role.ADMIN, "New Name", "newlogin", null)).thenReturn(userUpdated);
            when(req.getSession(false)).thenReturn(session);
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(me);
            when(me.getUserId()).thenReturn("u1");
            when(userUpdated.getUserId()).thenReturn("u1");
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.sensitiveChanged(userBefore, userUpdated, false)).thenReturn(false);
                web.when(() -> Web.buildAdminChangeSummary(userBefore, userUpdated, false))
                        .thenReturn("No visible changes");
                web.when(() -> Web.buildMachineReadableDiff(userBefore, userUpdated, false))
                        .thenReturn(Map.of("what", "No visible changes"));
                web.when(() -> Web.redirectOk(eq(req), eq(resp), anyString(), anyString())).then(inv -> null);
                // when
                s.doPost(req, resp);
                // then
                verify(session).setAttribute(WebConst.Attr.USER, userUpdated);
                verify(notify, never()).notify(any());
                web.verify(() -> Web.renewSessionAndPut(any(), anyString(), any()), never());
                web.verify(() -> Web.redirectOk(eq(req), eq(resp),
                        eq(WebConst.Path.USER_EDIT + "?id=u1"),
                        eq("The user has been updated")));
            }
        }
    }

    @Nested
    @DisplayName("doPost errors")
    class DoPostErrors {

        @BeforeEach
        void formInput() {
            when(req.getParameter(WebConst.Param.ID)).thenReturn("u1");
            when(req.getParameter(WebConst.Param.USER_NAME)).thenReturn("N");
            when(req.getParameter(WebConst.Param.USER_LOGIN)).thenReturn("l");
            when(req.getParameter(WebConst.Param.ROLE)).thenReturn("USER");
            when(req.getParameter(WebConst.Param.PASSWORD)).thenReturn(null);
            when(users.findById("u1")).thenReturn(Optional.of(userBefore));
        }

        @Test
        @DisplayName("given DuplicateLoginException when doPost then redirectErr with fixed message")
        void duplicateLogin() throws Exception {
            // given
            AdminUserEditServlet s = newServletWithDeps();
            when(users.adminUpdate("u1", Role.USER, "N", "l", null))
                    .thenThrow(new DuplicateLoginException("dup"));
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.redirectErr(eq(req), eq(resp), anyString(), anyString())).then(inv -> null);
                // when
                s.doPost(req, resp);
                // then
                web.verify(() -> Web.redirectErr(eq(req), eq(resp),
                        eq(WebConst.Path.USER_EDIT + "?id=u1"),
                        eq("The username is already occupied")));
            }
        }

        @Test
        @DisplayName("given IllegalArgumentException when doPost then redirectErr with ex message")
        void illegalArgument() throws Exception {
            // given
            AdminUserEditServlet s = newServletWithDeps();
            when(users.adminUpdate("u1", Role.USER, "N", "l", null))
                    .thenThrow(new IllegalArgumentException("bad args"));
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.redirectErr(eq(req), eq(resp), anyString(), anyString())).then(inv -> null);
                // when
                s.doPost(req, resp);
                // then
                web.verify(() -> Web.redirectErr(eq(req), eq(resp),
                        eq(WebConst.Path.USER_EDIT + "?id=u1"),
                        eq("bad args")));
            }
        }

        @Test
        @DisplayName("given NoSuchElementException when doPost then redirectErr with ex message")
        void noSuchElement() throws Exception {
            // given
            AdminUserEditServlet s = newServletWithDeps();
            when(users.adminUpdate("u1", Role.USER, "N", "l", null))
                    .thenThrow(new NoSuchElementException("not found"));
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.redirectErr(eq(req), eq(resp), anyString(), anyString())).then(inv -> null);
                // when
                s.doPost(req, resp);
                // then
                web.verify(() -> Web.redirectErr(eq(req), eq(resp),
                        eq(WebConst.Path.USER_EDIT + "?id=u1"),
                        eq("not found")));
            }
        }
    }
}