package com.javarush.apalinskiy.repositories;

import com.javarush.apalinskiy.quest.model.QuestNode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class QuestRepositoryTest {

    private static QuestRepository fromBytesReflect(byte[] bytes) throws Throwable {
        var m = QuestRepository.class.getDeclaredMethod("fromBytes", byte[].class, int.class);
        m.setAccessible(true);
        try {
            return (QuestRepository) m.invoke(null, bytes, 1);
        } catch (InvocationTargetException ite) {
            throw ite.getCause();
        }
    }

    private static String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(data));
        } catch (Exception e) {
            fail("SHA-256 not available");
            return null;
        }
    }

    @Nested
    class FromClasspathFactory {

        @Test
        void missingResourceThrowsIOExceptionTest() {
            // Given
            String name = "no_such_file.json";
            // When / Then
            IOException ex = assertThrows(IOException.class, () -> QuestRepository.fromClasspath(name, 1));
            assertEquals("Resource not found on classpath: " + name, ex.getMessage());
        }

        @Test
        void loadsResourceSuccessfullyAndComputesVersionTest() throws Exception {
            // Given
            String name = "quest_repo_ok.json";
            try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(name)) {
                assertNotNull(in, "test resource missing: " + name);
                byte[] bytes = in.readAllBytes();
                // When
                QuestRepository repo = QuestRepository.fromClasspath(name, 1);
                // Then
                assertEquals(1, repo.getStartId());
                assertEquals("sha256:" + sha256Hex(bytes), repo.version());
                assertEquals(1, repo.start().getId());
                assertEquals(2, repo.get(2).getId());
                assertTrue(repo.choose(1, "go").isPresent());
            }
        }
    }

    @Nested
    class FromBytesFactory {

        @Test
        void buildsRepositoryOkTest() throws Throwable {
            // Given
            byte[] bytes = """
                    [
                      {"id":1,"text":"Start","options":[{"choice":"Go","next":2}],"final":false},
                      {"id":2,"text":"End","final":true}
                    ]
                    """.getBytes(StandardCharsets.UTF_8);
            // When
            QuestRepository repo = fromBytesReflect(bytes);
            // Then
            assertNotNull(repo);
            assertEquals(1, repo.getStartId());
            assertEquals("sha256:" + sha256Hex(bytes), repo.version());
        }

        @Test
        void malformedJsonThrowsIOExceptionTest() {
            // Given
            byte[] bytes = """
                    [
                      {"id":1,"text":"x","final":false}
                    """.getBytes(StandardCharsets.UTF_8);
            // When / Then
            assertThrows(IOException.class, () -> fromBytesReflect(bytes));
        }

        @Test
        void brokenLinkThrowsIllegalStateTest() {
            // Given
            byte[] bytes = """
                    [
                      {"id":1,"text":"Start","final":true},
                      {"id":10,"text":"Has bad link","options":[{"choice":"Open","next":999}],"final":false}
                    ]
                    """.getBytes(StandardCharsets.UTF_8);
            // When / Then
            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> fromBytesReflect(bytes));
            assertEquals("Broken link: #10 -> #999", ex.getMessage());
        }
    }

    @Nested
    class DelegationMethods {

        @Test
        void startGetChooseWorkTest() throws Throwable {
            // Given
            byte[] bytes = """
                    [
                      {"id":1,"text":"Start","options":[{"choice":"Go north","next":2}],"final":false},
                      {"id":2,"text":"End","final":true}
                    ]
                    """.getBytes(StandardCharsets.UTF_8);
            QuestRepository repo = fromBytesReflect(bytes);
            // When
            var start = repo.start();
            var node2 = repo.get(2);
            Optional<QuestNode> next = repo.choose(1, "  GO   NORTH ");
            // Then
            assertEquals(1, start.getId());
            assertNotNull(node2);
            assertEquals(2, node2.getId());
            assertTrue(next.isPresent());
            assertEquals(2, next.get().getId());
        }

        @Test
        void chooseReturnsEmptyForUnknownAnswerTest() throws Throwable {
            // Given
            byte[] bytes = """
                    [
                      {"id":1,"text":"Start","options":[{"choice":"Go","next":2}],"final":false},
                      {"id":2,"text":"End","final":true}
                    ]
                    """.getBytes(StandardCharsets.UTF_8);
            QuestRepository repo = fromBytesReflect(bytes);
            // When
            Optional<QuestNode> next = repo.choose(1, "nope");
            // Then
            assertTrue(next.isEmpty());
        }
    }

    @Nested
    class VersioningBehavior {

        @Test
        void sameBytesSameVersionTest() throws Throwable {
            // Given
            byte[] bytes = """
                    [ {"id":1,"text":"S","final":true} ]
                    """.getBytes(StandardCharsets.UTF_8);
            // When
            QuestRepository r1 = fromBytesReflect(bytes);
            QuestRepository r2 = fromBytesReflect(bytes);
            // Then
            assertEquals(r1.version(), r2.version());
        }

        @Test
        void differentBytesDifferentVersionTest() throws Throwable {
            // Given
            byte[] a = """
                    [ {"id":1,"text":"A","final":true} ]
                    """.getBytes(StandardCharsets.UTF_8);
            byte[] b = """
                    [ {"id":1,"text":"B","final":true} ]
                    """.getBytes(StandardCharsets.UTF_8);
            // When
            String va = fromBytesReflect(a).version();
            String vb = fromBytesReflect(b).version();
            // Then
            assertNotEquals(va, vb);
        }
    }

    @Nested
    class Sha256FallbackPath {

        @Test
        void returnsUnknownWhenMessageDigestUnavailableTest() throws Throwable {
            // Given
            try (MockedStatic<MessageDigest> md = mockStatic(MessageDigest.class)) {
                md.when(() -> MessageDigest.getInstance("SHA-256"))
                        .thenThrow(new NoSuchAlgorithmException("SHA-256"));
                byte[] bytes = """
                        [ {"id":1,"text":"S","final":true} ]
                        """.getBytes(StandardCharsets.UTF_8);
                // When
                QuestRepository repo = fromBytesReflect(bytes);
                // Then
                assertEquals("sha256:unknown", repo.version());
            }
        }
    }

    @Nested
    class PrivateConstructorGuard {

        @Test
        void nullSnapshotThrowsNpeWithMessageTest() {
            // Given
            Constructor<?> ctor = Arrays.stream(QuestRepository.class.getDeclaredConstructors())
                    .filter(c -> c.getParameterCount() == 2 && c.getParameterTypes()[0] == int.class)
                    .findFirst()
                    .orElseThrow();
            ctor.setAccessible(true);
            // When / Then
            NullPointerException ex = assertThrows(NullPointerException.class, () -> {
                try {
                    ctor.newInstance(1, null);
                } catch (InvocationTargetException ite) {
                    throw ite.getCause();
                }
            });
            assertEquals("snapshot", ex.getMessage());
        }
    }
}