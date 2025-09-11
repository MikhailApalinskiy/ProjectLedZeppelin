package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.app.WebConst;
import com.javarush.apalinskiy.domain.quest.QuestNode;
import com.javarush.apalinskiy.repository.quest.CustomQuestRepository;
import com.javarush.apalinskiy.service.quest.QuestAuthoringService;
import com.javarush.apalinskiy.web.util.Web;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("AdminModerationGraphServlet")
@ExtendWith(MockitoExtension.class)
class AdminModerationGraphServletTest {

    @Mock
    ServletConfig config;
    @Mock
    ServletContext ctx;
    @Mock
    HttpServletRequest req;
    @Mock
    HttpServletResponse resp;
    @Mock
    QuestAuthoringService authoring;
    @Mock
    CustomQuestRepository.PendingNew pn;
    @Mock
    CustomQuestRepository.PendingEdit pe;
    @Mock
    QuestNode node;

    @Test
    @DisplayName("given ctx has authoring when init then ok")
    void init_ok() throws Exception {
        // given
        AdminModerationGraphServlet s = new AdminModerationGraphServlet();
        when(config.getServletContext()).thenReturn(ctx);
        try (MockedStatic<Web> web = mockStatic(Web.class)) {
            web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.AUTHORING_SERVICE, QuestAuthoringService.class))
                    .thenReturn(authoring);
            // when / then
            s.init(config);
        }
    }

    @Test
    @DisplayName("given no authoring in ctx when init then UnavailableException")
    void init_missing_service() {
        // given
        AdminModerationGraphServlet s = new AdminModerationGraphServlet();
        when(config.getServletContext()).thenReturn(ctx);
        try (MockedStatic<Web> web = mockStatic(Web.class)) {
            web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.AUTHORING_SERVICE, QuestAuthoringService.class))
                    .thenThrow(new IllegalStateException("no bean"));
            // when / then
            assertThrows(UnavailableException.class, () -> s.init(config));
        }
    }

    @Test
    @DisplayName("given missing 'kind' or 'id' when doGet then 400")
    void doGet_missing_params() throws Exception {
        // given
        AdminModerationGraphServlet s = new AdminModerationGraphServlet();
        when(config.getServletContext()).thenReturn(ctx);
        try (MockedStatic<Web> web = mockStatic(Web.class)) {
            web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.AUTHORING_SERVICE, QuestAuthoringService.class))
                    .thenReturn(authoring);
            web.when(() -> Web.trimOrNull(null)).thenReturn(null);
            s.init(config);
            when(req.getParameter("kind")).thenReturn(null);
            when(req.getParameter("id")).thenReturn("abc");
            // when
            s.doGet(req, resp);
            // then
            verify(resp).sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing parameters");
        }
    }

    @Test
    @DisplayName("given unknown kind when doGet then 400")
    void doGet_unknown_kind() throws Exception {
        // given
        AdminModerationGraphServlet s = new AdminModerationGraphServlet();
        when(config.getServletContext()).thenReturn(ctx);
        try (MockedStatic<Web> web = mockStatic(Web.class)) {
            web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.AUTHORING_SERVICE, QuestAuthoringService.class))
                    .thenReturn(authoring);
            web.when(() -> Web.trimOrNull("weird")).thenReturn("weird");
            web.when(() -> Web.trimOrNull("id1")).thenReturn("id1");
            s.init(config);
            when(req.getParameter("kind")).thenReturn("weird");
            when(req.getParameter("id")).thenReturn("id1");
            // when
            s.doGet(req, resp);
            // then
            verify(resp).sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown kind");
        }
    }

    @Test
    @DisplayName("given kind=new and item not found when doGet then 404")
    void doGet_new_notFound() throws Exception {
        // given
        AdminModerationGraphServlet s = new AdminModerationGraphServlet();
        when(config.getServletContext()).thenReturn(ctx);
        try (MockedStatic<Web> web = mockStatic(Web.class)) {
            web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.AUTHORING_SERVICE, QuestAuthoringService.class))
                    .thenReturn(authoring);
            web.when(() -> Web.trimOrNull("new")).thenReturn("new");
            web.when(() -> Web.trimOrNull("abc")).thenReturn("abc");
            s.init(config);
            when(authoring.listPendingNew()).thenReturn(List.of());
            when(req.getParameter("kind")).thenReturn("new");
            when(req.getParameter("id")).thenReturn("abc");
            // when
            s.doGet(req, resp);
            // then
            verify(resp).sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Test
    @DisplayName("given kind=new and item found when doGet then builds model, sets attrs, forwards")
    void doGet_new_ok() throws Exception {
        // given
        AdminModerationGraphServlet s = new AdminModerationGraphServlet();
        when(config.getServletContext()).thenReturn(ctx);
        List<QuestNode> srcNodes = Arrays.asList(null, node);
        when(pn.getPendingId()).thenReturn("abc");
        when(pn.getStartId()).thenReturn(11);
        when(pn.getNodes()).thenReturn(srcNodes);
        when(pn.getName()).thenReturn("Cool");
        try (MockedStatic<Web> web = mockStatic(Web.class)) {
            web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.AUTHORING_SERVICE, QuestAuthoringService.class))
                    .thenReturn(authoring);
            web.when(() -> Web.trimOrNull("new")).thenReturn("new");
            web.when(() -> Web.trimOrNull("abc")).thenReturn("abc");
            web.when(() -> Web.buildQuestSvgModel(eq(req), anyList(), eq(11), eq(true))).then(inv -> null);
            web.when(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.QUESTS_MOD_PREVIEW))).then(inv -> null);
            s.init(config);
            when(authoring.listPendingNew()).thenReturn(List.of(pn));
            when(req.getParameter("kind")).thenReturn("new");
            when(req.getParameter("id")).thenReturn("abc");
            when(req.getContextPath()).thenReturn("/app");
            // when
            s.doGet(req, resp);
            // then
            web.verify(() -> Web.buildQuestSvgModel(eq(req), argThat(list ->
                    list.size() == 1 && list.getFirst() == node
            ), eq(11), eq(true)));
            verify(req).setAttribute("previewTitle", "New quest: Cool");
            verify(req).setAttribute("backUrl", "/app" + WebConst.Path.QUESTS_MOD);
            web.verify(() -> Web.forward(req, resp, WebConst.Jsp.QUESTS_MOD_PREVIEW));
        }
    }

    @Test
    @DisplayName("given kind=edit and item not found when doGet then 404")
    void doGet_edit_notFound() throws Exception {
        // given
        AdminModerationGraphServlet s = new AdminModerationGraphServlet();
        when(config.getServletContext()).thenReturn(ctx);
        try (MockedStatic<Web> web = mockStatic(Web.class)) {
            web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.AUTHORING_SERVICE, QuestAuthoringService.class))
                    .thenReturn(authoring);
            web.when(() -> Web.trimOrNull("edit")).thenReturn("edit");
            web.when(() -> Web.trimOrNull("q1")).thenReturn("q1");
            s.init(config);
            when(authoring.listPendingEdits()).thenReturn(List.of());
            when(req.getParameter("kind")).thenReturn("edit");
            when(req.getParameter("id")).thenReturn("q1");
            // when
            s.doGet(req, resp);
            // then
            verify(resp).sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Test
    @DisplayName("given kind=edit and item found when doGet then builds model, sets attrs, forwards")
    void doGet_edit_ok() throws Exception {
        // given
        AdminModerationGraphServlet s = new AdminModerationGraphServlet();
        when(config.getServletContext()).thenReturn(ctx);
        List<QuestNode> srcNodes = Arrays.asList(node, null);
        when(pe.getQuestId()).thenReturn("q1");
        when(pe.getStartId()).thenReturn(5);
        when(pe.getNodes()).thenReturn(srcNodes);
        when(pe.getName()).thenReturn("Edited");
        try (MockedStatic<Web> web = mockStatic(Web.class)) {
            web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.AUTHORING_SERVICE, QuestAuthoringService.class))
                    .thenReturn(authoring);
            web.when(() -> Web.trimOrNull("edit")).thenReturn("edit");
            web.when(() -> Web.trimOrNull("q1")).thenReturn("q1");
            web.when(() -> Web.buildQuestSvgModel(eq(req), anyList(), eq(5), eq(true))).then(inv -> null);
            web.when(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.QUESTS_MOD_PREVIEW))).then(inv -> null);
            s.init(config);
            when(authoring.listPendingEdits()).thenReturn(List.of(pe));
            when(req.getParameter("kind")).thenReturn("edit");
            when(req.getParameter("id")).thenReturn("q1");
            when(req.getContextPath()).thenReturn("/app");
            // when
            s.doGet(req, resp);
            // then
            web.verify(() -> Web.buildQuestSvgModel(eq(req), argThat(list ->
                    list.size() == 1 && list.getFirst() == node
            ), eq(5), eq(true)));
            verify(req).setAttribute("previewTitle", "Quest edits: Edited");
            verify(req).setAttribute("backUrl", "/app" + WebConst.Path.QUESTS_MOD);
            web.verify(() -> Web.forward(req, resp, WebConst.Jsp.QUESTS_MOD_PREVIEW));
        }
    }
}