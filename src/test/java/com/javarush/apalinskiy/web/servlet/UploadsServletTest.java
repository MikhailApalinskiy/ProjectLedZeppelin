package com.javarush.apalinskiy.web.servlet;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UploadsServlet")
class UploadsServletTest {

    @TempDir
    Path tmpDir;
    @Mock
    HttpServletRequest req;
    @Mock
    HttpServletResponse resp;
    @Mock
    ServletConfig config;

    UploadsServlet subject;

    private void setBaseDir(Path dir) {
        try {
            subject = new UploadsServlet();
            Field f = UploadsServlet.class.getDeclaredField("baseDir");
            f.setAccessible(true);
            f.set(subject, dir);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static class BAOSServletStream extends ServletOutputStream {
        private final ByteArrayOutputStream baos;

        BAOSServletStream(ByteArrayOutputStream baos) {
            this.baos = baos;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
        }

        @Override
        public void write(int b) {
            baos.write(b);
        }
    }

    @Nested
    @DisplayName("doGet")
    class DoGet {

        @BeforeEach
        void setUp() {
            setBaseDir(tmpDir);
        }

        @Test
        @DisplayName("edge: pathInfo is null -> 404")
        void should_404_When_PathInfoNull() throws Exception {
            // given
            when(req.getPathInfo()).thenReturn(null);
            // when
            subject.doGet(req, resp);
            // then
            verify(resp).sendError(HttpServletResponse.SC_NOT_FOUND);
        }

        @Test
        @DisplayName("edge: pathInfo is blank or '/' -> 404")
        void should_404_When_PathInfoBlankOrSlash() throws Exception {
            // given
            when(req.getPathInfo()).thenReturn("  ");
            // when
            subject.doGet(req, resp);
            // then
            verify(resp).sendError(HttpServletResponse.SC_NOT_FOUND);
            // given (case '/')
            reset(resp);
            when(req.getPathInfo()).thenReturn("/");
            // when
            subject.doGet(req, resp);
            // then
            verify(resp).sendError(HttpServletResponse.SC_NOT_FOUND);
        }

        @Test
        @DisplayName("edge: traversal '../...' -> 403")
        void should_403_When_Traversal() throws Exception {
            // given
            when(req.getPathInfo()).thenReturn("/../secret.txt");
            // when
            subject.doGet(req, resp);
            // then
            verify(resp).sendError(HttpServletResponse.SC_FORBIDDEN);
        }

        @Test
        @DisplayName("edge: file not exists -> 404")
        void should_404_When_FileNotFound() throws Exception {
            // given
            when(req.getPathInfo()).thenReturn("/missing.bin");
            // when
            subject.doGet(req, resp);
            // then
            verify(resp).sendError(HttpServletResponse.SC_NOT_FOUND);
        }

        @Test
        @DisplayName("given regular file with unknown ext -> streams bytes + octet-stream + cache headers + length")
        void should_Stream_With_OctetStream_And_Headers() throws Exception {
            // given
            Path sub = tmpDir.resolve("sub");
            Files.createDirectories(sub);
            Path f = sub.resolve("file.data");
            byte[] content = "hello world".getBytes(StandardCharsets.UTF_8);
            Files.write(f, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            when(req.getPathInfo()).thenReturn("/sub/file.data");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ServletOutputStream sos = new BAOSServletStream(baos);
            when(resp.getOutputStream()).thenReturn(sos);
            // when
            subject.doGet(req, resp);
            // then
            verify(resp).setContentType("application/octet-stream");
            verify(resp).setHeader("Cache-Control", "public, max-age=31536000, immutable");
            verify(resp).setContentLengthLong(Files.size(f));
            assertArrayEquals(content, baos.toByteArray());
        }

        @Test
        @DisplayName("given .png file -> content-type is image/png and body is correct")
        void should_Set_Png_ContentType_And_Stream() throws Exception {
            // given
            Path img = tmpDir.resolve("pic.png");
            byte[] bytes = new byte[]{(byte) 0x89, 'P', 'N', 'G', 0, 1, 2, 3};
            Files.write(img, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            when(req.getPathInfo()).thenReturn("/pic.png");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            when(resp.getOutputStream()).thenReturn(new BAOSServletStream(baos));
            // when
            subject.doGet(req, resp);
            // then
            verify(resp).setContentType("image/png");
            verify(resp).setHeader("Cache-Control", "public, max-age=31536000, immutable");
            verify(resp).setContentLengthLong(bytes.length);
            assertArrayEquals(bytes, baos.toByteArray());
        }
    }

    @Nested
    @DisplayName("init")
    class InitBlock {
        @Test
        @DisplayName("calls resolveBaseDir via init and stores result")
        void should_Init_BaseDir() throws Exception {
            // given
            UploadsServlet s = new UploadsServlet();
            ServletContext sc = mock(ServletContext.class);
            when(config.getServletContext()).thenReturn(sc);
            try (MockedStatic<com.javarush.apalinskiy.web.util.Uploads> up =
                         mockStatic(com.javarush.apalinskiy.web.util.Uploads.class)) {
                up.when(() -> com.javarush.apalinskiy.web.util.Uploads.resolveBaseDir(eq(sc)))
                        .thenReturn(tmpDir);
                // when
                s.init(config);
                // then
                Field f = UploadsServlet.class.getDeclaredField("baseDir");
                f.setAccessible(true);
                assertEquals(tmpDir, f.get(s));
            }
        }
    }
}