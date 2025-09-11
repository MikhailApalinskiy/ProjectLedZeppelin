package com.javarush.apalinskiy.web.util;

import com.javarush.apalinskiy.app.WebConst;
import jakarta.servlet.ServletContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Uploads.resolveBaseDir")
class UploadsTest {

    @Mock
    ServletContext ctx;
    @TempDir
    Path tmpDir;

    private String prevCatalinaBase;
    private String prevUserDir;
    private String prevTmpDir;

    @BeforeEach
    void saveSysProps() {
        prevCatalinaBase = System.getProperty("catalina.base");
        prevUserDir = System.getProperty("user.dir");
        prevTmpDir = System.getProperty("java.io.tmpdir");
    }

    @AfterEach
    void restoreSysProps() {
        setOrClear("catalina.base", prevCatalinaBase);
        setOrClear("user.dir", prevUserDir);
        setOrClear("java.io.tmpdir", prevTmpDir);
    }

    private static void setOrClear(String key, String val) {
        if (val == null) System.clearProperty(key);
        else System.setProperty(key, val);
    }

    @Nested
    @DisplayName("cached path")
    class CachedPath {

        @Test
        @DisplayName("returns cached Path from context attribute and skips init-param")
        void returnsCached() {
            // Given
            Path cached = tmpDir.resolve("already");
            when(ctx.getAttribute(WebConst.InitParam.CTX_UPLOADS_DIR)).thenReturn(cached);
            // When
            Path got = Uploads.resolveBaseDir(ctx);
            // Then
            assertSame(cached, got);
            verify(ctx).getAttribute(WebConst.InitParam.CTX_UPLOADS_DIR);
            verifyNoMoreInteractions(ctx);
        }
    }

    @Nested
    @DisplayName("from init-param uploads.dir")
    class FromInitParam {

        @Test
        @DisplayName("uses absolute path from init-param, creates directory, caches it")
        void fromInitParam_ok() {
            // Given
            Path desired = tmpDir.resolve("uploads-here");
            when(ctx.getAttribute(WebConst.InitParam.CTX_UPLOADS_DIR)).thenReturn(null);
            when(ctx.getInitParameter("uploads.dir")).thenReturn(desired.toString());
            // When
            Path got = Uploads.resolveBaseDir(ctx);
            // Then
            assertEquals(desired.normalize(), got);
            assertTrue(Files.isDirectory(got));
            verify(ctx).setAttribute(WebConst.InitParam.CTX_UPLOADS_DIR, got);
        }
    }

    @Nested
    @DisplayName("defaults when no explicit dir")
    class Defaults {

        @Test
        @DisplayName("uses catalina.base/uploads (or user.dir/uploads) and caches it")
        void usesCatalinaBaseOrUserDir() {
            // Given
            when(ctx.getAttribute(WebConst.InitParam.CTX_UPLOADS_DIR)).thenReturn(null);
            when(ctx.getInitParameter("uploads.dir")).thenReturn(null);
            System.setProperty("catalina.base", tmpDir.toString());
            // When
            Path got = Uploads.resolveBaseDir(ctx);
            // Then
            assertEquals(tmpDir.resolve("uploads").normalize(), got);
            assertTrue(Files.isDirectory(got));
            verify(ctx).setAttribute(WebConst.InitParam.CTX_UPLOADS_DIR, got);
        }
    }

    @Nested
    @DisplayName("fallback on createDirectories error")
    class Fallback {

        @Test
        @DisplayName("if target is an existing file, falls back to java.io.tmpdir/textquest-uploads")
        void fallsBackToTmpWhenCannotCreate() throws IOException {
            // Given
            Path fileAsBase = Files.createTempFile(tmpDir, "uploads-test", ".tmp");
            when(ctx.getAttribute(WebConst.InitParam.CTX_UPLOADS_DIR)).thenReturn(null);
            when(ctx.getInitParameter("uploads.dir")).thenReturn(fileAsBase.toString());
            Path myTmp = tmpDir.resolve("sys-tmp");
            Files.createDirectories(myTmp);
            System.setProperty("java.io.tmpdir", myTmp.toString());
            // When
            Path got = Uploads.resolveBaseDir(ctx);
            // Then
            assertEquals("textquest-uploads", Objects.requireNonNull(got.getFileName()).toString());
            assertTrue(got.startsWith(myTmp));
            assertTrue(Files.isDirectory(got));
            verify(ctx).setAttribute(WebConst.InitParam.CTX_UPLOADS_DIR, got);
        }
    }
}