package com.javarush.apalinskiy.web.listener;


import com.javarush.apalinskiy.quest.model.QuestNode;
import com.javarush.apalinskiy.service.DefaultUserService;
import com.javarush.apalinskiy.service.QuestService;
import com.javarush.apalinskiy.service.UserService;
import com.javarush.apalinskiy.user.Role;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppBootstrapTest {

    private static final String QUEST_NAME = "quest.json";
    private static final String QUEST_OK = """
            [
              { "id": 1, "text": "Start", "options": [ {"choice":"Go","next":2} ], "final": false },
              { "id": 2, "text": "End",  "final": true }
            ]
            """;

    static class MemoryResourceCL extends ClassLoader {
        private final Map<String, byte[]> map = new HashMap<>();

        MemoryResourceCL(ClassLoader parent) {
            super(parent);
        }

        MemoryResourceCL with() {
            map.put(AppBootstrapTest.QUEST_NAME, AppBootstrapTest.QUEST_OK.getBytes(StandardCharsets.UTF_8));
            return this;
        }

        @Override
        public InputStream getResourceAsStream(String name) {
            byte[] b = map.get(name);
            if (b != null) return new ByteArrayInputStream(b);
            return super.getResourceAsStream(name);
        }
    }

    static class BlockResourceCL extends ClassLoader {
        private final String blocked;

        BlockResourceCL(ClassLoader parent, String blocked) {
            super(parent);
            this.blocked = blocked;
        }

        @Override
        public InputStream getResourceAsStream(String name) {
            if (name != null && name.equals(blocked)) return null;
            return super.getResourceAsStream(name);
        }
    }

    static class DuplicateLoginException extends RuntimeException {
    }

    @Nested
    class ContextInitSuccess {

        @Test
        void putsUserAndQuestServicesIntoContextTest() {
            // Given
            ServletContext ctx = mock(ServletContext.class);
            ServletContextEvent event = new ServletContextEvent(ctx);
            AppBootstrap bootstrap = new AppBootstrap();
            ClassLoader prev = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(
                    new MemoryResourceCL(prev).with()
            );
            try {
                // When
                bootstrap.contextInitialized(event);
                // Then
                ArgumentCaptor<Object> usCap = ArgumentCaptor.forClass(Object.class);
                ArgumentCaptor<Object> qsCap = ArgumentCaptor.forClass(Object.class);
                verify(ctx).setAttribute(eq(AppBootstrap.ATTR_USER_SERVICE), usCap.capture());
                verify(ctx).setAttribute(eq(AppBootstrap.ATTR_QUEST_SERVICE), qsCap.capture());
                Object userService = usCap.getValue();
                Object questService = qsCap.getValue();
                assertNotNull(userService);
                assertNotNull(questService);
                assertInstanceOf(UserService.class, userService);
                assertInstanceOf(QuestService.class, questService);
                QuestNode start = ((QuestService) questService).getStart();
                assertEquals(1, start.getId());
            } finally {
                Thread.currentThread().setContextClassLoader(prev);
            }
        }
    }

    @Nested
    class ContextInitFailure {

        @Test
        void wrapsExceptionWhenQuestResourceMissingTest() {
            // Given
            ServletContext ctx = mock(ServletContext.class);
            ServletContextEvent event = new ServletContextEvent(ctx);
            AppBootstrap bootstrap = new AppBootstrap();
            ClassLoader prev = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(new BlockResourceCL(prev, QUEST_NAME));
            try {
                // When / Then
                RuntimeException ex = assertThrows(RuntimeException.class,
                        () -> bootstrap.contextInitialized(event));
                assertEquals("Failed to load quest resource: " + QUEST_NAME, ex.getMessage());
                assertNotNull(ex.getCause());
                verify(ctx, never()).setAttribute(eq(AppBootstrap.ATTR_QUEST_SERVICE), any());
            } finally {
                Thread.currentThread().setContextClassLoader(prev);
            }
        }
    }

    @Nested
    class RegisterCatchBranch {

        @Test
        void swallowsDuplicateLoginAndContinues_withoutChangingBootstrap() {
            // Given
            ServletContext ctx = mock(ServletContext.class);
            ServletContextEvent event = new ServletContextEvent(ctx);
            ClassLoader prev = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(
                    new MemoryResourceCL(prev).with()
            );
            try (MockedConstruction<DefaultUserService> cons =
                         mockConstruction(DefaultUserService.class, (mock, c) ->
                                 doThrow(new DuplicateLoginException())
                                         .when(mock).register(any(Role.class), anyString(), anyString(), anyString()))) {
                // When
                new AppBootstrap().contextInitialized(event);
                // Then
                ArgumentCaptor<Object> usCap = ArgumentCaptor.forClass(Object.class);
                ArgumentCaptor<Object> qsCap = ArgumentCaptor.forClass(Object.class);
                verify(ctx).setAttribute(eq(AppBootstrap.ATTR_USER_SERVICE), usCap.capture());
                verify(ctx).setAttribute(eq(AppBootstrap.ATTR_QUEST_SERVICE), qsCap.capture());
                assertSame(cons.constructed().getFirst(), usCap.getValue());
                QuestService qs = (QuestService) qsCap.getValue();
                assertEquals(1, qs.getStart().getId());
            } finally {
                Thread.currentThread().setContextClassLoader(prev);
            }
        }

        @Test
        void rethrowsNonDuplicateRuntime_withoutChangingBootstrap() {
            // Given
            ServletContext ctx = mock(ServletContext.class);
            ServletContextEvent event = new ServletContextEvent(ctx);
            try (MockedConstruction<DefaultUserService> ignored =
                         mockConstruction(DefaultUserService.class, (mock, c) ->
                                 doThrow(new IllegalStateException("boom"))
                                         .when(mock).register(any(Role.class), anyString(), anyString(), anyString()))) {
                // When / Then
                IllegalStateException ex = assertThrows(IllegalStateException.class,
                        () -> new AppBootstrap().contextInitialized(event));
                assertEquals("boom", ex.getMessage());
                verify(ctx, never()).setAttribute(eq(AppBootstrap.ATTR_USER_SERVICE), any());
                verify(ctx, never()).setAttribute(eq(AppBootstrap.ATTR_QUEST_SERVICE), any());
            }
        }
    }
}