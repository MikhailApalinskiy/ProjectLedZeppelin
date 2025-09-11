package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.app.WebConst;
import com.javarush.apalinskiy.service.quest.QuestAuthoringService;
import com.javarush.apalinskiy.service.quest.QuestService;
import com.javarush.apalinskiy.service.save.SaveStateService;
import com.javarush.apalinskiy.web.util.Web;
import jakarta.servlet.RequestDispatcher;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GoSavesServlet")
class GoSavesServletTest {

    @Mock
    HttpServletRequest req;
    @Mock
    HttpServletResponse resp;
    @Mock
    HttpSession session;
    @Mock
    RequestDispatcher rd;
    @Mock
    SaveStateService saveState;
    @Mock
    QuestService questService;
    @Mock
    QuestAuthoringService authoring;

    private static void setFieldDeep(Object target, String field, Object value) {
        Class<?> c = target.getClass();
        while (c != null) {
            try {
                Field f = c.getDeclaredField(field);
                f.setAccessible(true);
                f.set(target, value);
                return;
            } catch (NoSuchFieldException ignored) {
                c = c.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        throw new RuntimeException(new NoSuchFieldException(field));
    }

    @Nested
    @DisplayName("handleGo()")
    class HandleGo {

        @Test
        @DisplayName("given existing slot when handleGo then forward to confirm with filled attrs")
        void existing_forwardsToConfirm() throws Exception {
            // given
            GoSavesServlet s = new GoSavesServlet();
            setFieldDeep(s, "saveState", saveState);
            setFieldDeep(s, "questService", questService);
            setFieldDeep(s, "authoring", authoring);
            when(req.getParameter(WebConst.Param.NODE)).thenReturn("7");
            SaveStateService.GlobalSlot g = mock(SaveStateService.GlobalSlot.class);
            when(g.nodeId()).thenReturn(5);
            when(g.title()).thenReturn("Old title");
            when(saveState.getGlobalSlot("u1", 2)).thenReturn(Optional.of(g));
            when(req.getRequestDispatcher(WebConst.Jsp.CONFIRM)).thenReturn(rd);
            when(questService.getById(7)).thenReturn(null);
            // when
            s.handleGo(req, resp, "u1", "main", 2, "/next");
            // then
            verify(req).setAttribute("slotIndex", 2);
            verify(req).setAttribute("newNodeId", 7);
            verify(req).setAttribute(eq("newNodeTitle"), eq("Node #7"));
            verify(req).setAttribute("oldNodeId", 5);
            verify(req).setAttribute("oldNodeTitle", "Old title");
            verify(req).setAttribute(WebConst.Param.NEXT, "/next");
            verify(req).setAttribute(eq(WebConst.Param.PURPOSE), eq("save"));
            verify(rd).forward(req, resp);
            verifyNoMoreInteractions(rd);
        }

        @Test
        @DisplayName("given empty slot (no existing) when handleGo then set slot, flash, redirect to quest")
        void empty_setsSlotAndRedirects() throws Exception {
            // given
            GoSavesServlet s = new GoSavesServlet();
            setFieldDeep(s, "saveState", saveState);
            setFieldDeep(s, "questService", questService);
            when(req.getParameter(WebConst.Param.NODE)).thenReturn("42");
            when(saveState.getGlobalSlot("u1", 3)).thenReturn(Optional.empty());
            when(req.getSession()).thenReturn(session);
            when(resp.encodeRedirectURL(anyString())).thenAnswer(a -> a.getArgument(0));
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.buildQuestUrlFromNext(eq(req), eq("/quest"), eq(42)))
                        .thenReturn("/app/quest?id=42");
                when(questService.getById(42)).thenReturn(null);
                // when
                s.handleGo(req, resp, "u1", null, 3, "/quest");
                // then
                verify(saveState).setGlobalSlot("u1", 3, "main", "main", 42, "Node #42");
                verify(session).setAttribute(eq(WebConst.Attr.FLASH),
                        eq("The save is recorded in the slot №" + (3 + 1) + "."));
                ArgumentCaptor<String> cap = ArgumentCaptor.forClass(String.class);
                verify(resp).sendRedirect(cap.capture());
                assertEquals("/app/quest?id=42", cap.getValue());
            }
        }
    }

    @Nested
    @DisplayName("handleConfirm()")
    class HandleConfirm {

        @Test
        @DisplayName("given slot and custom quest when handleConfirm then overwrite, flash, redirect")
        void confirm_overwrite_andRedirect() throws Exception {
            // given
            GoSavesServlet s = new GoSavesServlet();
            setFieldDeep(s, "saveState", saveState);
            when(req.getParameter(WebConst.Param.NODE)).thenReturn("11");
            when(req.getSession()).thenReturn(session);
            when(resp.encodeRedirectURL(anyString())).thenAnswer(a -> a.getArgument(0));
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.buildQuestUrlFromNext(eq(req), eq("/x"), eq(11)))
                        .thenReturn("/go?id=11");
                // when
                s.handleConfirm(req, resp, "user-1", "custom-q", 0, "/x");
                // then
                verify(saveState).setGlobalSlot("user-1", 0, "custom-q", "Custom quest", 11, "Node #11");
                verify(session).setAttribute(eq(WebConst.Attr.FLASH),
                        eq("Slot №" + (1) + " is overwritten. Saved."));
                ArgumentCaptor<String> cap = ArgumentCaptor.forClass(String.class);
                verify(resp).sendRedirect(cap.capture());
                assertEquals("/go?id=11", cap.getValue());
            }
        }
    }

    @Nested
    @DisplayName("trivial endpoints")
    class Trivial {

        @Test
        @DisplayName("given servlet when path then /saves")
        void path_ok() {
            GoSavesServlet s = new GoSavesServlet();
            assertEquals(WebConst.Path.SAVES, s.path());
        }

        @Test
        @DisplayName("given servlet when listJsp then saves.jsp")
        void listJsp_ok() {
            GoSavesServlet s = new GoSavesServlet();
            assertEquals(WebConst.Jsp.SAVES, s.listJsp());
        }

        @Test
        @DisplayName("given servlet when confirmJsp then confirm.jsp")
        void confirmJsp_ok() {
            GoSavesServlet s = new GoSavesServlet();
            assertEquals(WebConst.Jsp.CONFIRM, s.confirmJsp());
        }
    }
}