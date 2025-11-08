package com.javarush.apalinskiy.app;

import com.javarush.apalinskiy.repository.hibernate.quest.InMemoryQuestStore;
import com.javarush.apalinskiy.repository.notify.NotificationRepository;
import com.javarush.apalinskiy.repository.social.FriendRepository;
import com.javarush.apalinskiy.service.impl.quest.DefaultQuestService;
import com.javarush.apalinskiy.service.notify.NotificationService;
import com.javarush.apalinskiy.service.quest.QuestAuthoringService;
import com.javarush.apalinskiy.service.quest.QuestService;
import com.javarush.apalinskiy.service.social.FriendService;
import com.javarush.apalinskiy.service.user.UserService;
import com.javarush.apalinskiy.service.user.UserStatsService;
import com.javarush.apalinskiy.utils.HibernateUtil;
import com.javarush.apalinskiy.web.util.Uploads;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import org.hibernate.SessionFactory;
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
    @Mock
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
        @DisplayName("should register USER_SERVICE in servlet context")
        void registersUserService() {
            // Given
            try (MockedStatic<InMemoryQuestStore> stores = mockStatic(InMemoryQuestStore.class);
                 MockedConstruction<DefaultQuestService> qsvc = mockConstruction(DefaultQuestService.class);
                 MockedStatic<HibernateUtil> hib = mockStatic(HibernateUtil.class)) {
                stores.when(() -> InMemoryQuestStore.fromClasspath(
                                WebConst.App.QUEST_RESOURCE, WebConst.App.QUEST_START_ID))
                        .thenReturn(mock(InMemoryQuestStore.class));
                stores.when(() -> InMemoryQuestStore.empty(WebConst.App.QUEST_START_ID))
                        .thenReturn(mock(InMemoryQuestStore.class));
                hib.when(HibernateUtil::getSessionFactory)
                        .thenReturn(mock(SessionFactory.class));
                // When
                sut.contextInitialized(evt);
                // Then
                verify(ctx).setAttribute(eq(WebConst.Ctx.USER_SERVICE), any(UserService.class));
            }
        }

        @Test
        @DisplayName("should register PROD_REPOSITORY and QUEST_SERVICE")
        void registersProdAndQuestService() {
            // Given
            InMemoryQuestStore prod = mock(InMemoryQuestStore.class);
            try (MockedStatic<InMemoryQuestStore> stores = mockStatic(InMemoryQuestStore.class);
                 MockedConstruction<DefaultQuestService> qsvc = mockConstruction(DefaultQuestService.class);
                 MockedStatic<HibernateUtil> hib = mockStatic(HibernateUtil.class)) {
                stores.when(() -> InMemoryQuestStore.fromClasspath(
                                WebConst.App.QUEST_RESOURCE, WebConst.App.QUEST_START_ID))
                        .thenReturn(prod);
                stores.when(() -> InMemoryQuestStore.empty(WebConst.App.QUEST_START_ID))
                        .thenReturn(mock(InMemoryQuestStore.class));
                hib.when(HibernateUtil::getSessionFactory)
                        .thenReturn(mock(SessionFactory.class));
                // When
                sut.contextInitialized(evt);
                // Then
                verify(ctx).setAttribute(WebConst.Ctx.PROD_REPOSITORY, prod);
                verify(ctx).setAttribute(eq(WebConst.Ctx.QUEST_SERVICE), any(QuestService.class));
            }
        }

        @Test
        @DisplayName("should register EDITOR_REPOSITORY and AUTHORING_SERVICE (with drafts enabled)")
        void registersEditorAndAuthoring() {
            // Given
            InMemoryQuestStore editor = mock(InMemoryQuestStore.class);
            try (MockedStatic<InMemoryQuestStore> stores = mockStatic(InMemoryQuestStore.class);
                 MockedConstruction<DefaultQuestService> qsvc = mockConstruction(DefaultQuestService.class);
                 MockedStatic<HibernateUtil> hib = mockStatic(HibernateUtil.class)) {

                stores.when(() -> InMemoryQuestStore.fromClasspath(
                                WebConst.App.QUEST_RESOURCE, WebConst.App.QUEST_START_ID))
                        .thenReturn(mock(InMemoryQuestStore.class));
                stores.when(() -> InMemoryQuestStore.empty(WebConst.App.QUEST_START_ID))
                        .thenReturn(editor);
                hib.when(HibernateUtil::getSessionFactory)
                        .thenReturn(mock(SessionFactory.class));

                // When
                sut.contextInitialized(evt);

                // Then
                verify(ctx).setAttribute(WebConst.Ctx.EDITOR_REPOSITORY, editor);
                verify(ctx).setAttribute(eq(WebConst.Ctx.AUTHORING_SERVICE), any(QuestAuthoringService.class));
            }
        }

        @Test
        @DisplayName("should register NOTIFY_*, FRIEND_*, USER_STATS_SERVICE, and SAVE_STATE_SERVICE")
        void registersOtherServices() {
            // Given
            try (MockedStatic<InMemoryQuestStore> stores = mockStatic(InMemoryQuestStore.class);
                 MockedConstruction<DefaultQuestService> qsvc = mockConstruction(DefaultQuestService.class);
                 MockedStatic<HibernateUtil> hib = mockStatic(HibernateUtil.class)) {
                stores.when(() -> InMemoryQuestStore.fromClasspath(
                                WebConst.App.QUEST_RESOURCE, WebConst.App.QUEST_START_ID))
                        .thenReturn(mock(InMemoryQuestStore.class));
                stores.when(() -> InMemoryQuestStore.empty(WebConst.App.QUEST_START_ID))
                        .thenReturn(mock(InMemoryQuestStore.class));
                hib.when(HibernateUtil::getSessionFactory)
                        .thenReturn(mock(SessionFactory.class));
                // When
                sut.contextInitialized(evt);
                // Then
                verify(ctx).setAttribute(eq(WebConst.Ctx.NOTIFY_REPO), any(NotificationRepository.class));
                verify(ctx).setAttribute(eq(WebConst.Ctx.NOTIFY_SERVICE), any(NotificationService.class));
                verify(ctx).setAttribute(eq(WebConst.Ctx.FRIEND_REPOSITORY), any(FriendRepository.class));
                verify(ctx).setAttribute(eq(WebConst.Ctx.FRIEND_SERVICE), any(FriendService.class));
                verify(ctx).setAttribute(eq(WebConst.Ctx.USER_STATS_SERVICE), any(UserStatsService.class));
                verify(ctx).setAttribute(eq(WebConst.Ctx.SAVE_STATE_SERVICE), any());
            }
        }

        @Test
        @DisplayName("should call Uploads.resolveBaseDir(ctx)")
        void callsUploadsResolveBaseDir() {
            // Given
            try (MockedStatic<InMemoryQuestStore> stores = mockStatic(InMemoryQuestStore.class);
                 MockedConstruction<DefaultQuestService> qsvc = mockConstruction(DefaultQuestService.class);
                 MockedStatic<HibernateUtil> hib = mockStatic(HibernateUtil.class);
                 MockedStatic<Uploads> uploads = mockStatic(Uploads.class)) {
                stores.when(() -> InMemoryQuestStore.fromClasspath(
                                WebConst.App.QUEST_RESOURCE, WebConst.App.QUEST_START_ID))
                        .thenReturn(mock(InMemoryQuestStore.class));
                stores.when(() -> InMemoryQuestStore.empty(WebConst.App.QUEST_START_ID))
                        .thenReturn(mock(InMemoryQuestStore.class));
                hib.when(HibernateUtil::getSessionFactory)
                        .thenReturn(mock(SessionFactory.class));
                // When
                sut.contextInitialized(evt);
                // Then
                uploads.verify(() -> Uploads.resolveBaseDir(ctx));
            }
        }

        @Test
        @DisplayName("should wrap IOException from InMemoryQuestStore in RuntimeException")
        void wrapsIOExceptionFromQuestLoad() {
            // Given
            try (MockedStatic<InMemoryQuestStore> stores = mockStatic(InMemoryQuestStore.class);
                 MockedStatic<HibernateUtil> hib = mockStatic(HibernateUtil.class)) {
                stores.when(() -> InMemoryQuestStore.fromClasspath(
                                WebConst.App.QUEST_RESOURCE, WebConst.App.QUEST_START_ID))
                        .thenThrow(new IOException("nofile"));
                hib.when(HibernateUtil::getSessionFactory)
                        .thenReturn(mock(SessionFactory.class));
                // When / Then
                RuntimeException ex = assertThrows(RuntimeException.class, () -> sut.contextInitialized(evt));
                assertTrue(ex.getMessage().contains("Failed to load quest resource"));
            }
        }
    }

    @Nested
    @DisplayName("contextDestroyed")
    class ContextDestroyed {

        @Test
        @DisplayName("should delegate to HibernateUtil.shutdown()")
        void callsHibernateShutdown() {
            // Given
            try (MockedStatic<HibernateUtil> hib = mockStatic(HibernateUtil.class)) {
                // When
                sut.contextDestroyed(evt);
                // Then
                hib.verify(HibernateUtil::shutdown);
            }
        }
    }
}