package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.app.WebConst;
import com.javarush.apalinskiy.domain.quest.custom.CustomQuest;
import com.javarush.apalinskiy.repository.inmemory.quest.InMemoryQuestStore;
import com.javarush.apalinskiy.service.quest.QuestAuthoringService;
import com.javarush.apalinskiy.web.util.Web;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
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
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GraphSvgServlet")
class GraphSvgServletTest {

    @Mock
    ServletConfig config;
    @Mock
    ServletContext ctx;
    @Mock
    InMemoryQuestStore repo;
    @Mock
    QuestAuthoringService authoring;
    @Mock
    CustomQuest customQuest;
    @Mock
    HttpServletRequest req;
    @Mock
    HttpServletResponse resp;
    @Mock
    HttpSession session;

    private static Object getField(Object target) {
        try {
            Field f = target.getClass().getDeclaredField("repo");
            f.setAccessible(true);
            return f.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("init()")
    class Init {

        @Test
        @DisplayName("given editor repo in context when init then stores repo")
        void ok() throws Exception {
            // given
            when(config.getServletContext()).thenReturn(ctx);
            when(ctx.getAttribute(WebConst.Ctx.EDITOR_REPOSITORY)).thenReturn(repo);
            GraphSvgServlet s = new GraphSvgServlet();
            // when
            s.init(config);
            // then
            Object actual = getField(s);
            assertSame(repo, actual);
        }

        @Test
        @DisplayName("given missing/invalid repo when init then throws ServletException")
        void missing() {
            // given
            when(config.getServletContext()).thenReturn(ctx);
            when(ctx.getAttribute(WebConst.Ctx.EDITOR_REPOSITORY)).thenReturn("not-a-repo");
            GraphSvgServlet s = new GraphSvgServlet();
            // when / then
            ServletException ex = assertThrows(ServletException.class, () -> s.init(config));
            assertTrue(ex.getMessage().contains(WebConst.Ctx.EDITOR_REPOSITORY));
        }
    }

    @Nested
    @DisplayName("doGet()")
    class DoGet {

        @Test
        @DisplayName("given load param and authoring when doGet then loads to editor, sets session/name, builds & forwards")
        void load_ok() throws Exception {
            // given
            when(config.getServletContext()).thenReturn(ctx);
            when(ctx.getAttribute(WebConst.Ctx.EDITOR_REPOSITORY)).thenReturn(repo);
            when(ctx.getAttribute(WebConst.Ctx.AUTHORING_SERVICE)).thenReturn(authoring);
            GraphSvgServlet s = new GraphSvgServlet();
            s.init(config);
            when(req.getSession(true)).thenReturn(session);
            when(req.getParameter(WebConst.Param.LOAD)).thenReturn("Q1");
            when(authoring.getFromCatalog("Q1")).thenReturn(Optional.of(customQuest));
            when(customQuest.getName()).thenReturn("Quest Name");
            when(repo.nodes()).thenReturn(List.of());
            when(repo.startId()).thenReturn(1);
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.copyParamsToAttrs(any(), any(), any())).then(inv -> null);
                web.when(() -> Web.buildQuestSvgModel(eq(req), anyList(), eq(1), eq(true))).then(inv -> null);
                web.when(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.GRAPH_SVG))).then(inv -> null);
                // when
                s.doGet(req, resp);
                // then
                verify(authoring).loadToEditor("Q1");
                verify(session).setAttribute(WebConst.Attr.EDITING_QUEST_ID, "Q1");
                verify(session).setAttribute(WebConst.Attr.EDITING_QUEST_NAME, "Quest Name");
                verify(req).setAttribute(WebConst.Attr.OK, "The quest is uploaded to the editor");
                web.verify(() -> Web.buildQuestSvgModel(eq(req), eq(List.of()), eq(1), eq(true)));
                web.verify(() -> Web.forward(req, resp, WebConst.Jsp.GRAPH_SVG));
            }
        }

        @Test
        @DisplayName("given load throws when doGet then sets error but still builds & forwards")
        void load_error_but_still_forwards() throws Exception {
            // given
            when(config.getServletContext()).thenReturn(ctx);
            when(ctx.getAttribute(WebConst.Ctx.EDITOR_REPOSITORY)).thenReturn(repo);
            when(ctx.getAttribute(WebConst.Ctx.AUTHORING_SERVICE)).thenReturn(authoring);
            GraphSvgServlet s = new GraphSvgServlet();
            s.init(config);
            when(req.getSession(true)).thenReturn(session);
            when(req.getParameter(WebConst.Param.LOAD)).thenReturn("BAD");
            doThrow(new IllegalStateException("boom")).when(authoring).loadToEditor("BAD");
            when(repo.nodes()).thenReturn(List.of());
            when(repo.startId()).thenReturn(2);
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.copyParamsToAttrs(any(), any(), any())).then(inv -> null);
                web.when(() -> Web.buildQuestSvgModel(eq(req), anyList(), eq(2), eq(true))).then(inv -> null);
                web.when(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.GRAPH_SVG))).then(inv -> null);
                // when
                s.doGet(req, resp);
                // then
                verify(req).setAttribute(eq(WebConst.Attr.ERROR), contains("Couldn't upload the quest: boom"));
                web.verify(() -> Web.forward(req, resp, WebConst.Jsp.GRAPH_SVG));
            }
        }

        @Test
        @DisplayName("given no load but authoring present & name missing when doGet then fills editing name")
        void fill_missing_editing_name() throws Exception {
            // given
            when(config.getServletContext()).thenReturn(ctx);
            when(ctx.getAttribute(WebConst.Ctx.EDITOR_REPOSITORY)).thenReturn(repo);
            when(ctx.getAttribute(WebConst.Ctx.AUTHORING_SERVICE)).thenReturn(authoring);
            GraphSvgServlet s = new GraphSvgServlet();
            s.init(config);
            when(req.getSession(true)).thenReturn(session);
            when(req.getParameter(WebConst.Param.LOAD)).thenReturn(null);
            when(session.getAttribute(WebConst.Attr.EDITING_QUEST_ID)).thenReturn("QX");
            when(session.getAttribute(WebConst.Attr.EDITING_QUEST_NAME)).thenReturn("");
            when(authoring.getFromCatalog("QX")).thenReturn(Optional.of(customQuest));
            when(customQuest.getName()).thenReturn("From Catalog");
            when(repo.nodes()).thenReturn(List.of());
            when(repo.startId()).thenReturn(3);
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.copyParamsToAttrs(any(), any(), any())).then(inv -> null);
                web.when(() -> Web.buildQuestSvgModel(eq(req), anyList(), eq(3), eq(true))).then(inv -> null);
                web.when(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.GRAPH_SVG))).then(inv -> null);
                // when
                s.doGet(req, resp);
                // then
                verify(session).setAttribute(WebConst.Attr.EDITING_QUEST_NAME, "From Catalog");
                web.verify(() -> Web.forward(req, resp, WebConst.Jsp.GRAPH_SVG));
            }
        }

        @Test
        @DisplayName("given repo.nodes throws when doGet then builds with empty list and forwards")
        void safeNodes_fallback_empty() throws Exception {
            // given
            when(config.getServletContext()).thenReturn(ctx);
            when(ctx.getAttribute(WebConst.Ctx.EDITOR_REPOSITORY)).thenReturn(repo);
            when(ctx.getAttribute(WebConst.Ctx.AUTHORING_SERVICE)).thenReturn(null);
            GraphSvgServlet s = new GraphSvgServlet();
            s.init(config);
            when(req.getSession(true)).thenReturn(session);
            when(req.getParameter(WebConst.Param.LOAD)).thenReturn(null);
            when(repo.startId()).thenReturn(10);
            doThrow(new RuntimeException("fail")).when(repo).nodes();
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                @SuppressWarnings("rawtypes") ArgumentCaptor<List> cap = ArgumentCaptor.forClass(List.class);
                web.when(() -> Web.copyParamsToAttrs(any(), any(), any())).then(inv -> null);
                //noinspection unchecked
                web.when(() -> Web.buildQuestSvgModel(eq(req), cap.capture(), eq(10), eq(true))).then(inv -> null);
                web.when(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.GRAPH_SVG))).then(inv -> null);
                // when
                s.doGet(req, resp);
                // then
                assertTrue(cap.getValue().isEmpty());
                web.verify(() -> Web.forward(req, resp, WebConst.Jsp.GRAPH_SVG));
            }
        }

        @Test
        @DisplayName("given no authoring and no load when doGet then just builds & forwards")
        void noAuthoring_simpleFlow() throws Exception {
            // given
            when(config.getServletContext()).thenReturn(ctx);
            when(ctx.getAttribute(WebConst.Ctx.EDITOR_REPOSITORY)).thenReturn(repo);
            when(ctx.getAttribute(WebConst.Ctx.AUTHORING_SERVICE)).thenReturn(null);
            GraphSvgServlet s = new GraphSvgServlet();
            s.init(config);
            when(req.getSession(true)).thenReturn(session);
            when(req.getParameter(WebConst.Param.LOAD)).thenReturn(null);
            when(repo.nodes()).thenReturn(List.of());
            when(repo.startId()).thenReturn(5);
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.copyParamsToAttrs(any(), any(), any())).then(inv -> null);
                web.when(() -> Web.buildQuestSvgModel(eq(req), anyList(), eq(5), eq(true))).then(inv -> null);
                web.when(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.GRAPH_SVG))).then(inv -> null);
                // when
                s.doGet(req, resp);
                // then
                web.verify(() -> Web.buildQuestSvgModel(eq(req), eq(List.of()), eq(5), eq(true)));
                web.verify(() -> Web.forward(req, resp, WebConst.Jsp.GRAPH_SVG));
            }
        }
    }
}