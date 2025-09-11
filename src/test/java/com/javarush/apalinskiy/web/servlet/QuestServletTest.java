package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.app.WebConst;
import com.javarush.apalinskiy.domain.quest.QuestNode;
import com.javarush.apalinskiy.domain.quest.choice.ChooseResult;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.service.quest.QuestAuthoringService;
import com.javarush.apalinskiy.service.quest.QuestService;
import com.javarush.apalinskiy.service.user.UserStatsService;
import com.javarush.apalinskiy.web.util.Web;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
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
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuestServlet")
class QuestServletTest {

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
    QuestService prodService;
    @Mock
    QuestAuthoringService authoring;
    @Mock
    UserStatsService userStats;
    @Mock
    User user;
    @Mock
    QuestNode node;
    @Mock
    ChooseResult chooseResult;

    QuestServlet subject;

    private void ensureSubject() {
        if (subject == null) subject = new QuestServlet();
    }

    private <T> T getField(String name, Class<T> type) {
        try {
            Field f = QuestServlet.class.getDeclaredField(name);
            f.setAccessible(true);
            return type.cast(f.get(subject));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private void setField(String name, Object value) {
        try {
            if (subject == null) subject = new QuestServlet();
            Field f = QuestServlet.class.getDeclaredField(name);
            f.setAccessible(true);
            f.set(subject, value);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private void withSession() {
        when(req.getSession()).thenReturn(session);
    }

    @Nested
    @DisplayName("init")
    class Init {

        @BeforeEach
        void beforeEach() {
            ensureSubject();
        }

        @Test
        @DisplayName("given QUEST_SERVICE & USER_STATS beans and authoring attr when init then fields are set")
        void should_Init_All_When_Present() throws Exception {
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // given
                when(config.getServletContext()).thenReturn(ctx);
                when(ctx.getAttribute(WebConst.Ctx.AUTHORING_SERVICE)).thenReturn(authoring);
                web.when(() -> Web.ctxBean(eq(ctx), eq(WebConst.Ctx.QUEST_SERVICE), eq(QuestService.class)))
                        .thenReturn(prodService);
                web.when(() -> Web.ctxBean(eq(ctx), eq(WebConst.Ctx.USER_STATS_SERVICE), eq(UserStatsService.class)))
                        .thenReturn(userStats);
                // when
                subject.init(config);
                // then
                assertSame(prodService, getField("prodService", QuestService.class));
                assertSame(authoring, getField("authoring", QuestAuthoringService.class));
                assertSame(userStats, getField("userStats", UserStatsService.class));
            }
        }

        @Test
        @DisplayName("given QUEST_SERVICE missing when init then UnavailableException with message")
        void should_Throw_When_QuestServiceMissing() {
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // given
                when(config.getServletContext()).thenReturn(ctx);
                web.when(() -> Web.ctxBean(eq(ctx), eq(WebConst.Ctx.QUEST_SERVICE), eq(QuestService.class)))
                        .thenThrow(new IllegalStateException("no bean"));
                // when / then
                UnavailableException ex = assertThrows(UnavailableException.class, () -> subject.init(config));
                assertEquals("QuestService is not initialized", ex.getMessage());
            }
        }

        @Test
        @DisplayName("given USER_STATS missing when init then userStats stays null (no exception)")
        void should_Init_With_UserStatsNull_When_Missing() throws Exception {
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // given
                when(config.getServletContext()).thenReturn(ctx);
                when(ctx.getAttribute(WebConst.Ctx.AUTHORING_SERVICE)).thenReturn(null);
                web.when(() -> Web.ctxBean(eq(ctx), eq(WebConst.Ctx.QUEST_SERVICE), eq(QuestService.class)))
                        .thenReturn(prodService);
                web.when(() -> Web.ctxBean(eq(ctx), eq(WebConst.Ctx.USER_STATS_SERVICE), eq(UserStatsService.class)))
                        .thenThrow(new IllegalStateException("no stats"));
                // when
                subject.init(config);
                // then
                assertSame(prodService, getField("prodService", QuestService.class));
                assertNull(getField("authoring", QuestAuthoringService.class));
                assertNull(getField("userStats", UserStatsService.class));
            }
        }
    }

    @Nested
    @DisplayName("doGet")
    class DoGet {

        @BeforeEach
        void wire() {
            ensureSubject();
            setField("prodService", prodService);
            setField("authoring", authoring);
            setField("userStats", userStats);
        }

        @Test
        @DisplayName("edge: missing customId -> 404")
        void should_404_When_MissingCustom() throws Exception {
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // given
                web.when(() -> Web.normalizedCustomParam(req)).thenReturn("c-x");
                web.when(() -> Web.isMissingCustomId(eq("c-x"), eq(authoring))).thenReturn(true);
                // when
                subject.doGet(req, resp);
                // then
                verify(resp).sendError(HttpServletResponse.SC_NOT_FOUND,
                        "Custom quest not found or was deleted");
            }
        }

        @Test
        @DisplayName("given main quest & id=null when doGet then forward to start")
        void should_ForwardStart_When_MainAndNoId() throws Exception {
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // given
                web.when(() -> Web.normalizedCustomParam(req)).thenReturn(null);
                web.when(() -> Web.isMissingCustomId(eq(null), eq(authoring))).thenReturn(false);
                web.when(() -> Web.firstIntParam(eq(req), eq(WebConst.Param.ID), eq(WebConst.Param.NODE)))
                        .thenReturn(null);
                when(prodService.getStart()).thenReturn(node);
                when(prodService.version()).thenReturn("v1");
                web.when(() -> Web.pullFlash(req, WebConst.Attr.FLASH)).thenAnswer(inv -> null);
                web.when(() -> Web.displayName(null, authoring)).thenReturn("Main Quest");
                // when
                subject.doGet(req, resp);
                // then
                verify(req).setAttribute(WebConst.Attr.CUSTOM, null);
                verify(req).setAttribute("questTitle", "Main Quest");
                verify(req).setAttribute(WebConst.Attr.NODE, node);
                verify(req).setAttribute(WebConst.Attr.VERSION, "v1");
                web.verify(() -> Web.forward(req, resp, WebConst.Jsp.QUEST));
            }
        }

