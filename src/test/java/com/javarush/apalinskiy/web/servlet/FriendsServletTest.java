package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.app.WebConst;
import com.javarush.apalinskiy.domain.notify.NotificationEvent;
import com.javarush.apalinskiy.domain.notify.NotificationType;
import com.javarush.apalinskiy.domain.social.FriendRequest;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.service.impl.user.DefaultUserService;
import com.javarush.apalinskiy.service.notify.NotificationService;
import com.javarush.apalinskiy.service.social.FriendService;
import com.javarush.apalinskiy.web.util.Web;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("FriendsServlet (unit)")
@ExtendWith(MockitoExtension.class)
class FriendsServletTest {

    @Mock
    ServletConfig config;
    @Mock
    ServletContext ctx;
    @Mock
    FriendService friendService;
    @Mock
    NotificationService notify;
    @Mock
    HttpServletRequest req;
    @Mock
    HttpServletResponse resp;
    @Mock
    HttpSession session;

    private FriendsServlet sut;

    private User user(String id) {
        User u = new User();
        u.setUserId(id);
        return u;
    }

    private void initServletOk(boolean withNotify) throws ServletException {
        sut = new FriendsServlet();
        when(config.getServletContext()).thenReturn(ctx);
        try (MockedStatic<Web> web = mockStatic(Web.class)) {
            web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.FRIEND_SERVICE, FriendService.class))
                    .thenReturn(friendService);
            if (withNotify) {
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.NOTIFY_SERVICE, NotificationService.class))
                        .thenReturn(notify);
            } else {
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.NOTIFY_SERVICE, NotificationService.class))
                        .thenThrow(new IllegalStateException("no notify"));
            }
            sut.init(config);
        }
    }

    @Nested
    @DisplayName("init(config)")
    class InitPhase {

        @Test
        @DisplayName("Given both beans exist — When init — Then resolves FriendService and NotificationService")
        void initWithNotify() throws ServletException {
            // Given
            sut = new FriendsServlet();
            when(config.getServletContext()).thenReturn(ctx);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.FRIEND_SERVICE, FriendService.class))
                        .thenReturn(friendService);
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.NOTIFY_SERVICE, NotificationService.class))
                        .thenReturn(notify);
                // When
                sut.init(config);
                // Then
                web.verify(() -> Web.ctxBean(ctx, WebConst.Ctx.FRIEND_SERVICE, FriendService.class));
                web.verify(() -> Web.ctxBean(ctx, WebConst.Ctx.NOTIFY_SERVICE, NotificationService.class));
            }
        }

        @Test
        @DisplayName("Given notify missing — When init — Then not fail and notify==null")
        void initWithoutNotify() {
            // Given
            sut = new FriendsServlet();
            when(config.getServletContext()).thenReturn(ctx);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.FRIEND_SERVICE, FriendService.class))
                        .thenReturn(friendService);
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.NOTIFY_SERVICE, NotificationService.class))
                        .thenThrow(new IllegalStateException("no notify"));
                // When / Then
                assertDoesNotThrow(() -> sut.init(config));
            }
        }
    }

    @Nested
    @DisplayName("doGet(req, resp)")
    class DoGet {

        @BeforeEach
        void setUp() throws ServletException {
            initServletOk(true);
            lenient().when(req.getParameter(anyString())).thenReturn(null);
        }

        @Test
        @DisplayName("Given unauthenticated — When doGet — Then redirect to login and return")
        void unauthRedirects() throws Exception {
            // Given
            when(req.getSession(true)).thenReturn(session);
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(null);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.pullFlash(req, WebConst.Attr.OK)).thenAnswer(inv -> null);
                web.when(() -> Web.pullFlash(req, WebConst.Attr.ERROR)).thenAnswer(inv -> null);
                // When
                sut.doGet(req, resp);
                // Then
                web.verify(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.LOGIN), eq("Please login")));

                web.verify(() -> Web.forward(any(), any(), anyString()), times(0));
            }
        }

        @Test
        @DisplayName("Given authenticated and params — When doGet — Then list friends, set attrs, forward")
        void happyPath() throws Exception {
            // Given
            User me = user("u1");
            when(req.getSession(true)).thenReturn(session);
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(me);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.pullFlash(req, WebConst.Attr.OK)).thenAnswer(inv -> null);
                web.when(() -> Web.pullFlash(req, WebConst.Attr.ERROR)).thenAnswer(inv -> null);
                when(req.getParameter("q")).thenReturn("  Mike  ");
                when(req.getParameter("page")).thenReturn("2");
                when(req.getParameter("size")).thenReturn("15");
                web.when(() -> Web.trimOrNull("  Mike  ")).thenReturn("Mike");
                web.when(() -> Web.parseIntOrDefault("2", 1)).thenReturn(2);
                web.when(() -> Web.parseIntOrDefault("15", 10)).thenReturn(15);
                DefaultUserService.PagedResult<User> paged =
                        new DefaultUserService.PagedResult<>(List.of(user("f1"), user("f2")), 20L, 2, 15);
                when(friendService.listFriendsPaged("u1", 2, 15, "Mike")).thenReturn(paged);
                FriendRequest inReq = mock(FriendRequest.class);
                FriendRequest outReq = mock(FriendRequest.class);
                when(friendService.incoming("u1")).thenReturn(List.of(inReq));
                when(friendService.outgoing("u1")).thenReturn(List.of(outReq));
                when(req.getContextPath()).thenReturn("/app");
                // When
                sut.doGet(req, resp);
                // Then
                verify(req).setAttribute("friends", paged.items());
                verify(req).setAttribute("total", paged.total());
                verify(req).setAttribute("page", paged.page());
                verify(req).setAttribute("size", paged.size());
                verify(req).setAttribute("pages", paged.totalPages());
                verify(req).setAttribute("q", "Mike");
                verify(req).setAttribute("incoming", List.of(inReq));
                verify(req).setAttribute("outgoing", List.of(outReq));
                verify(req).setAttribute("selfUrl", "/app" + WebConst.Path.FRIENDS);
                web.verify(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.FRIENDS)));
            }
        }

        @Test
        @DisplayName("Given invalid page/size — When doGet — Then clamp page>=1 and size in [12..100]")
        void clampPageSize() throws Exception {
            // Given
            User me = user("u1");
            when(req.getSession(true)).thenReturn(session);
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(me);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.pullFlash(req, WebConst.Attr.OK)).thenAnswer(inv -> null);
                web.when(() -> Web.pullFlash(req, WebConst.Attr.ERROR)).thenAnswer(inv -> null);
                when(req.getParameter("q")).thenReturn(null);
                when(req.getParameter("page")).thenReturn("-5");
                when(req.getParameter("size")).thenReturn("0");
                web.when(() -> Web.trimOrNull(null)).thenReturn(null);
                web.when(() -> Web.parseIntOrDefault("-5", 1)).thenReturn(-5);
                web.when(() -> Web.parseIntOrDefault("0", 10)).thenReturn(0);
                DefaultUserService.PagedResult<User> paged =
                        new DefaultUserService.PagedResult<>(List.of(), 0L, 1, 12);
                when(friendService.listFriendsPaged("u1", 1, 12, null)).thenReturn(paged);
                when(friendService.incoming("u1")).thenReturn(List.of());
                when(friendService.outgoing("u1")).thenReturn(List.of());
                when(req.getContextPath()).thenReturn("");
                // When
                sut.doGet(req, resp);
                // Then
                verify(req).setAttribute("page", 1);
                verify(req).setAttribute("size", 12);
                verify(req).setAttribute("pages", 1);
                verify(req).setAttribute("selfUrl", WebConst.Path.FRIENDS);
                web.verify(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.FRIENDS)));
            }
        }

        @Test
        @DisplayName("Given size>100 — When doGet — Then clamp size=100")
        void clampMaxSize() throws Exception {
            // Given
            User me = user("u1");
            when(req.getSession(true)).thenReturn(session);
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(me);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.pullFlash(req, WebConst.Attr.OK)).thenAnswer(inv -> null);
                web.when(() -> Web.pullFlash(req, WebConst.Attr.ERROR)).thenAnswer(inv -> null);
                when(req.getParameter("page")).thenReturn("1");
                when(req.getParameter("size")).thenReturn("500");
                web.when(() -> Web.parseIntOrDefault("1", 1)).thenReturn(1);
                web.when(() -> Web.parseIntOrDefault("500", 10)).thenReturn(500);
                DefaultUserService.PagedResult<User> paged =
                        new DefaultUserService.PagedResult<>(List.of(), 0L, 1, 100);
                when(friendService.listFriendsPaged("u1", 1, 100, null)).thenReturn(paged);
                when(friendService.incoming("u1")).thenReturn(List.of());
                when(friendService.outgoing("u1")).thenReturn(List.of());
                when(req.getContextPath()).thenReturn("");
                // When
                sut.doGet(req, resp);
                // Then
                verify(req).setAttribute("size", 100);
                web.verify(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.FRIENDS)));
            }
        }
    }

    @Nested
    @DisplayName("doPost(req, resp)")
    class DoPost {

        @BeforeEach
        void setUp() throws ServletException {
            initServletOk(true);
        }

        private void authAs() {
            when(req.getSession(true)).thenReturn(session);
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(user("u1"));
        }

        @Test
        @DisplayName("Given unauthenticated — When doPost — Then redirect to login and return")
        void unauthRedirects() throws Exception {
            // Given
            when(req.getSession(true)).thenReturn(session);
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(null);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // When
                sut.doPost(req, resp);
                // Then
                web.verify(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.LOGIN), eq("Please login")));
            }
        }

        @Test
        @DisplayName("request: missing id — Then redirectErr")
        void requestMissingId() throws Exception {
            // Given
            authAs();
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("request");
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.trimOrNull("request")).thenReturn("request");
                web.when(() -> Web.trimOrNull(null)).thenReturn(null);
                // When
                sut.doPost(req, resp);
                // Then
                web.verify(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.FRIENDS), eq("The recipient is not specified")));
                verify(friendService, never()).sendRequest(anyString(), anyString());
            }
        }

        @Test
        @DisplayName("request: ok — Then service.sendRequest and redirectOk")
        void requestOk() throws Exception {
            // Given
            authAs();
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("request");
            when(req.getParameter(WebConst.Param.ID)).thenReturn("u2");
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.trimOrNull("request")).thenReturn("request");
                web.when(() -> Web.trimOrNull("u2")).thenReturn("u2");
                // When
                sut.doPost(req, resp);
                // Then
                verify(friendService).sendRequest("u1", "u2");
                web.verify(() -> Web.redirectOk(eq(req), eq(resp), eq(WebConst.Path.FRIENDS), eq("The application has been sent")));
            }
        }

        @Test
        @DisplayName("accept: missing fromId — Then redirectErr")
        void acceptMissing() throws Exception {
            // Given
            authAs();
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("accept");
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.trimOrNull("accept")).thenReturn("accept");
                web.when(() -> Web.trimOrNull(null)).thenReturn(null);
                // When
                sut.doPost(req, resp);
                // Then
                web.verify(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.FRIENDS), eq("The sender is not specified")));
                verify(friendService, never()).accept(anyString(), anyString());
            }
        }

        @Test
        @DisplayName("accept: ok — Then service.accept and redirectOk")
        void acceptOk() throws Exception {
            // Given
            authAs();
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("accept");
            when(req.getParameter(WebConst.Param.FROM_ID)).thenReturn("u2");
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.trimOrNull("accept")).thenReturn("accept");
                web.when(() -> Web.trimOrNull("u2")).thenReturn("u2");
                // When
                sut.doPost(req, resp);
                // Then
                verify(friendService).accept("u1", "u2");
                web.verify(() -> Web.redirectOk(eq(req), eq(resp), eq(WebConst.Path.FRIENDS), eq("The application has been accepted")));
            }
        }

        @Test
        @DisplayName("decline: missing fromId — Then redirectErr")
        void declineMissing() throws Exception {
            // Given
            authAs();
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("decline");
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.trimOrNull("decline")).thenReturn("decline");
                web.when(() -> Web.trimOrNull(null)).thenReturn(null);
                // When
                sut.doPost(req, resp);
                // Then
                web.verify(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.FRIENDS), eq("The sender is not specified")));
                verify(friendService, never()).decline(anyString(), anyString());
            }
        }

        @Test
        @DisplayName("decline: ok — Then service.decline and redirectOk")
        void declineOk() throws Exception {
            // Given
            authAs();
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("decline");
            when(req.getParameter(WebConst.Param.FROM_ID)).thenReturn("u2");
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.trimOrNull("decline")).thenReturn("decline");
                web.when(() -> Web.trimOrNull("u2")).thenReturn("u2");
                // When
                sut.doPost(req, resp);
                // Then
                verify(friendService).decline("u1", "u2");
                web.verify(() -> Web.redirectOk(eq(req), eq(resp), eq(WebConst.Path.FRIENDS), eq("The application was rejected")));
            }
        }

        @Test
        @DisplayName("cancel: missing id — Then redirectErr")
        void cancelMissing() throws Exception {
            // Given
            authAs();
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("cancel");
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.trimOrNull("cancel")).thenReturn("cancel");
                web.when(() -> Web.trimOrNull(null)).thenReturn(null);
                // When
                sut.doPost(req, resp);
                // Then
                web.verify(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.FRIENDS), eq("The recipient is not specified")));
                verify(friendService, never()).cancel(anyString(), anyString());
            }
        }

        @Test
        @DisplayName("cancel: ok — Then service.cancel and redirectOk")
        void cancelOk() throws Exception {
            // Given
            authAs();
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("cancel");
            when(req.getParameter(WebConst.Param.ID)).thenReturn("u2");
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.trimOrNull("cancel")).thenReturn("cancel");
                web.when(() -> Web.trimOrNull("u2")).thenReturn("u2");
                // When
                sut.doPost(req, resp);
                // Then
                verify(friendService).cancel("u1", "u2");
                web.verify(() -> Web.redirectOk(eq(req), eq(resp), eq(WebConst.Path.FRIENDS), eq("The application has been cancelled")));
            }
        }

        @Test
        @DisplayName("remove: missing id — Then redirectErr")
        void removeMissing() throws Exception {
            // Given
            authAs();
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("remove");
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.trimOrNull("remove")).thenReturn("remove");
                web.when(() -> Web.trimOrNull(null)).thenReturn(null);
                // When
                sut.doPost(req, resp);
                // Then
                web.verify(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.FRIENDS), eq("Friend not specified")));
                verify(friendService, never()).remove(anyString(), anyString());
            }
        }

        @Test
        @DisplayName("remove: ok without notify — Then service.remove and redirectOk")
        void removeOkNoNotify() throws Exception {
            // Given
            initServletOk(false);
            authAs();
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("remove");
            when(req.getParameter(WebConst.Param.ID)).thenReturn("u2");
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.trimOrNull("remove")).thenReturn("remove");
                web.when(() -> Web.trimOrNull("u2")).thenReturn("u2");
                // When
                sut.doPost(req, resp);
                // Then
                verify(friendService).remove("u1", "u2");
                verifyNoInteractions(notify);
                web.verify(() -> Web.redirectOk(eq(req), eq(resp), eq(WebConst.Path.FRIENDS), eq("The user has been removed from friends")));
            }
        }

        @Test
        @DisplayName("remove: ok with notify — Then service.remove, notify FRIEND_REMOVED, redirectOk")
        void removeOkWithNotify() throws Exception {
            // Given
            initServletOk(true);
            authAs();
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("remove");
            when(req.getParameter(WebConst.Param.ID)).thenReturn("u2");
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.trimOrNull("remove")).thenReturn("remove");
                web.when(() -> Web.trimOrNull("u2")).thenReturn("u2");
                // When
                sut.doPost(req, resp);
                // Then
                verify(friendService).remove("u1", "u2");
                ArgumentCaptor<NotificationEvent> cap = ArgumentCaptor.forClass(NotificationEvent.class);
                verify(notify).notify(cap.capture());
                NotificationEvent ev = cap.getValue();
                assertEquals(NotificationType.FRIEND_REMOVED, ev.type());
                assertEquals("u1", ev.actorUserId());
                assertEquals("u2", ev.targetUserId());
                web.verify(() -> Web.redirectOk(eq(req), eq(resp), eq(WebConst.Path.FRIENDS), eq("The user has been removed from friends")));
            }
        }

        @Test
        @DisplayName("unknown action — Then redirectErr('Unknown action')")
        void unknownAction() throws Exception {
            // Given
            authAs();
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("something-else");
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.trimOrNull("something-else")).thenReturn("something-else");
                // When
                sut.doPost(req, resp);
                // Then
                web.verify(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.FRIENDS), eq("Unknown action")));
                verifyNoInteractions(friendService);
            }
        }

        @Test
        @DisplayName("service throws — Then redirectErr with message")
        void serviceThrows() throws Exception {
            // Given
            authAs();
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("request");
            when(req.getParameter(WebConst.Param.ID)).thenReturn("u2");
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.trimOrNull("request")).thenReturn("request");
                web.when(() -> Web.trimOrNull("u2")).thenReturn("u2");
                doThrow(new IllegalStateException("boom")).when(friendService).sendRequest("u1", "u2");
                // When
                sut.doPost(req, resp);
                // Then
                web.verify(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.FRIENDS), eq("boom")));
            }
        }
    }
}