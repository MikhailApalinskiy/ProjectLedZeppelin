package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.app.WebConst;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.service.user.UserService;
import com.javarush.apalinskiy.web.util.Web;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsersServlet")
class UsersServletTest {

    @Mock
    ServletConfig config;
    @Mock
    HttpServletRequest req;
    @Mock
    HttpServletResponse resp;
    @Mock
    UserService userService;
    @Mock
    User u1;
    @Mock
    User u2;

    private UsersServlet subject;

    private void ensureSubject() {
        if (subject == null) subject = new UsersServlet();
    }

    private void setField(Object value) {
        try {
            Field f = UsersServlet.class.getDeclaredField("userService");
            f.setAccessible(true);
            f.set(subject, value);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Nested
    @DisplayName("init")
    class InitBlock {

        @BeforeEach
        void setUp() {
            ensureSubject();
        }

        @Test
        @DisplayName("given USER_SERVICE bean in ctx when init then field set")
        void should_Init_When_UserServicePresent() throws Exception {
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // given
                ServletContext sc = mock(ServletContext.class);
                when(config.getServletContext()).thenReturn(sc);
                web.when(() -> Web.ctxBean(eq(sc), eq(WebConst.Ctx.USER_SERVICE), eq(UserService.class)))
                        .thenReturn(userService);
                // when
                subject.init(config);
                // then
                web.verify(() -> Web.ctxBean(sc, WebConst.Ctx.USER_SERVICE, UserService.class));
            }
        }

        @Test
        @DisplayName("edge: USER_SERVICE missing -> IllegalStateException bubbles")
        void should_Throw_When_UserServiceMissing() {
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // given
                ServletContext sc = mock(ServletContext.class);
                when(config.getServletContext()).thenReturn(sc);
                web.when(() -> Web.ctxBean(eq(sc), eq(WebConst.Ctx.USER_SERVICE), eq(UserService.class)))
                        .thenThrow(new IllegalStateException("no bean"));
                // when / then
                assertThrows(IllegalStateException.class, () -> subject.init(config));
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Nested
    @DisplayName("doGet")
    class DoGetBlock {

        @BeforeEach
        void wire() {
            ensureSubject();
            setField(userService);
        }

        @Test
        @DisplayName("edge: q==null -> findAll(), set users, forward USERS.jsp")
        void should_ListAll_When_QueryNull() throws Exception {
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // given
                when(req.getParameter("q")).thenReturn(null);
                web.when(() -> Web.trimOrNull(null)).thenReturn(null);
                when(userService.findAll()).thenReturn(List.of(u1, u2));
                web.when(() -> Web.copyParamsToAttrs(eq(req), eq(WebConst.Attr.OK), eq(WebConst.Attr.ERROR))).thenAnswer(inv -> null);
                web.when(() -> Web.pullFlash(eq(req), eq(WebConst.Attr.OK))).thenAnswer(inv -> null);
                web.when(() -> Web.pullFlash(eq(req), eq(WebConst.Attr.ERROR))).thenAnswer(inv -> null);
                web.when(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.USERS))).thenAnswer(inv -> null);
                // when
                subject.doGet(req, resp);
                // then
                verify(userService).findAll();
                verify(req).setAttribute("users", List.of(u1, u2));
                web.verify(() -> Web.forward(req, resp, WebConst.Jsp.USERS));
            }
        }

        @Test
        @DisplayName("given q!=null and user found by id -> single result, forward")
        void should_FindById_When_Present() throws Exception {
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // given
                when(req.getParameter("q")).thenReturn("U1");
                web.when(() -> Web.trimOrNull("U1")).thenReturn("U1");
                when(userService.findById("U1")).thenReturn(Optional.of(u1));
                web.when(() -> Web.copyParamsToAttrs(eq(req), eq(WebConst.Attr.OK), eq(WebConst.Attr.ERROR))).thenAnswer(inv -> null);
                web.when(() -> Web.pullFlash(eq(req), anyString())).thenAnswer(inv -> null);
                web.when(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.USERS))).thenAnswer(inv -> null);
                // when
                subject.doGet(req, resp);
                // then
                verify(userService).findById("U1");
                verify(req).setAttribute("users", List.of(u1));
                web.verify(() -> Web.forward(req, resp, WebConst.Jsp.USERS));
                verifyNoMoreInteractions(userService);
            }
        }

        @Test
        @DisplayName("given q!=null, not found by id but found by login -> single result, forward")
        void should_FindByLogin_When_IdMiss() throws Exception {
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // given
                when(req.getParameter("q")).thenReturn("NickName");
                web.when(() -> Web.trimOrNull("NickName")).thenReturn("NickName");
                when(userService.findById("NickName")).thenReturn(Optional.empty());
                when(userService.findByLogin("nickname")).thenReturn(Optional.of(u2));
                web.when(() -> Web.copyParamsToAttrs(eq(req), eq(WebConst.Attr.OK), eq(WebConst.Attr.ERROR))).thenAnswer(inv -> null);
                web.when(() -> Web.pullFlash(eq(req), anyString())).thenAnswer(inv -> null);
                web.when(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.USERS))).thenAnswer(inv -> null);
                // when
                subject.doGet(req, resp);
                // then
                verify(userService).findById("NickName");
                verify(userService).findByLogin("nickname");
                verify(req).setAttribute("users", List.of(u2));
                web.verify(() -> Web.forward(req, resp, WebConst.Jsp.USERS));
            }
        }

        @Test
        @DisplayName("given q!=null, not found by id and by login -> empty list, forward")
        void should_Empty_When_NotFoundAnywhere() throws Exception {
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // given
                when(req.getParameter("q")).thenReturn("Something");
                web.when(() -> Web.trimOrNull("Something")).thenReturn("Something");
                when(userService.findById("Something")).thenReturn(Optional.empty());
                when(userService.findByLogin("something")).thenReturn(Optional.empty());
                web.when(() -> Web.copyParamsToAttrs(eq(req), eq(WebConst.Attr.OK), eq(WebConst.Attr.ERROR))).thenAnswer(inv -> null);
                web.when(() -> Web.pullFlash(eq(req), anyString())).thenAnswer(inv -> null);
                web.when(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.USERS))).thenAnswer(inv -> null);
                // when
                subject.doGet(req, resp);
                // then
                verify(userService).findById("Something");
                verify(userService).findByLogin("something");
                ArgumentCaptor<List<User>> cap = ArgumentCaptor.forClass(List.class);
                verify(req).setAttribute(eq("users"), cap.capture());
                assertTrue(cap.getValue().isEmpty());
                web.verify(() -> Web.forward(req, resp, WebConst.Jsp.USERS));
            }
        }
    }
}