package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.app.WebConst;
import com.javarush.apalinskiy.domain.quest.QuestNode;
import com.javarush.apalinskiy.domain.quest.custom.CustomQuest;
import com.javarush.apalinskiy.service.quest.QuestAuthoringService;
import com.javarush.apalinskiy.web.util.FormQuestNodeParser;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("CreateQuestServlet (unit)")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class CreateQuestServletTest {

    @Mock
    ServletConfig config;
    @Mock
    ServletContext servletCtx;
    @Mock
    QuestAuthoringService authoring;
    @Mock
    HttpServletRequest req;
    @Mock
    HttpServletResponse resp;
    @Mock
    HttpSession session;

    private CreateQuestServlet sut;

    private void initServletHappy() throws ServletException {
        sut = new CreateQuestServlet();
        when(config.getServletContext()).thenReturn(servletCtx);
        try (MockedStatic<Web> web = mockStatic(Web.class)) {
            web.when(() -> Web.ctxBean(servletCtx, WebConst.Ctx.AUTHORING_SERVICE, QuestAuthoringService.class))
                    .thenReturn(authoring);
            sut.init(config);
        }
    }

    @Nested
    @DisplayName("init(config)")
    class InitPhase {

        @Test
        @DisplayName("Given bean exists — When init — Then authoring resolved via Web.ctxBean")
        void initOk() throws ServletException {
            // Given
            sut = new CreateQuestServlet();
            when(config.getServletContext()).thenReturn(servletCtx);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.ctxBean(servletCtx, WebConst.Ctx.AUTHORING_SERVICE, QuestAuthoringService.class))
                        .thenReturn(authoring);
                // When
                sut.init(config);
                // Then
                web.verify(() -> Web.ctxBean(servletCtx, WebConst.Ctx.AUTHORING_SERVICE, QuestAuthoringService.class));
            }
        }

        @Test
        @DisplayName("Given missing bean — When init — Then throws UnavailableException")
        void initMissing() {
            // Given
            sut = new CreateQuestServlet();
            when(config.getServletContext()).thenReturn(servletCtx);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.ctxBean(servletCtx, WebConst.Ctx.AUTHORING_SERVICE, QuestAuthoringService.class))
                        .thenThrow(new IllegalStateException("no bean"));
                // When / Then
                assertThrows(UnavailableException.class, () -> sut.init(config));
            }
        }
    }

    @Nested
    @DisplayName("doGet(req, resp)")
    class DoGet {

        @BeforeEach
        void setUp() throws ServletException {
            initServletHappy();
            lenient().when(req.getParameter(anyString())).thenReturn(null);
        }

        @Test
        @DisplayName("Given ?new present — When doGet — Then clearEditorDraft, remove session attrs, redirectOk")
        void newDraft() throws Exception {
            // Given
            when(req.getParameter(WebConst.Param.NEW)).thenReturn("1");
            when(req.getSession(false)).thenReturn(session);
            when(session.getId()).thenReturn("S1");
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // When
                sut.doGet(req, resp);
                // Then
                verify(authoring).clearEditorDraft();
                verify(session).removeAttribute("editingQuestId");
                verify(session).removeAttribute("editingQuestName");
                web.verify(() -> Web.redirectOk(eq(req), eq(resp), eq(WebConst.Path.CREATE),
                        eq("An empty draft of the quest has been created")));
            }
        }

        @Test
        @DisplayName("Given ?load=q1 and quest LIVE — When doGet — Then loadToEditor, set session attrs, redirectOk")
        void loadLive() throws Exception {
            // Given
            when(req.getParameter(WebConst.Param.LOAD)).thenReturn("q1");
            when(req.getSession(true)).thenReturn(session);
            CustomQuest cq = mock(CustomQuest.class);
            when(cq.getModerationStatus()).thenReturn(CustomQuest.ModerationStatus.LIVE);
            when(cq.getName()).thenReturn("QuestName");
            when(authoring.getFromCatalog("q1")).thenReturn(Optional.of(cq));
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.trimOrNull("q1")).thenReturn("q1");
                // When
                sut.doGet(req, resp);
                // Then
                verify(authoring).loadToEditor("q1");
                verify(authoring).getFromCatalog("q1");
                verify(session).setAttribute("editingQuestId", "q1");
                verify(session).setAttribute("editingQuestName", "QuestName");
                web.verify(() -> Web.redirectOk(eq(req), eq(resp), eq(WebConst.Path.CREATE),
                        eq("The quest is uploaded to the editor")));
            }
        }

        @Test
        @DisplayName("Given ?load=q2 but authoring throws — When doGet — Then redirectErr with message")
        void loadError() throws Exception {
            // Given
            when(req.getParameter(WebConst.Param.LOAD)).thenReturn("q2");
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.trimOrNull("q2")).thenReturn("q2");
                doThrow(new IllegalArgumentException("bad")).when(authoring).loadToEditor("q2");
                // When
                sut.doGet(req, resp);
                // Then
                web.verify(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.CREATE),
                        eq("Couldn't upload the quest: bad")));
            }
        }

        @Test
        @DisplayName("Given prefill by id — When doGet — Then prefill attributes and forward to JSP")
        void prefillById() throws Exception {
            // Given
            when(req.getParameter(WebConst.Param.ID)).thenReturn("42");
            QuestNode node = mock(QuestNode.class);
            when(node.getId()).thenReturn(42);
            when(node.getText()).thenReturn("Hello");
            when(node.getFin()).thenReturn(true);
            when(node.getImage()).thenReturn("/img.png");
            when(authoring.get(42)).thenReturn(node);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // When
                sut.doGet(req, resp);
                // Then
                verify(req).setAttribute("form_id", 42);
                verify(req).setAttribute("form_text", "Hello");
                verify(req).setAttribute("form_final", true);
                verify(req).setAttribute("form_image", "/img.png");
                web.verify(() -> Web.forward(req, resp, WebConst.Jsp.CREATE));
            }
        }

        @Test
        @DisplayName("Given no id and no stash — When doGet — Then prefill from FIRST node (if present) and forward")
        void prefillFromFirst() throws Exception {
            // Given
            when(req.getSession(false)).thenReturn(null);
            QuestNode first = mock(QuestNode.class);
            when(first.getId()).thenReturn(1);
            when(first.getText()).thenReturn("First");
            when(first.getFin()).thenReturn(true);
            when(first.getImage()).thenReturn(null);
            when(authoring.nodes()).thenReturn(List.of(first));
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // When
                sut.doGet(req, resp);
                // Then
                verify(req).setAttribute("form_id", 1);
                verify(req).setAttribute("form_text", "First");
                verify(req).setAttribute("form_final", true);
                verify(req).setAttribute("form_image", null);
                web.verify(() -> Web.forward(req, resp, WebConst.Jsp.CREATE));
            }
        }

        @Test
        @DisplayName("Given stash in session — When doGet — Then restore stash and forward")
        void restoreStash() throws Exception {
            // Given
            when(req.getSession(false)).thenReturn(session);
            when(session.getAttribute("form_id")).thenReturn("7");
            when(session.getAttribute("form_text")).thenReturn("text");
            when(session.getAttribute("form_final")).thenReturn(Boolean.TRUE);
            when(session.getAttribute("form_options")).thenReturn("go -> 2");
            when(session.getAttribute("form_image")).thenReturn("/a.jpg");
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // When
                sut.doGet(req, resp);
                // Then
                verify(req).setAttribute("form_id", "7");
                verify(req).setAttribute("form_text", "text");
                verify(req).setAttribute("form_final", true);
                verify(req).setAttribute("form_options", "go -> 2");
                verify(req).setAttribute("form_image", "/a.jpg");
                verify(session).removeAttribute("form_id");
                verify(session).removeAttribute("form_text");
                verify(session).removeAttribute("form_final");
                verify(session).removeAttribute("form_options");
                verify(session).removeAttribute("form_image");
                web.verify(() -> Web.forward(req, resp, WebConst.Jsp.CREATE));
            }
        }

        @Test
        @DisplayName("Given clear=1 — When doGet — Then render empty form via forward")
        void clearSkipsPrefill() throws Exception {
            // Given
            when(req.getParameter(WebConst.Param.CLEAR)).thenReturn("1");
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // When
                sut.doGet(req, resp);
                // Then
                web.verify(() -> Web.forward(req, resp, WebConst.Jsp.CREATE));
            }
        }
    }

    @Nested
    @DisplayName("doPost(req, resp)")
    class DoPost {

        @BeforeEach
        void setUp() throws ServletException {
            initServletHappy();
        }

        @Test
        @DisplayName("Given missing action — When doPost — Then redirectErr(The action is not specified)")
        void missingAction() throws Exception {
            // Given
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn(null);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // When
                sut.doPost(req, resp);
                // Then
                web.verify(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.CREATE),
                        eq("The action is not specified")));
            }
        }

        @Test
        @DisplayName("Given replaceNode non-final without options — When doPost — Then stash form and redirectErr")
        void replaceNodeNoOptions() throws Exception {
            // Given
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("replaceNode");
            when(req.getParameter(WebConst.Param.FINAL)).thenReturn(null);
            when(req.getParameter(WebConst.Param.OPTIONS)).thenReturn("   ");
            when(req.getSession(true)).thenReturn(session);
            try (MockedStatic<Web> web = mockStatic(Web.class);
                 MockedStatic<FormQuestNodeParser> parser = mockStatic(FormQuestNodeParser.class)) {
                // When
                sut.doPost(req, resp);
                // Then
                verify(session).setAttribute(eq("form_final"), eq(false));
                web.verify(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.CREATE),
                        eq("For the NON-final branch, you must specify at least one answer option.\n")));
                parser.verifyNoInteractions();
                verify(authoring, never()).saveNode(any());
            }
        }

        @Test
        @DisplayName("Given replaceNode final — When doPost — Then parseNode, saveNode, redirect with OK flash")
        void replaceNodeFinalHappy() throws Exception {
            // Given
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("replaceNode");
            when(req.getParameter(WebConst.Param.FINAL)).thenReturn("on");
            when(req.getContentType()).thenReturn(null);
            QuestNode node = mock(QuestNode.class);
            when(node.getId()).thenReturn(5);
            when(node.getFin()).thenReturn(true);
            when(node.getImage()).thenReturn(null);
            try (MockedStatic<FormQuestNodeParser> parser = mockStatic(FormQuestNodeParser.class);
                 MockedStatic<Web> web = mockStatic(Web.class)) {
                parser.when(() -> FormQuestNodeParser.parseNode(req)).thenReturn(node);
                // When
                sut.doPost(req, resp);
                // Then
                verify(authoring).saveNode(same(node));
                web.verify(() -> Web.redirect(eq(req), eq(resp), eq(WebConst.Path.CREATE),
                        argThat((Map<String, String> m) ->
                                "1".equals(m.get(WebConst.Param.CLEAR)) &&
                                        m.get(WebConst.Attr.OK).contains("Node #5 saved")
                        )));
            }
        }

        @Test
        @DisplayName("Given deleteNode with bad id — When doPost — Then stash form and redirectErr")
        void deleteNodeBadId() throws Exception {
            // Given
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("deleteNode");
            when(req.getParameter(WebConst.Param.ID)).thenReturn("oops");
            when(req.getSession(true)).thenReturn(session);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // When
                sut.doPost(req, resp);
                // Then
                verify(session).setAttribute(eq("form_id"), any());
                web.verify(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.CREATE),
                        eq("Specify the correct ID to delete")));
                verify(authoring, never()).deleteNode(anyInt());
            }
        }

        @Test
        @DisplayName("Given deleteNode not found — When doPost — Then stash form and redirectErr 'not found'")
        void deleteNodeNotFound() throws Exception {
            // Given
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("deleteNode");
            when(req.getParameter(WebConst.Param.ID)).thenReturn("13");
            when(authoring.deleteNode(13)).thenReturn(false);
            when(req.getSession(true)).thenReturn(session);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // When
                sut.doPost(req, resp);
                // Then
                web.verify(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.CREATE),
                        eq("Node #13 not found in the draft")));
            }
        }

        @Test
        @DisplayName("Given deleteNode ok — When doPost — Then redirect with OK flash")
        void deleteNodeOk() throws Exception {
            // Given
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("deleteNode");
            when(req.getParameter(WebConst.Param.ID)).thenReturn("7");
            when(authoring.deleteNode(7)).thenReturn(true);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // When
                sut.doPost(req, resp);
                // Then
                web.verify(() -> Web.redirect(eq(req), eq(resp), eq(WebConst.Path.CREATE),
                        argThat((Map<String, String> m) ->
                                "1".equals(m.get(WebConst.Param.CLEAR)) &&
                                        m.get(WebConst.Attr.OK).contains("Node #7 deleted")
                        )));
            }
        }

        @Test
        @DisplayName("Given unknown action — When doPost — Then sendError via resp.sendRedirect(...)")
        void unknownAction() throws Exception {
            // Given
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("doSomethingElse");
            when(resp.encodeRedirectURL(anyString())).thenAnswer(inv -> inv.getArgument(0));
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.urlEncode(anyString())).thenAnswer(inv -> inv.getArgument(0));
                // When
                sut.doPost(req, resp);
                // Then
                verify(resp).sendRedirect(argThat(s -> s.startsWith(WebConst.Path.CREATE + "?" + WebConst.Attr.ERROR + "=")));
            }
        }

        @Test
        @DisplayName("Given business exception — When doPost — Then stash form and redirectErr with message")
        void businessException() throws Exception {
            // Given
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("replaceNode");
            when(req.getParameter(WebConst.Param.FINAL)).thenReturn("on");
            QuestNode node = mock(QuestNode.class);
            try (MockedStatic<FormQuestNodeParser> parser = mockStatic(FormQuestNodeParser.class);
                 MockedStatic<Web> web = mockStatic(Web.class)) {
                parser.when(() -> FormQuestNodeParser.parseNode(req)).thenReturn(node);
                doThrow(new IllegalStateException("bad state")).when(authoring).saveNode(any());
                when(req.getSession(true)).thenReturn(session);
                // When
                sut.doPost(req, resp);
                // Then
                verify(session).setAttribute(eq("form_id"), any());
                web.verify(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.CREATE),
                        eq("bad state")));
            }
        }
    }
}