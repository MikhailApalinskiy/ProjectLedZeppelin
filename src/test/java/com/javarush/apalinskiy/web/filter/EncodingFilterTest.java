package com.javarush.apalinskiy.web.filter;

import com.javarush.apalinskiy.app.WebConst;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
@DisplayName("EncodingFilter")
class EncodingFilterTest {

    EncodingFilter sut;

    @Mock
    ServletRequest req;
    @Mock
    ServletResponse resp;
    @Mock
    FilterChain chain;

    @BeforeEach
    void setUp() {
        sut = new EncodingFilter();
    }

    @Nested
    @DisplayName("doFilter")
    class DoFilter {

        @Test
        @DisplayName("sets request/response encoding to UTF-8 and then delegates to chain")
        void setsEncodingAndDelegates() throws IOException, ServletException {
            // When
            assertDoesNotThrow(() -> sut.doFilter(req, resp, chain));
            // Then
            InOrder in = inOrder(req, resp, chain);
            in.verify(req).setCharacterEncoding(eq(WebConst.Charset.UTF8));
            in.verify(resp).setCharacterEncoding(eq(WebConst.Charset.UTF8));
            in.verify(chain).doFilter(eq(req), eq(resp));
        }
    }
}