package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.app.WebConst;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.web.view.SlotView;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("AbstractSlotsServlet")
@ExtendWith(MockitoExtension.class)
class AbstractSlotsServletTest {

    @Mock
    HttpServletRequest req;
    @Mock
    HttpServletResponse resp;
    @Mock
    RequestDispatcher dispatcher;
    @Mock
    User user;

    static class TestSlotsServlet extends AbstractSlotsServlet {
        boolean goCalled, confirmCalled, cancelCalled;
        String userIdArg, questIdArg, nextArg;
        int slotArg;
        String confirmJspPath = WebConst.Jsp.CONFIRM;

        @Override
        protected String path() {
            return WebConst.Path.SAVES;
        }

        @Override
        protected String listJsp() {
            return WebConst.Jsp.SAVES;
        }

        @Override
        protected String confirmJsp() {
            return confirmJspPath;
        }

        @Override
        protected void handleGo(HttpServletRequest req, HttpServletResponse resp,
                                String userId, String questId, int slot, String next) {
            goCalled = true;
            userIdArg = userId;
            questIdArg = questId;
            slotArg = slot;
            nextArg = next;
        }

        @Override
        protected void handleConfirm(HttpServletRequest req, HttpServletResponse resp,
                                     String userId, String questId, int slot, String next) {
            confirmCalled = true;
            userIdArg = userId;
            questIdArg = questId;
            slotArg = slot;
            nextArg = next;
        }

        @Override
        protected void handleCancel(HttpServletRequest req, HttpServletResponse resp,
                                    String userId, String questId, int slot) {
            cancelCalled = true;
            userIdArg = userId;
            questIdArg = questId;
            slotArg = slot;
        }
    }

    @Nested
    @DisplayName("doGet")
    class DoGet {

        @Test
        @DisplayName("given unauthenticated when doGet then returns immediately (no forward)")
        void unauthenticated() throws Exception {
            // given
            TestSlotsServlet servlet = spy(new TestSlotsServlet());
            doReturn(null).when(servlet).requireAuthOrRedirect(eq(req), eq(resp), anyString());
            // when
            servlet.doGet(req, resp);
            // then
            verify(req, never()).getRequestDispatcher(anyString());
            verify(resp, never()).sendError(anyInt(), anyString());
        }

        @Test
        @DisplayName("given authenticated when doGet then puts 'slots' and forwards to listJsp")
        void authenticated() throws Exception {
            // given
            TestSlotsServlet servlet = spy(new TestSlotsServlet());
            doReturn(user).when(servlet).requireAuthOrRedirect(eq(req), eq(resp), anyString());
            when(user.getUserId()).thenReturn("7");
            List<SlotView> slots = List.of(SlotView.empty(0, "qid", "qname"));
            doReturn(slots).when(servlet).buildSlotsAll("7");
            when(req.getRequestDispatcher(WebConst.Jsp.SAVES)).thenReturn(dispatcher);
            // when
            servlet.doGet(req, resp);
            // then
            verify(req).setAttribute("slots", slots);
            verify(dispatcher).forward(req, resp);
        }
    }

    @Nested
    @DisplayName("doPost")
    class DoPost {

        @Test
        @DisplayName("given bad params (null op / invalid slot) when doPost then 400")
        void badParams400() throws Exception {
            // given
            TestSlotsServlet servlet = spy(new TestSlotsServlet());
            doReturn(user).when(servlet).requireAuthOrRedirect(eq(req), eq(resp), anyString());
            when(req.getParameter(WebConst.Param.OP)).thenReturn(null);
            when(req.getParameter(WebConst.Param.SLOT)).thenReturn("-1");
            // when
            servlet.doPost(req, resp);
            // then
            verify(resp).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
            verifyNoMoreInteractions(resp);
        }

        @Test
        @DisplayName("given op=GO valid params when doPost then handleGo called with parsed values")
        void routesToGo() throws Exception {
            // given
            TestSlotsServlet servlet = spy(new TestSlotsServlet());
            doReturn(user).when(servlet).requireAuthOrRedirect(eq(req), eq(resp), anyString());
            when(user.getUserId()).thenReturn("123");
            when(req.getParameter(WebConst.Param.OP)).thenReturn(WebConst.Op.GO);
            when(req.getParameter(WebConst.Param.SLOT)).thenReturn("2");
            when(req.getParameter(WebConst.Param.NEXT)).thenReturn("/next");
            when(req.getParameter(WebConst.Param.CUSTOM)).thenReturn("  custom  ");
            // when
            servlet.doPost(req, resp);
            // then
            assertTrue(servlet.goCalled);
            assertEquals("123", servlet.userIdArg);
            assertEquals("custom", servlet.questIdArg);
            assertEquals(2, servlet.slotArg);
            assertEquals("/next", servlet.nextArg);
        }

