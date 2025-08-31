package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.quest.model.Option;
import com.javarush.apalinskiy.quest.model.QuestNode;
import com.javarush.apalinskiy.service.QuestService;
import com.javarush.apalinskiy.service.dto.ChoiceError;
import com.javarush.apalinskiy.service.dto.ChooseResult;
import com.javarush.apalinskiy.web.listener.AppBootstrap;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestServletTest {
    private QuestServlet servlet;

    // mocks
    @Mock
    private ServletConfig cfg;
    @Mock
    private ServletContext ctx;
    @Mock
    private HttpServletRequest req;
    @Mock
    private HttpServletResponse resp;
    @Mock
    private RequestDispatcher rd;
    @Mock
    private QuestService service;

    @BeforeEach
    void setUp() {
        servlet = new QuestServlet();
    }


    private void initWithService() throws Exception {
        when(cfg.getServletContext()).thenReturn(ctx);
        when(ctx.getAttribute(AppBootstrap.ATTR_QUEST_SERVICE)).thenReturn(service);
        servlet.init(cfg);
    }

    private void stubForwardPlumbing() {
        when(req.getRequestDispatcher("/WEB-INF/jsp/quest.jsp")).thenReturn(rd);
        when(service.version()).thenReturn("v1");
    }

    private QuestNode startNode() {
        return QuestNode.of(1, "Start", List.of(new Option("go", 2)), false, null);
    }

    private QuestNode endNode() {
        return QuestNode.fin(2, "End", null);
    }

    private void verifyCommonForwardAttrs(QuestNode expectedNode, String expectedErrorOrNull)
            throws ServletException, IOException {
        verify(req, atLeastOnce()).setAttribute("node", expectedNode);
        verify(req, atLeastOnce()).setAttribute("version", "v1");
        if (expectedErrorOrNull != null && !expectedErrorOrNull.isBlank()) {
            verify(req, atLeastOnce()).setAttribute("error", expectedErrorOrNull);
        } else {
            verify(req, never()).setAttribute(eq("error"), any());
        }
        verify(req).getRequestDispatcher("/WEB-INF/jsp/quest.jsp");
        verify(rd).forward(req, resp);
    }


    @Nested
    class InitLifecycle {

        @Test
        void throwsUnavailableWhenNoServiceInContextTest() {
            // Given
            QuestServlet s = new QuestServlet();
            when(cfg.getServletContext()).thenReturn(ctx);
            when(ctx.getAttribute(AppBootstrap.ATTR_QUEST_SERVICE)).thenReturn(null);
            // When / Then
            UnavailableException ex = assertThrows(UnavailableException.class, () -> s.init(cfg));
            assertEquals("QuestService is not initialized in ServletContext", ex.getMessage());
        }

        @Test
        void throwsUnavailableWhenWrongTypeInContextTest() {
            // Given
            QuestServlet s = new QuestServlet();
            when(cfg.getServletContext()).thenReturn(ctx);
            when(ctx.getAttribute(AppBootstrap.ATTR_QUEST_SERVICE)).thenReturn("not a service");
            // When / Then
            UnavailableException ex = assertThrows(UnavailableException.class, () -> s.init(cfg));
            assertEquals("QuestService is not initialized in ServletContext", ex.getMessage());
        }

        @Test
        void initOkStoresServiceAndWorksTest() throws Exception {
            // Given
            QuestServlet s = new QuestServlet();
            when(cfg.getServletContext()).thenReturn(ctx);
            when(ctx.getAttribute(AppBootstrap.ATTR_QUEST_SERVICE)).thenReturn(service);
            when(service.getStart()).thenReturn(startNode());
            when(service.version()).thenReturn("v1");
            when(req.getRequestDispatcher("/WEB-INF/jsp/quest.jsp")).thenReturn(rd);
            // When
            s.init(cfg);
            s.doGet(req, resp);
            // Then
            verify(service).getStart();
            verify(rd).forward(req, resp);
        }
    }

    @Nested
    class DoGetBehavior {

        @Test
        void blankFromIdForwardsStartWithErrorTest() throws Exception {
            // Given
            initWithService();
            stubForwardPlumbing();
            when(req.getParameter("fromId")).thenReturn("   \t ");
            QuestNode start = startNode();
            when(service.getStart()).thenReturn(start);
            // When
            servlet.doPost(req, resp);
            // Then
            verifyCommonForwardAttrs(start, "Некорректный fromId");
        }

        @Test
        void noIdParamForwardsStartTest() throws Exception {
            // Given
            initWithService();
            stubForwardPlumbing();
            when(req.getParameter("id")).thenReturn(null);
            QuestNode start = startNode();
            when(service.getStart()).thenReturn(start);
            // When
            servlet.doGet(req, resp);
            // Then
            verify(service).getStart();
            verifyCommonForwardAttrs(start, null);
        }

        @Test
        void blankIdForwardsStartTest() throws Exception {
            // Given
            initWithService();
            stubForwardPlumbing();
            when(req.getParameter("id")).thenReturn("  ");
            QuestNode start = startNode();
            when(service.getStart()).thenReturn(start);
            // When
            servlet.doGet(req, resp);
            // Then
            verify(service).getStart();
            verifyCommonForwardAttrs(start, null);
        }

        @Test
        void nonNumericIdForwardsStartTest() throws Exception {
            // Given
            initWithService();
            stubForwardPlumbing();
            when(req.getParameter("id")).thenReturn("abc");
            QuestNode start = startNode();
            when(service.getStart()).thenReturn(start);
            // When
            servlet.doGet(req, resp);
            // Then
            verify(service).getStart();
            verifyCommonForwardAttrs(start, null);
        }

        @Test
        void validIdFoundForwardsThatNodeTest() throws Exception {
            // Given
            initWithService();
            stubForwardPlumbing();
            when(req.getParameter("id")).thenReturn("2");
            QuestNode n2 = endNode();
            when(service.getById(2)).thenReturn(n2);
            // When
            servlet.doGet(req, resp);
            // Then
            verify(service).getById(2);
            verifyCommonForwardAttrs(n2, null);
        }

        @Test
        void validIdNotFoundSetsErrorAndForwardsStartTest() throws Exception {
            // Given
            initWithService();
            stubForwardPlumbing();
            when(req.getParameter("id")).thenReturn(" 42 ");
            when(service.getById(42)).thenReturn(null);
            QuestNode start = startNode();
            when(service.getStart()).thenReturn(start);
            // When
            servlet.doGet(req, resp);
            // Then
            verify(service).getById(42);
            verify(req).setAttribute("error", "Узел не найден: id= 42 ");
            verifyCommonForwardAttrs(start, "Узел не найден: id= 42 ");
        }
    }

    @Nested
    class DoPostBehavior {

        @Test
        void nullFromIdForwardsStartWithErrorTest() throws Exception {
            // Given
            initWithService();
            stubForwardPlumbing();
            when(req.getParameter("fromId")).thenReturn(null);
            QuestNode start = startNode();
            when(service.getStart()).thenReturn(start);
            // When
            servlet.doPost(req, resp);
            // Then
            verifyCommonForwardAttrs(start, "Некорректный fromId");
        }

        @Test
        void nonNumericFromIdForwardsStartWithErrorTest() throws Exception {
            // Given
            initWithService();
            stubForwardPlumbing();
            when(req.getParameter("fromId")).thenReturn("abc");
            QuestNode start = startNode();
            when(service.getStart()).thenReturn(start);
            // When
            servlet.doPost(req, resp);
            // Then
            verifyCommonForwardAttrs(start, "Некорректный fromId");
        }

        @Test
        void chooseOkRedirectsPrgTest() throws Exception {
            // Given
            initWithService();
            when(req.getParameter("fromId")).thenReturn("1");
            when(req.getParameter("answer")).thenReturn("go");
            when(service.choose(1, "go")).thenReturn(ChooseResult.ok(endNode()));
            when(req.getContextPath()).thenReturn("/app");
            when(resp.encodeRedirectURL(anyString())).thenAnswer(inv -> inv.getArgument(0));
            // When
            servlet.doPost(req, resp);
            // Then
            verify(resp).encodeRedirectURL("/app/quest?id=2");
            verify(resp).sendRedirect("/app/quest?id=2");
            verify(rd, never()).forward(any(), any());
        }

        @Test
        void chooseErrorForwardsNodeWithMessageTest() throws Exception {
            // Given
            initWithService();
            stubForwardPlumbing();
            when(req.getParameter("fromId")).thenReturn("1");
            when(req.getParameter("answer")).thenReturn("nope");
            when(service.choose(1, "nope"))
                    .thenReturn(ChooseResult.error(ChoiceError.NO_SUCH_OPTION, "Нет такого варианта ответа"));
            QuestNode start = startNode();
            when(service.getById(1)).thenReturn(start);
            // When
            servlet.doPost(req, resp);
            // Then
            verifyCommonForwardAttrs(start, "Нет такого варианта ответа");
        }

        @Test
        void chooseErrorNodeMissingFallsBackToStartTest() throws Exception {
            // Given
            initWithService();
            stubForwardPlumbing();
            when(req.getParameter("fromId")).thenReturn("1");
            when(req.getParameter("answer")).thenReturn("nope");
            when(service.choose(1, "nope"))
                    .thenReturn(ChooseResult.error(ChoiceError.NO_SUCH_OPTION, "Ошибка"));
            when(service.getById(1)).thenReturn(null);
            QuestNode start = startNode();
            when(service.getStart()).thenReturn(start);
            // When
            servlet.doPost(req, resp);
            // Then
            verify(service).getById(1);
            verify(service).getStart();
            verifyCommonForwardAttrs(start, "Ошибка");
        }

        @Test
        void chooseErrorWithBlankMessageDoesNotSetErrorAttributeTest() throws Exception {
            // Given
            initWithService();
            stubForwardPlumbing();
            when(req.getParameter("fromId")).thenReturn("1");
            when(req.getParameter("answer")).thenReturn("x");
            when(service.choose(1, "x"))
                    .thenReturn(ChooseResult.error(ChoiceError.NO_SUCH_OPTION, "  "));
            QuestNode start = startNode();
            when(service.getById(1)).thenReturn(start);
            // When
            servlet.doPost(req, resp);
            // Then
            verify(req, atLeastOnce()).setAttribute(eq("node"), same(start));
            verify(req, atLeastOnce()).setAttribute(eq("version"), eq("v1"));
            verify(req, never()).setAttribute(eq("error"), any());
            verify(rd).forward(req, resp);
        }
    }
}