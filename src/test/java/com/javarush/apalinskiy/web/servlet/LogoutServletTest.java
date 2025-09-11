package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.app.WebConst;
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
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LogoutServlet")
class LogoutServletTest {

    @Mock
    HttpServletRequest req;
    @Mock
    HttpServletResponse resp;
    @Mock
    HttpSession session;

    LogoutServlet servlet;

    @BeforeEach
    void setUp() {
        servlet = new LogoutServlet();
    }

    @Nested
    @DisplayName("doGet")
    class DoGet {

        @Test
        @DisplayName("given any request when doGet then 405")
        void get_returns405() throws IOException {
            // given
            // when
            servlet.doGet(req, resp);
            // then
            verify(resp).sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            verify(resp, never()).sendRedirect(anyString());
        }
    }

    @Nested
    @DisplayName("doPost")
    class DoPost {

        @Test
        @DisplayName("given existing session when doPost then invalidate and redirect to contextPath + '/'")
        void post_withSession_invalidateAndRedirect() throws Exception {
            // given
            when(req.getSession(false)).thenReturn(session);
            when(req.getContextPath()).thenReturn("/app");
            when(resp.encodeRedirectURL(anyString())).thenAnswer(a -> a.getArgument(0));
            // when
            servlet.doPost(req, resp);
            // then
            verify(req).getSession(false);
            verify(session).invalidate();
            ArgumentCaptor<String> url = ArgumentCaptor.forClass(String.class);
            verify(resp).sendRedirect(url.capture());
            assertEquals("/app" + WebConst.Path.HOME, url.getValue());
        }

        @Test
        @DisplayName("given no session when doPost then just redirect to contextPath + '/'")
        void post_noSession_redirectOnly() throws Exception {
            // given
            when(req.getSession(false)).thenReturn(null);
            when(req.getContextPath()).thenReturn("/app");
            when(resp.encodeRedirectURL(anyString())).thenAnswer(a -> a.getArgument(0));
            // when
            servlet.doPost(req, resp);
            // then
            verify(req).getSession(false);
            verify(session, never()).invalidate();
            ArgumentCaptor<String> url = ArgumentCaptor.forClass(String.class);
            verify(resp).sendRedirect(url.capture());
            assertEquals("/app" + WebConst.Path.HOME, url.getValue());
            verify(resp, never()).sendError(anyInt());
        }
    }
}