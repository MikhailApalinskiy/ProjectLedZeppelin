package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.app.WebConst;
import com.javarush.apalinskiy.web.util.Web;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HomeServlet")
class HomeServletTest {

    @Mock
    HttpServletRequest req;
    @Mock
    HttpServletResponse resp;

    @Nested
    @DisplayName("doGet")
    class DoGet {

        @Test
        @DisplayName("given '/app/' when doGet then sets no-cache and forwards to INDEX")
        void rootSlash_forwardsIndex() throws Exception {
            // given
            when(req.getContextPath()).thenReturn("/app");
            when(req.getRequestURI()).thenReturn("/app/");
            HomeServlet s = new HomeServlet();
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.INDEX))).then(inv -> null);
                // when
                s.doGet(req, resp);
                // then
                verify(resp).setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
                verify(resp).setHeader("Pragma", "no-cache");
                web.verify(() -> Web.forward(req, resp, WebConst.Jsp.INDEX));
                verify(resp, never()).sendError(anyInt());
            }
        }

        @Test
        @DisplayName("given '/app' (empty rest) when doGet then forwards to INDEX")
        void emptyRest_forwardsIndex() throws Exception {
            // given
            when(req.getContextPath()).thenReturn("/app");
            when(req.getRequestURI()).thenReturn("/app");
            HomeServlet s = new HomeServlet();
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.INDEX))).then(inv -> null);
                // when
                s.doGet(req, resp);
                // then
                verify(resp).setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
                verify(resp).setHeader("Pragma", "no-cache");
                web.verify(() -> Web.forward(req, resp, WebConst.Jsp.INDEX));
                verify(resp, never()).sendError(anyInt());
            }
        }

        @Test
        @DisplayName("given '/app/abc' when doGet then sets no-cache and returns 404")
        void nonRoot_returns404() throws Exception {
            // given
            when(req.getContextPath()).thenReturn("/app");
            when(req.getRequestURI()).thenReturn("/app/abc");
            HomeServlet s = new HomeServlet();
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                // when
                s.doGet(req, resp);
                // then
                verify(resp).setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
                verify(resp).setHeader("Pragma", "no-cache");
                verify(resp).sendError(HttpServletResponse.SC_NOT_FOUND);
                web.verifyNoInteractions();
            }
        }
    }

    @Nested
    @DisplayName("doPost")
    class DoPost {

        @Test
        @DisplayName("given any POST when doPost then returns 405")
        void post_returns405() throws IOException {
            // given
            HomeServlet s = new HomeServlet();
            // when
            s.doPost(req, resp);
            // then
            verify(resp).sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }
    }
}