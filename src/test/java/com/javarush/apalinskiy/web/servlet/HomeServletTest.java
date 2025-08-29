package com.javarush.apalinskiy.web.servlet;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HomeServletTest {

    @Mock
    private HttpServletRequest req;
    @Mock
    private HttpServletResponse resp;
    @Mock
    RequestDispatcher dispatcher;
    private HomeServlet servlet;

    @BeforeEach
    void setUp() {
        servlet = new HomeServlet();
    }

    @Nested
    class DoGet {

        @Test
        void setsCacheHeadersTest() throws Exception {
            // given
            when(req.getRequestDispatcher("/WEB-INF/jsp/index.jsp")).thenReturn(dispatcher);
            // when
            servlet.doGet(req, resp);
            // then
            verify(resp, times(1)).setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            verify(resp, times(1)).setHeader("Pragma", "no-cache");
        }

        @Test
        void forwardsToIndexJspTest() throws Exception {
            // given
            when(req.getRequestDispatcher("/WEB-INF/jsp/index.jsp")).thenReturn(dispatcher);
            // when
            servlet.doGet(req, resp);
            // then
            verify(req, times(1)).getRequestDispatcher("/WEB-INF/jsp/index.jsp");
            verify(dispatcher, times(1)).forward(req, resp);
        }

        @Test
        void doesNotRedirectOrSendErrorTest() throws Exception {
            // given
            when(req.getRequestDispatcher("/WEB-INF/jsp/index.jsp")).thenReturn(dispatcher);
            // when
            servlet.doGet(req, resp);
            // then
            verify(resp, never()).sendRedirect(anyString());
            verify(resp, never()).sendError(anyInt());
        }

        @Test
        void doesNotTouchSessionTest() throws Exception {
            // given
            when(req.getRequestDispatcher("/WEB-INF/jsp/index.jsp")).thenReturn(dispatcher);
            // when
            servlet.doGet(req, resp);
            // then
            verify(req, never()).getSession();
            verify(req, never()).getSession(anyBoolean());
        }

        @Test
        void setsHeadersBeforeForwardInOrderTest() throws Exception {
            // given
            when(req.getRequestDispatcher("/WEB-INF/jsp/index.jsp")).thenReturn(dispatcher);
            // when
            servlet.doGet(req, resp);
            // then
            InOrder inOrder = inOrder(resp, dispatcher);
            inOrder.verify(resp).setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            inOrder.verify(resp).setHeader("Pragma", "no-cache");
            inOrder.verify(dispatcher).forward(req, resp);
        }

        @Test
        void throwsIOExceptionPropagatesAndHeadersWereSetTest() throws Exception {
            // given
            when(req.getRequestDispatcher("/WEB-INF/jsp/index.jsp")).thenReturn(dispatcher);
            doThrow(new IOException("boom")).when(dispatcher).forward(any(), any());
            // when / then
            assertThrows(IOException.class, () -> servlet.doGet(req, resp));
            verify(resp).setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            verify(resp).setHeader("Pragma", "no-cache");
        }

        @Test
        void throwsServletExceptionPropagatesAndHeadersWereSetTest() throws Exception {
            // given
            when(req.getRequestDispatcher("/WEB-INF/jsp/index.jsp")).thenReturn(dispatcher);
            doThrow(new ServletException("oops")).when(dispatcher).forward(any(), any());
            // when / then
            assertThrows(ServletException.class, () -> servlet.doGet(req, resp));
            verify(resp).setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            verify(resp).setHeader("Pragma", "no-cache");
        }
    }

    @Nested
    class DoPost {

        @Test
        void returns405OnlySendErrorTest() throws Exception {
            // given
            // when
            servlet.doPost(req, resp);
            // then
            verify(resp, times(1)).sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            verify(resp, never()).sendRedirect(anyString());
            verify(req, never()).getRequestDispatcher(anyString());
        }

        @Test
        void doesNotForwardOrRedirectTest() throws Exception {
            // given / when
            servlet.doPost(req, resp);
            // then
            verify(resp, never()).sendRedirect(anyString());
            verify(resp, times(1)).sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            verify(req, never()).getRequestDispatcher(anyString());
            verifyNoMoreInteractions(resp);
        }

        @Test
        void propagatesIOExceptionFromSendErrorTest() throws Exception {
            // given
            doThrow(new IOException("fail")).when(resp).sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            // when / then
            assertThrows(IOException.class, () -> servlet.doPost(req, resp));
        }
    }
}