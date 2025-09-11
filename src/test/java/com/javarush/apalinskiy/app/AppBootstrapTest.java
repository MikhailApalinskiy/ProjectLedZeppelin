package com.javarush.apalinskiy.app;

import com.javarush.apalinskiy.domain.user.Role;
import com.javarush.apalinskiy.exceptions.DuplicateLoginException;
import com.javarush.apalinskiy.repository.inmemory.quest.InMemoryQuestStore;
import com.javarush.apalinskiy.repository.notify.NotificationRepository;
import com.javarush.apalinskiy.repository.social.FriendRepository;
import com.javarush.apalinskiy.service.impl.quest.DefaultQuestService;
import com.javarush.apalinskiy.service.impl.user.DefaultUserService;
import com.javarush.apalinskiy.service.notify.NotificationService;
import com.javarush.apalinskiy.service.quest.QuestAuthoringService;
import com.javarush.apalinskiy.service.quest.QuestService;
import com.javarush.apalinskiy.service.social.FriendService;
import com.javarush.apalinskiy.service.user.UserService;
import com.javarush.apalinskiy.service.user.UserStatsService;
import com.javarush.apalinskiy.web.util.Uploads;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unused")
@ExtendWith(MockitoExtension.class)
@DisplayName("AppBootstrap")
class AppBootstrapTest {

    AppBootstrap sut;
    @Mock
    ServletContext ctx;
    ServletContextEvent evt;

    @BeforeEach
    void setUp() {
        sut = new AppBootstrap();
        evt = new ServletContextEvent(ctx);
    }

    @Nested
    @DisplayName("contextInitialized")
    class ContextInitialized {

        @Test
        @DisplayName("sets USER_SERVICE")
        void setsUserService() {
            // Given
            try (MockedStatic<InMemoryQuestStore> stores = mockStatic(InMemoryQuestStore.class);
                 MockedConstruction<DefaultQuestService> qsvc = mockConstruction(DefaultQuestService.class)) {
                stores.when(() -> InMemoryQuestStore.fromClasspath(
                                WebConst.App.QUEST_RESOURCE, WebConst.App.QUEST_START_ID))
                        .thenReturn(mock(InMemoryQuestStore.class));
                stores.when(() -> InMemoryQuestStore.empty(WebConst.App.QUEST_START_ID))
                        .thenReturn(mock(InMemoryQuestStore.class));
                // When
                sut.contextInitialized(evt);
                // Then
                verify(ctx).setAttribute(eq(WebConst.Ctx.USER_SERVICE), any(UserService.class));
            }
        }

        @Test
        @DisplayName("sets PROD_REPOSITORY and QUEST_SERVICE")
        void setsProdAndQuestService() {
            // Given
            InMemoryQuestStore prod = mock(InMemoryQuestStore.class);
            try (MockedStatic<InMemoryQuestStore> stores = mockStatic(InMemoryQuestStore.class);
                 MockedConstruction<DefaultQuestService> qsvc = mockConstruction(DefaultQuestService.class)) {
                stores.when(() -> InMemoryQuestStore.fromClasspath(
                                WebConst.App.QUEST_RESOURCE, WebConst.App.QUEST_START_ID))
                        .thenReturn(prod);
                stores.when(() -> InMemoryQuestStore.empty(WebConst.App.QUEST_START_ID))
                        .thenReturn(mock(InMemoryQuestStore.class));
                // When
                sut.contextInitialized(evt);
                // Then
                verify(ctx).setAttribute(WebConst.Ctx.PROD_REPOSITORY, prod);
                verify(ctx).setAttribute(eq(WebConst.Ctx.QUEST_SERVICE), any(QuestService.class));
            }
        }

        @Test
        @DisplayName("sets EDITOR_REPOSITORY and AUTHORING_SERVICE")
        void setsEditorAndAuthoring() {
            // Given
            InMemoryQuestStore editor = mock(InMemoryQuestStore.class);
            try (MockedStatic<InMemoryQuestStore> stores = mockStatic(InMemoryQuestStore.class);
                 MockedConstruction<DefaultQuestService> qsvc = mockConstruction(DefaultQuestService.class)) {
                stores.when(() -> InMemoryQuestStore.fromClasspath(
                                WebConst.App.QUEST_RESOURCE, WebConst.App.QUEST_START_ID))
                        .thenReturn(mock(InMemoryQuestStore.class));
                stores.when(() -> InMemoryQuestStore.empty(WebConst.App.QUEST_START_ID))
                        .thenReturn(editor);
                // When
                sut.contextInitialized(evt);
                // Then
                verify(ctx).setAttribute(WebConst.Ctx.EDITOR_REPOSITORY, editor);
                verify(ctx).setAttribute(eq(WebConst.Ctx.AUTHORING_SERVICE), any(QuestAuthoringService.class));
            }
        }

