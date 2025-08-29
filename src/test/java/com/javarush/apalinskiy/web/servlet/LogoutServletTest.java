package com.javarush.apalinskiy.web.servlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogoutServletTest {

    @Mock
    private HttpServletRequest req;
    @Mock
    private HttpServletResponse resp;
    @Mock
    private HttpSession session;
    private LogoutServlet servlet;

    @BeforeEach
    void setUp() {
        servlet = new LogoutServlet();
    }

    @Nested
    class DoGet {

        @Test
        void returns405AndDoesNotRedirectOrTouchSessionTest() throws Exception {
            // when
            servlet.doGet(req, resp);
            // then
            verify(resp, times(1)).sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            verify(resp, never()).sendRedirect(anyString());
            verify(req, never()).getSession();
            verify(req, never()).getSession(anyBoolean());
            verifyNoMoreInteractions(resp);
            verifyNoInteractions(req);
        }

        @Test
        void propagatesIOExceptionFromSendErrorTest() throws Exception {
            // given
            doThrow(new IOException())
                    .when(resp).sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            // when / then
            assertThrows(IOException.class, () -> servlet.doGet(req, resp));
        }
    }

    @Nested
    class DoPost {

        @Test
        void withSessionInvalidatesThenRedirectsTest() throws Exception {
            // given
            when(req.getSession(false)).thenReturn(session);
            when(req.getContextPath()).thenReturn("");
            // when
            servlet.doPost(req, resp);
            // then
            InOrder inOrder = inOrder(session, resp);
            inOrder.verify(session).invalidate();
            inOrder.verify(resp).sendRedirect("/");
            verify(resp, never()).sendError(anyInt());
            verify(req, times(1)).getSession(false);
            verify(req, never()).getSession();
            verify(req, never()).getSession(true);
        }

        @Test
        void withoutSessionRedirectsOnlyTest() throws Exception {
            // given
            when(req.getSession(false)).thenReturn(null);
            when(req.getContextPath()).thenReturn("/app");
            // when
            servlet.doPost(req, resp);
            // then
            verify(req, times(1)).getSession(false);
            verify(session, never()).invalidate();
            verify(resp, times(1)).sendRedirect("/app/");
            verify(resp, never()).sendError(anyInt());
        }

        @Test
        void doesNotCreateNewSessionTest() throws Exception {
            // given
            when(req.getSession(false)).thenReturn(session);
            when(req.getContextPath()).thenReturn("");
            // when
            servlet.doPost(req, resp);
            // then
            verify(req, never()).getSession(true);
            verify(req, never()).getSession();
        }

        @Test
        void propagatesIOExceptionFromSendRedirectTest() throws Exception {
            // given
            when(req.getSession(false)).thenReturn(null);
            when(req.getContextPath()).thenReturn("");
            doThrow(new IOException())
                    .when(resp).sendRedirect("/");
            // when / then
            assertThrows(IOException.class, () -> servlet.doPost(req, resp));
        }
    }

    @Nested
    class RedirectTargets {

        @ParameterizedTest(name = "contextPath=\"{0}\" -> redirect to \"{1}\"")
        @CsvSource({
                "'', /",
                "/app, /app/",
                "/root, /root/"
        })
        void redirectsToContextRootForDifferentContextPathsTest(String contextPath, String expected) throws Exception {
            // given
            when(req.getSession(false)).thenReturn(null);
            when(req.getContextPath()).thenReturn(contextPath);
            // when
            servlet.doPost(req, resp);
            // then
            verify(resp, times(1)).sendRedirect(expected);
            verify(resp, never()).sendError(anyInt());
            verify(req, times(1)).getSession(false);
            verify(req, times(1)).getContextPath();
            verifyNoMoreInteractions(resp, req);
        }

        @ParameterizedTest(name = "with session: contextPath=\"{0}\" -> redirect \"{1}\" after invalidate()")
        @CsvSource({
                "'', /",
                "/app, /app/",
                "/root, /root/"
        })
        void withSessionInvalidatesThenRedirectsToExpectedUrlTest(String contextPath, String expected) throws Exception {
            // given
            when(req.getSession(false)).thenReturn(session);
            when(req.getContextPath()).thenReturn(contextPath);
            // when
            servlet.doPost(req, resp);
            // then
            InOrder inOrder = inOrder(session, resp);
            inOrder.verify(session).invalidate();
            inOrder.verify(resp).sendRedirect(expected);
        }
    }
}