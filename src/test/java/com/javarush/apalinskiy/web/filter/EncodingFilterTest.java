package com.javarush.apalinskiy.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EncodingFilterTest {

    @Mock
    private HttpServletRequest req;
    @Mock
    private HttpServletResponse resp;
    @Mock
    private FilterChain chain;
    private EncodingFilter filter;

    @BeforeEach
    void setUp() {
        filter = new EncodingFilter();
    }

    @Nested
    class Basics {

        @Test
        void setsUtf8OnRequestAndResponseTest() throws Exception {
            // given
            // when
            filter.doFilter(req, resp, chain);
            // then
            verify(req,  times(1)).setCharacterEncoding("UTF-8");
            verify(resp, times(1)).setCharacterEncoding("UTF-8");
        }
    }

    @Nested
    class Delegation {

        @Test
        void delegatesOnceWithSameRequestAndResponseTest() throws Exception {
            // given
            // when
            filter.doFilter(req, resp, chain);
            // then
            verify(chain, times(1)).doFilter(same(req), same(resp));
            verifyNoMoreInteractions(chain);
        }
    }

    @Nested
    class Order {

        @Test
        void encodingsAreSetBeforeChainTest() throws Exception {
            // given
            // when
            filter.doFilter(req, resp, chain);
            // then
            InOrder inOrder = inOrder(req, resp, chain);
            inOrder.verify(req).setCharacterEncoding("UTF-8");
            inOrder.verify(resp).setCharacterEncoding("UTF-8");
            inOrder.verify(chain).doFilter(req, resp);
        }
    }

    @Nested
    class Exceptions {

        @Test
        void propagatesIOExceptionAfterSettingEncodingsTest() throws Exception {
            // given
            doThrow(new IOException("io-fail")).when(chain).doFilter(req, resp);
            // when / then
            assertThrows(IOException.class, () -> filter.doFilter(req, resp, chain));
            verify(req).setCharacterEncoding("UTF-8");
            verify(resp).setCharacterEncoding("UTF-8");
        }

        @Test
        void propagatesServletExceptionAfterSettingEncodingsTest() throws Exception {
            // given
            doThrow(new ServletException("servlet-fail")).when(chain).doFilter(req, resp);
            // when / then
            assertThrows(ServletException.class, () -> filter.doFilter(req, resp, chain));
            verify(req).setCharacterEncoding("UTF-8");
            verify(resp).setCharacterEncoding("UTF-8");
        }
    }

    @Nested
    class Idempotency {

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"ISO-8859-1", "UTF-16", "UTF-8"})
        void overwritesToUtf8ForVariousInitialEncodingsTest(String initial) throws Exception {
            // given
            AtomicReference<String> reqEnc  = new AtomicReference<>(initial);
            AtomicReference<String> respEnc = new AtomicReference<>(initial);
            when(req.getCharacterEncoding()).thenAnswer(inv -> reqEnc.get());
            when(resp.getCharacterEncoding()).thenAnswer(inv -> respEnc.get());
            doAnswer(inv -> { reqEnc.set(inv.getArgument(0));  return null; })
                    .when(req).setCharacterEncoding(anyString());
            doAnswer(inv -> { respEnc.set(inv.getArgument(0)); return null; })
                    .when(resp).setCharacterEncoding(anyString());
            // when
            filter.doFilter(req, resp, chain);
            // then
            assertEquals("UTF-8", req.getCharacterEncoding());
            assertEquals("UTF-8", resp.getCharacterEncoding());
            verify(req,  times(1)).setCharacterEncoding("UTF-8");
            verify(resp, times(1)).setCharacterEncoding("UTF-8");
            verify(chain, times(1)).doFilter(same(req), same(resp));
        }
    }
}