        @Test
        @DisplayName("given op=CONFIRM and confirmJsp=null when doPost then 405")
        void confirmWithoutJsp405() throws Exception {
            // given
            TestSlotsServlet servlet = spy(new TestSlotsServlet());
            servlet.confirmJspPath = null;
            doReturn(user).when(servlet).requireAuthOrRedirect(eq(req), eq(resp), anyString());
            when(user.getUserId()).thenReturn("1");
            when(req.getParameter(WebConst.Param.OP)).thenReturn(WebConst.Op.CONFIRM);
            when(req.getParameter(WebConst.Param.SLOT)).thenReturn("0");
            when(req.getParameter(WebConst.Param.NEXT)).thenReturn(null);
            when(req.getParameter(WebConst.Param.CUSTOM)).thenReturn(null);
            // when
            servlet.doPost(req, resp);
            // then
            verify(resp).sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            assertFalse(servlet.confirmCalled);
        }

        @Test
        @DisplayName("given op=CONFIRM and confirmJsp!=null when doPost then handleConfirm called")
        void routesToConfirm() throws Exception {
            // given
            TestSlotsServlet servlet = spy(new TestSlotsServlet());
            servlet.confirmJspPath = WebConst.Jsp.CONFIRM;
            doReturn(user).when(servlet).requireAuthOrRedirect(eq(req), eq(resp), anyString());
            when(user.getUserId()).thenReturn("5");
            when(req.getParameter(WebConst.Param.OP)).thenReturn(WebConst.Op.CONFIRM);
            when(req.getParameter(WebConst.Param.SLOT)).thenReturn("3");
            when(req.getParameter(WebConst.Param.NEXT)).thenReturn("/quest");
            when(req.getParameter(WebConst.Param.CUSTOM)).thenReturn("main");
            // when
            servlet.doPost(req, resp);
            // then
            assertTrue(servlet.confirmCalled);
            assertEquals("5", servlet.userIdArg);
            assertEquals("main", servlet.questIdArg);
            assertEquals(3, servlet.slotArg);
            assertEquals("/quest", servlet.nextArg);
        }

        @Test
        @DisplayName("given op=CANCEL and confirmJsp=null when doPost then 405")
        void cancelWithoutJsp405() throws Exception {
            // given
            TestSlotsServlet servlet = spy(new TestSlotsServlet());
            servlet.confirmJspPath = null;
            doReturn(user).when(servlet).requireAuthOrRedirect(eq(req), eq(resp), anyString());
            when(user.getUserId()).thenReturn("1");
            when(req.getParameter(WebConst.Param.OP)).thenReturn(WebConst.Op.CANCEL);
            when(req.getParameter(WebConst.Param.SLOT)).thenReturn("1");
            // when
            servlet.doPost(req, resp);
            // then
            verify(resp).sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            assertFalse(servlet.cancelCalled);
        }

        @Test
        @DisplayName("given op=CANCEL and confirmJsp!=null when doPost then handleCancel called")
        void routesToCancel() throws Exception {
            // given
            TestSlotsServlet servlet = spy(new TestSlotsServlet());
            servlet.confirmJspPath = WebConst.Jsp.CONFIRM;
            doReturn(user).when(servlet).requireAuthOrRedirect(eq(req), eq(resp), anyString());
            when(user.getUserId()).thenReturn("10");
            when(req.getParameter(WebConst.Param.OP)).thenReturn(WebConst.Op.CANCEL);
            when(req.getParameter(WebConst.Param.SLOT)).thenReturn("1");
            when(req.getParameter(WebConst.Param.CUSTOM)).thenReturn("x");
            when(req.getParameter(WebConst.Param.NEXT)).thenReturn(null);
            // when
            servlet.doPost(req, resp);
            // then
            assertTrue(servlet.cancelCalled);
            assertEquals("10", servlet.userIdArg);
            assertEquals("x", servlet.questIdArg);
            assertEquals(1, servlet.slotArg);
        }
    }

    @Nested
    @DisplayName("default handleCancel (super)")
    class DefaultCancelBehavior {

        static class CancelSuperServlet extends AbstractSlotsServlet {
            @Override
            protected String path() {
                return WebConst.Path.SAVES;
            }

            @Override
            protected String listJsp() {
                return WebConst.Jsp.SAVES;
            }

            @Override
            protected String confirmJsp() {
                return WebConst.Jsp.CONFIRM;
            }
        }

        @Test
        @DisplayName("given params when handleCancel then redirects back with preserved params")
        void cancelRedirectsBack() throws Exception {
            // given
            CancelSuperServlet servlet = spy(new CancelSuperServlet());
            when(req.getContextPath()).thenReturn("/app");
            when(resp.encodeRedirectURL(anyString())).thenAnswer(a -> a.getArgument(0));
            when(req.getParameter(WebConst.Param.NEXT)).thenReturn("/q");
            when(req.getParameter(WebConst.Param.PURPOSE)).thenReturn("p");
            when(req.getParameter(WebConst.Param.NODE)).thenReturn("1");
            when(req.getParameter(WebConst.Param.CUSTOM)).thenReturn("c");
            // when
            servlet.handleCancel(req, resp, "u", "c", 0);
            // then
            ArgumentCaptor<String> urlCap = ArgumentCaptor.forClass(String.class);
            verify(resp).sendRedirect(urlCap.capture());
            String url = urlCap.getValue();
            assertTrue(url.startsWith("/app" + WebConst.Path.SAVES));
            assertTrue(url.contains("next=%2Fq"));
            assertTrue(url.contains("purpose=p"));
            assertTrue(url.contains("node=1"));
            assertTrue(url.contains("custom=c"));
        }
    }
}