        @Test
        @DisplayName("edge: id provided but node not found -> set error and fallback to start")
        void should_FallbackToStart_When_NodeMissing() throws Exception {
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // given
                web.when(() -> Web.normalizedCustomParam(req)).thenReturn(null);
                web.when(() -> Web.isMissingCustomId(eq(null), eq(authoring))).thenReturn(false);
                web.when(() -> Web.firstIntParam(eq(req), eq(WebConst.Param.ID), eq(WebConst.Param.NODE)))
                        .thenReturn(123);
                when(prodService.getById(123)).thenReturn(null);
                when(prodService.getStart()).thenReturn(node);
                when(prodService.version()).thenReturn("v2");
                web.when(() -> Web.pullFlash(req, WebConst.Attr.FLASH)).thenAnswer(inv -> null);
                web.when(() -> Web.displayName(null, authoring)).thenReturn("Main");
                // when
                subject.doGet(req, resp);
                // then
                verify(req).setAttribute(eq(WebConst.Attr.ERROR),
                        eq(WebConst.Msg.NODE_NOT_FOUND_PREFIX + 123));
                verify(req).setAttribute(WebConst.Attr.NODE, node);
                verify(req).setAttribute(WebConst.Attr.VERSION, "v2");
                web.verify(() -> Web.forward(req, resp, WebConst.Jsp.QUEST));
            }
        }

        @Test
        @DisplayName("edge: customId present but authoring==null -> UnavailableException")
        void should_Throw_When_CustomAndNoAuthoring() {
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // given
                setField("authoring", null);
                web.when(() -> Web.normalizedCustomParam(req)).thenReturn("c1");
                web.when(() -> Web.isMissingCustomId(eq("c1"), isNull())).thenReturn(false);
                // when / then
                ServletException ex = assertThrows(ServletException.class, () -> subject.doGet(req, resp));
                assertTrue(ex.getMessage().contains("Authoring service is not available"));
            }
        }
    }

    @Nested
    @DisplayName("doPost")
    class DoPost {

        @BeforeEach
        void wire() {
            ensureSubject();
            setField("prodService", prodService);
            setField("authoring", authoring);
            setField("userStats", userStats);
        }

        @Test
        @DisplayName("edge: missing customId -> 404")
        void should_404_When_MissingCustom() throws Exception {
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // given
                web.when(() -> Web.normalizedCustomParam(req)).thenReturn("cx");
                web.when(() -> Web.isMissingCustomId(eq("cx"), eq(authoring))).thenReturn(true);
                // when
                subject.doPost(req, resp);
                // then
                verify(resp).sendError(HttpServletResponse.SC_NOT_FOUND,
                        "Custom quest not found or was deleted");
            }
        }

        @Test
        @DisplayName("edge: fromId=null -> forward start with BAD_FROM_ID")
        void should_Forward_When_FromIdNull() throws Exception {
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // given
                web.when(() -> Web.normalizedCustomParam(req)).thenReturn(null);
                web.when(() -> Web.isMissingCustomId(eq(null), eq(authoring))).thenReturn(false);
                when(req.getParameter(WebConst.Param.FROM_ID)).thenReturn(null);
                when(prodService.getStart()).thenReturn(node);
                when(prodService.version()).thenReturn("v1");
                web.when(() -> Web.parseIntOrNull(null)).thenReturn(null);
                // when
                subject.doPost(req, resp);
                // then
                verify(req).setAttribute(WebConst.Attr.NODE, node);
                verify(req).setAttribute(WebConst.Attr.VERSION, "v1");
                verify(req).setAttribute(WebConst.Attr.ERROR, WebConst.Msg.BAD_FROM_ID);
                web.verify(() -> Web.forward(req, resp, WebConst.Jsp.QUEST));
            }
        }

        @Test
        @DisplayName("given choose OK, next!=null, next.fin, userStats!=null and user in session -> onQuestCompleted + redirect")
        void should_CompleteAndRedirect_When_Finished() throws Exception {
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // given
                withSession();
                web.when(() -> Web.normalizedCustomParam(req)).thenReturn(null); // main quest
                web.when(() -> Web.isMissingCustomId(eq(null), eq(authoring))).thenReturn(false);
                when(req.getParameter(WebConst.Param.FROM_ID)).thenReturn("10");
                web.when(() -> Web.parseIntOrNull("10")).thenReturn(10);
                when(prodService.choose(10, null)).thenReturn(chooseResult);
                when(chooseResult.isOk()).thenReturn(true);
                QuestNode next = mock(QuestNode.class);
                when(chooseResult.getNext()).thenReturn(next);
                when(next.isFin()).thenReturn(true);
                when(next.getId()).thenReturn(777);
                when(session.getAttribute(WebConst.Attr.USER)).thenReturn(user);
                when(user.getUserId()).thenReturn("U1");
                web.when(() -> Web.questUrl(eq(req), eq(777), isNull())).thenReturn("/quest?id=777");
                when(resp.encodeRedirectURL("/quest?id=777")).thenReturn("/quest?id=777");
                // when
                subject.doPost(req, resp);
                // then
                verify(userStats).onQuestCompleted("U1", "main", 777);
                verify(resp).sendRedirect("/quest?id=777");
            }
        }

        @Test
        @DisplayName("edge: choose OK but next==null -> 500")
        void should_500_When_NextNull() throws Exception {
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // given
                web.when(() -> Web.normalizedCustomParam(req)).thenReturn(null);
                web.when(() -> Web.isMissingCustomId(eq(null), eq(authoring))).thenReturn(false);
                when(req.getParameter(WebConst.Param.FROM_ID)).thenReturn("5");
                web.when(() -> Web.parseIntOrNull("5")).thenReturn(5);
                when(prodService.choose(5, null)).thenReturn(chooseResult);
                when(chooseResult.isOk()).thenReturn(true);
                when(chooseResult.getNext()).thenReturn(null);
                // when
                subject.doPost(req, resp);
                // then
                verify(resp).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Next node is null");
                verifyNoInteractions(userStats);
            }
        }

        @Test
        @DisplayName("given choose ERROR -> forward current (or start) with message")
        void should_Forward_ErrorMessage() throws Exception {
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // given
                web.when(() -> Web.normalizedCustomParam(req)).thenReturn(null);
                web.when(() -> Web.isMissingCustomId(eq(null), eq(authoring))).thenReturn(false);
                when(req.getParameter(WebConst.Param.FROM_ID)).thenReturn("12");
                web.when(() -> Web.parseIntOrNull("12")).thenReturn(12);
                when(prodService.choose(12, null)).thenReturn(chooseResult);
                when(chooseResult.isOk()).thenReturn(false);
                when(chooseResult.getMessage()).thenReturn("Bad choice");
                when(prodService.getById(12)).thenReturn(null);     // fallback
                when(prodService.getStart()).thenReturn(node);
                when(prodService.version()).thenReturn("v9");
                // when
                subject.doPost(req, resp);
                // then
                verify(req).setAttribute(WebConst.Attr.CUSTOM, null);
                verify(req).setAttribute(WebConst.Attr.NODE, node);
                verify(req).setAttribute(WebConst.Attr.VERSION, "v9");
                verify(req).setAttribute(WebConst.Attr.ERROR, "Bad choice");
                web.verify(() -> Web.forward(req, resp, WebConst.Jsp.QUEST));
            }
        }

        @Test
        @DisplayName("given choose OK, next.fin, stats!=null, but user==null -> no stats, just redirect")
        void should_Redirect_NoStats_When_Anonymous() throws Exception {
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // given
                withSession();
                web.when(() -> Web.normalizedCustomParam(req)).thenReturn(null);
                web.when(() -> Web.isMissingCustomId(eq(null), eq(authoring))).thenReturn(false);
                when(req.getParameter(WebConst.Param.FROM_ID)).thenReturn("1");
                web.when(() -> Web.parseIntOrNull("1")).thenReturn(1);
                when(prodService.choose(1, null)).thenReturn(chooseResult);
                when(chooseResult.isOk()).thenReturn(true);
                QuestNode next = mock(QuestNode.class);
                when(chooseResult.getNext()).thenReturn(next);
                when(next.isFin()).thenReturn(true);
                when(next.getId()).thenReturn(2);
                when(session.getAttribute(WebConst.Attr.USER)).thenReturn(null);
                web.when(() -> Web.questUrl(eq(req), eq(2), isNull())).thenReturn("/q?id=2");
                when(resp.encodeRedirectURL("/q?id=2")).thenReturn("/q?id=2");
                // when
                subject.doPost(req, resp);
                // then
                verifyNoInteractions(userStats);
                verify(resp).sendRedirect("/q?id=2");
            }
        }

        @Test
        @DisplayName("edge: customId present but authoring==null -> UnavailableException")
        void should_Throw_When_CustomAndNoAuthoring() {
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // given
                setField("authoring", null);
                web.when(() -> Web.normalizedCustomParam(req)).thenReturn("custom-1");
                web.when(() -> Web.isMissingCustomId(eq("custom-1"), isNull())).thenReturn(false);
                // when / then
                assertThrows(ServletException.class, () -> subject.doPost(req, resp));
            }
        }
    }
}