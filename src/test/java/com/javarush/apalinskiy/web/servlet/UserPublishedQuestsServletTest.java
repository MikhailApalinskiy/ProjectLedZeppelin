package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.app.WebConst;
import com.javarush.apalinskiy.domain.quest.custom.CustomQuest;
import com.javarush.apalinskiy.domain.user.Role;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.service.quest.QuestAuthoringService;
import com.javarush.apalinskiy.service.user.UserService;
import com.javarush.apalinskiy.web.util.Web;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.UnavailableException;
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

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserPublishedQuestsServlet")
class UserPublishedQuestsServletTest {

    @Mock
    ServletConfig config;
    @Mock
    HttpServletRequest req;
    @Mock
    HttpServletResponse resp;
    @Mock
    UserService userService;
    @Mock
    QuestAuthoringService authoring;
    @Mock
    User viewUser;
    @Mock
    User me;
    @Mock
    CustomQuest q1;
    @Mock
    CustomQuest q2;

    UserPublishedQuestsServlet subject;

    private void ensureSubject() {
        if (subject == null) subject = new UserPublishedQuestsServlet();
    }

    private void setField(String name, Object value) {
        try {
            Field f = UserPublishedQuestsServlet.class.getDeclaredField(name);
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
        void before() {
            ensureSubject();
        }

        @Test
        @DisplayName("given both beans in ctx when init then fields set")
        void should_Init_When_BeansPresent() throws Exception {
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                // given
                ServletContext sc = mock(ServletContext.class);
                when(config.getServletContext()).thenReturn(sc);
                web.when(() -> Web.ctxBean(eq(sc), eq(WebConst.Ctx.USER_SERVICE), eq(UserService.class)))
                        .thenReturn(userService);
                web.when(() -> Web.ctxBean(eq(sc), eq(WebConst.Ctx.AUTHORING_SERVICE), eq(QuestAuthoringService.class)))
                        .thenReturn(authoring);
                // when
                subject.init(config);
                // then
                web.verify(() -> Web.ctxBean(sc, WebConst.Ctx.USER_SERVICE, UserService.class));
                web.verify(() -> Web.ctxBean(sc, WebConst.Ctx.AUTHORING_SERVICE, QuestAuthoringService.class));
            }
        }

        @Test
        @DisplayName("edge: one of beans missing -> UnavailableException with message prefix")
        void should_Throw_When_BeansMissing() {
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // given
                ServletContext sc = mock(ServletContext.class);
                when(config.getServletContext()).thenReturn(sc);
                web.when(() -> Web.ctxBean(eq(sc), eq(WebConst.Ctx.USER_SERVICE), eq(UserService.class)))
                        .thenThrow(new IllegalStateException("no user service"));
                // when / then
                UnavailableException ex = assertThrows(UnavailableException.class, () -> subject.init(config));
                assertTrue(ex.getMessage().startsWith("Required services not found: "));
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
            setField("userService", userService);
            setField("authoring", authoring);
        }

        @Test
        @DisplayName("edge: id==null -> viewUser=null, items=[], selfUrl without query, forward")
        void should_Forward_With_Empty_When_IdNull() throws Exception {
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // given
                when(req.getParameter("id")).thenReturn(null);
                web.when(() -> Web.trimOrNull(null)).thenReturn(null);
                web.when(() -> Web.pullFlash(eq(req), anyString())).thenAnswer(inv -> null);
                when(req.getContextPath()).thenReturn("/app");
                when(req.getServletPath()).thenReturn("/u/quests");
                web.when(() -> Web.attachQuestLists(eq(req), any())).thenAnswer(inv -> null);
                web.when(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.USER_QUESTS))).thenAnswer(inv -> null);
                // when
                subject.doGet(req, resp);
                // then
                verify(req).setAttribute("viewUser", null);
                verify(req).setAttribute("selfUrl", "/app/u/quests");
                web.verify(() -> Web.attachQuestLists(eq(req), argThat(List::isEmpty)));
                web.verify(() -> Web.forward(req, resp, WebConst.Jsp.USER_QUESTS));
                verifyNoInteractions(userService, authoring);
            }
        }

        @Test
        @DisplayName("given id but user not found -> items=[], selfUrl with encoded id, forward")
        void should_Forward_Empty_When_UserNotFound() throws Exception {
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // given
                when(req.getParameter("id")).thenReturn("u1");
                web.when(() -> Web.trimOrNull("u1")).thenReturn("u1");
                when(userService.findById("u1")).thenReturn(Optional.empty());
                web.when(() -> Web.pullFlash(eq(req), anyString())).thenAnswer(inv -> null);
                when(req.getContextPath()).thenReturn("/app");
                when(req.getServletPath()).thenReturn("/u/quests");
                web.when(() -> Web.urlEncode("u1")).thenReturn("u1");
                web.when(() -> Web.attachQuestLists(eq(req), any())).thenAnswer(inv -> null);
                web.when(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.USER_QUESTS))).thenAnswer(inv -> null);
                // when
                subject.doGet(req, resp);
                // then
                verify(userService).findById("u1");
                verify(req).setAttribute("viewUser", null);
                verify(req).setAttribute("selfUrl", "/app/u/quests?id=u1");
                web.verify(() -> Web.attachQuestLists(eq(req), argThat(List::isEmpty)));
                web.verify(() -> Web.forward(req, resp, WebConst.Jsp.USER_QUESTS));
                verifyNoInteractions(authoring);
            }
        }

        @Test
        @DisplayName("given owner viewing own page -> items not filtered (all returned)")
        void should_Return_All_For_Owner() throws Exception {
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // given
                when(req.getParameter("id")).thenReturn("U");
                web.when(() -> Web.trimOrNull("U")).thenReturn("U");
                when(userService.findById("U")).thenReturn(Optional.of(viewUser));
                when(viewUser.getUserId()).thenReturn("U");
                when(viewUser.getUserLogin()).thenReturn("loginU");
                List<CustomQuest> all = List.of(q1, q2);
                when(authoring.listOwnerFromCatalog("loginU")).thenReturn(all);
                when(req.getSession()).thenReturn(mock(jakarta.servlet.http.HttpSession.class));
                when(req.getSession().getAttribute(WebConst.Attr.USER)).thenReturn(me);
                when(me.getUserId()).thenReturn("U");
                web.when(() -> Web.pullFlash(eq(req), anyString())).thenAnswer(inv -> null);
                when(req.getContextPath()).thenReturn("/app");
                when(req.getServletPath()).thenReturn("/u/quests");
                web.when(() -> Web.urlEncode("U")).thenReturn("U");
                ArgumentCaptor<List<CustomQuest>> cap = ArgumentCaptor.forClass(List.class);
                web.when(() -> Web.attachQuestLists(eq(req), any())).thenAnswer(inv -> null);
                web.when(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.USER_QUESTS))).thenAnswer(inv -> null);
                // when
                subject.doGet(req, resp);
                // then
                verify(authoring).listOwnerFromCatalog("loginU");
                verify(req).setAttribute("viewUser", viewUser);
                verify(req).setAttribute("selfUrl", "/app/u/quests?id=U");
                web.verify(() -> Web.attachQuestLists(eq(req), cap.capture()));
                assertEquals(2, cap.getValue().size(), "для владельца список не фильтруется");
                web.verify(() -> Web.forward(req, resp, WebConst.Jsp.USER_QUESTS));
            }
        }

        @Test
        @DisplayName("given admin viewing -> items not filtered")
        void should_Return_All_For_Admin() throws Exception {
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // given
                when(req.getParameter("id")).thenReturn("X");
                web.when(() -> Web.trimOrNull("X")).thenReturn("X");
                web.when(() -> Web.pullFlash(eq(req), anyString())).thenAnswer(inv -> null);
                when(userService.findById("X")).thenReturn(Optional.of(viewUser));
                when(viewUser.getUserLogin()).thenReturn("loginX");
                when(authoring.listOwnerFromCatalog("loginX")).thenReturn(List.of(q1, q2));
                HttpSession session = mock(HttpSession.class);
                when(req.getSession()).thenReturn(session);
                when(session.getAttribute(WebConst.Attr.USER)).thenReturn(me);
                when(me.getRole()).thenReturn(Role.ADMIN);
                web.when(() -> Web.attachQuestLists(eq(req), any())).thenAnswer(inv -> null);
                ArgumentCaptor<List<CustomQuest>> cap = ArgumentCaptor.forClass(List.class);
                // when
                subject.doGet(req, resp);
                // then
                web.verify(() -> Web.attachQuestLists(eq(req), cap.capture()));
                assertEquals(2, cap.getValue().size());
            }
        }

        @Test
        @DisplayName("given viewer not owner and not admin -> items filtered to published only")
        void should_Filter_To_Published_For_Visitor() throws Exception {
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // given
                when(req.getParameter("id")).thenReturn("A");
                web.when(() -> Web.trimOrNull("A")).thenReturn("A");
                when(userService.findById("A")).thenReturn(Optional.of(viewUser));
                when(viewUser.getUserId()).thenReturn("A");
                when(viewUser.getUserLogin()).thenReturn("loginA");
                when(q1.isPublished()).thenReturn(true);
                when(q2.isPublished()).thenReturn(false);
                when(authoring.listOwnerFromCatalog("loginA")).thenReturn(List.of(q1, q2));
                var session = mock(jakarta.servlet.http.HttpSession.class);
                when(req.getSession()).thenReturn(session);
                when(session.getAttribute(WebConst.Attr.USER)).thenReturn(me);
                when(me.getUserId()).thenReturn("other");
                when(me.getRole()).thenReturn(Role.USER);
                web.when(() -> Web.pullFlash(eq(req), anyString())).thenAnswer(inv -> null);
                when(req.getContextPath()).thenReturn("/app");
                when(req.getServletPath()).thenReturn("/u/quests");
                web.when(() -> Web.urlEncode("A")).thenReturn("A");
                ArgumentCaptor<List<CustomQuest>> cap = ArgumentCaptor.forClass(List.class);
                web.when(() -> Web.attachQuestLists(eq(req), any())).thenAnswer(inv -> null);
                web.when(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.USER_QUESTS))).thenAnswer(inv -> null);
                // when
                subject.doGet(req, resp);
                // then
                web.verify(() -> Web.attachQuestLists(eq(req), cap.capture()));
                List<CustomQuest> filtered = cap.getValue();
                assertEquals(1, filtered.size(), "для гостя должен остаться только опубликованный квест");
                assertSame(q1, filtered.getFirst());
            }
        }
    }
}