        @Test
        @DisplayName("sets NOTIFY_* and FRIEND_* and USER_STATS_SERVICE")
        void setsOtherServices() {
            // Given
            try (MockedStatic<InMemoryQuestStore> stores = mockStatic(InMemoryQuestStore.class);
                 MockedConstruction<DefaultQuestService> qsvc = mockConstruction(DefaultQuestService.class)) {
                stores.when(() -> InMemoryQuestStore.fromClasspath(
                                WebConst.App.QUEST_RESOURCE, WebConst.App.QUEST_START_ID))
                        .thenReturn(mock(InMemoryQuestStore.class));
                stores.when(() -> InMemoryQuestStore.empty(WebConst.App.QUEST_START_ID))
                        .thenReturn(mock(InMemoryQuestStore.class));
                // When
                sut.contextInitialized(evt);
                // Then
                verify(ctx).setAttribute(eq(WebConst.Ctx.NOTIFY_REPO), any(NotificationRepository.class));
                verify(ctx).setAttribute(eq(WebConst.Ctx.NOTIFY_SERVICE), any(NotificationService.class));
                verify(ctx).setAttribute(eq(WebConst.Ctx.FRIEND_REPOSITORY), any(FriendRepository.class));
                verify(ctx).setAttribute(eq(WebConst.Ctx.FRIEND_SERVICE), any(FriendService.class));
                verify(ctx).setAttribute(eq(WebConst.Ctx.USER_STATS_SERVICE), any(UserStatsService.class));
            }
        }

        @Test
        @DisplayName("calls Uploads.resolveBaseDir(ctx)")
        void callsUploadsResolveBaseDir() {
            // Given
            try (MockedStatic<InMemoryQuestStore> stores = mockStatic(InMemoryQuestStore.class);
                 MockedConstruction<DefaultQuestService> qsvc = mockConstruction(DefaultQuestService.class);
                 MockedStatic<Uploads> uploads = mockStatic(Uploads.class)) {
                stores.when(() -> InMemoryQuestStore.fromClasspath(
                                WebConst.App.QUEST_RESOURCE, WebConst.App.QUEST_START_ID))
                        .thenReturn(mock(InMemoryQuestStore.class));
                stores.when(() -> InMemoryQuestStore.empty(WebConst.App.QUEST_START_ID))
                        .thenReturn(mock(InMemoryQuestStore.class));
                // When
                sut.contextInitialized(evt);
                // Then
                uploads.verify(() -> Uploads.resolveBaseDir(ctx));
            }
        }

        @Test
        @DisplayName("ignores DuplicateLoginException")
        void ignoresDuplicateLogin() {
            // Given
            try (MockedConstruction<DefaultUserService> usr =
                         mockConstruction(DefaultUserService.class, (mock, c) ->
                                 doThrow(new DuplicateLoginException()).when(mock).register(
                                         eq(Role.ADMIN),
                                         eq(WebConst.App.DEFAULT_ADMIN_NAME),
                                         eq(WebConst.App.DEFAULT_ADMIN_LOGIN),
                                         eq(WebConst.App.DEFAULT_ADMIN_PASS)
                                 )
                         );
                 MockedStatic<InMemoryQuestStore> stores = mockStatic(InMemoryQuestStore.class);
                 MockedConstruction<DefaultQuestService> qsvc = mockConstruction(DefaultQuestService.class)) {
                stores.when(() -> InMemoryQuestStore.fromClasspath(
                                WebConst.App.QUEST_RESOURCE, WebConst.App.QUEST_START_ID))
                        .thenReturn(mock(InMemoryQuestStore.class));
                stores.when(() -> InMemoryQuestStore.empty(WebConst.App.QUEST_START_ID))
                        .thenReturn(mock(InMemoryQuestStore.class));
                // When / Then
                assertDoesNotThrow(() -> sut.contextInitialized(evt));
            }
        }

        @Test
        @DisplayName("rethrows non-duplicate register exception")
        void rethrowsNonDuplicate() {
            // Given
            try (MockedConstruction<DefaultUserService> usr =
                         mockConstruction(DefaultUserService.class, (mock, c) ->
                                 doThrow(new IllegalStateException("boom"))
                                         .when(mock).register(any(), any(), any(), any()))) {
                // When / Then
                IllegalStateException ex =
                        assertThrows(IllegalStateException.class, () -> sut.contextInitialized(evt));
                assertEquals("boom", ex.getMessage());
            }
        }

        @Test
        @DisplayName("wraps IOException from InMemoryQuestStore.fromClasspath")
        void wrapsIOException() {
            // Given
            try (MockedStatic<InMemoryQuestStore> stores = mockStatic(InMemoryQuestStore.class)) {

                stores.when(() -> InMemoryQuestStore.fromClasspath(
                                WebConst.App.QUEST_RESOURCE, WebConst.App.QUEST_START_ID))
                        .thenThrow(new IOException("nofile"));
                // When / Then
                RuntimeException ex =
                        assertThrows(RuntimeException.class, () -> sut.contextInitialized(evt));
                assertTrue(ex.getMessage().contains("Failed to load quest resource"));
            }
        }
    }

    @Nested
    @DisplayName("contextDestroyed")
    class ContextDestroyed {

        @Test
        @DisplayName("no-op")
        void noOp() {
            // Given
            // When / Then
            assertDoesNotThrow(() -> sut.contextDestroyed(evt));
            verifyNoInteractions(ctx);
        }
    }
}