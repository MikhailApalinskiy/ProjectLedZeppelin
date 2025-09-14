package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.app.WebConst;
import com.javarush.apalinskiy.domain.notify.NotificationType;
import com.javarush.apalinskiy.domain.quest.custom.CustomQuest;
import com.javarush.apalinskiy.domain.user.Role;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.service.notify.NotificationService;
import com.javarush.apalinskiy.service.quest.QuestAuthoringService;
import com.javarush.apalinskiy.service.user.UserService;
import com.javarush.apalinskiy.web.util.Web;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteQuestServlet")
class DeleteQuestServletTest {

    @Mock
    HttpServletRequest req;
    @Mock
    HttpServletResponse resp;
    @Mock
    HttpSession ses;
    @Mock
    ServletConfig cfg;
    @Mock
    ServletContext ctx;
    @Mock
    QuestAuthoringService authoring;
    @Mock
    NotificationService notifications;
    @Mock
    UserService users;
    @Mock
    CustomQuest quest;
    @Mock
    User actor;

    private static void setField(Object target, String field, Object value) {
        try {
            Field f;
            try {
                f = target.getClass().getDeclaredField(field);
            } catch (NoSuchFieldException ex) {
                f = target.getClass().getSuperclass().getDeclaredField(field);
            }
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("init")
    class Init {

        @Test
        @DisplayName("given beans in context when init then ok")
        @SuppressWarnings("unused")
        void ok() throws Exception {
            // given
            DeleteQuestServlet s = new DeleteQuestServlet();
            when(cfg.getServletContext()).thenReturn(ctx);
            when(ctx.getAttribute(WebConst.Ctx.AUTHORING_SERVICE)).thenReturn(authoring);
            when(ctx.getAttribute(WebConst.Ctx.NOTIFY_SERVICE)).thenReturn(notifications);
            when(ctx.getAttribute(WebConst.Ctx.USER_SERVICE)).thenReturn(users);
            // when
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                s.init(cfg);
            }
            // then
        }

        @Test
        @DisplayName("given no authoring when init then UnavailableException")
        @SuppressWarnings("unused")
        void noAuthoring() {
            // given
            DeleteQuestServlet s = new DeleteQuestServlet();
            when(cfg.getServletContext()).thenReturn(ctx);
            when(ctx.getAttribute(WebConst.Ctx.AUTHORING_SERVICE)).thenReturn(null);
            // when / then
            assertThrows(UnavailableException.class, () -> {
                try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                    s.init(cfg);
                }
            });
        }
    }

    @Nested
    @DisplayName("doGet")
    class DoGet {
        @Test
        @DisplayName("given any when doGet then 405")
        void methodNotAllowed() throws Exception {
            // given
            DeleteQuestServlet s = new DeleteQuestServlet();
            // when
            s.doGet(req, resp);
            // then
            verify(resp).sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }
    }

    @Nested
    @DisplayName("doPost")
    class DoPost {

        @Test
        @DisplayName("given no session user when doPost then redirect to /login")
        void noUser_redirectLogin() throws Exception {
            // given
            DeleteQuestServlet s = new DeleteQuestServlet();
            when(req.getSession()).thenReturn(ses);
            // when
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.redirect(eq(req), eq(resp), eq(WebConst.Path.LOGIN), eq(Map.of())))
                        .then(inv -> null);
                s.doPost(req, resp);
                // then
                web.verify(() -> Web.redirect(eq(req), eq(resp), eq(WebConst.Path.LOGIN), eq(Map.of())));
                verifyNoMoreInteractions(resp);
            }
        }

        @Test
        @DisplayName("given id missing when doPost then set error and redirect to next")
        void idMissing_redirects() throws Exception {
            // given
            DeleteQuestServlet s = new DeleteQuestServlet();
            setField(s, "authoring", authoring);
            when(req.getSession()).thenReturn(ses);
            when(ses.getAttribute(WebConst.Attr.USER)).thenReturn(actor);
            when(req.getParameter(WebConst.Param.ID)).thenReturn(null);
            when(req.getParameter(WebConst.Param.NEXT)).thenReturn("/app/my/quests");
            when(req.getContextPath()).thenReturn("/app");
            when(resp.encodeRedirectURL(anyString())).thenAnswer(a -> a.getArgument(0));
            // when
            s.doPost(req, resp);
            // then
            verify(ses).setAttribute(eq(WebConst.Attr.ERROR), eq("The quest ID is not specified."));
            verify(resp).sendRedirect("/app/my/quests");
        }

        @Test
        @DisplayName("given admin & removed=true & owner!=actor when doPost then flash + notification + redirect")
        void admin_removed_notifies() throws Exception {
            // given
            DeleteQuestServlet s = new DeleteQuestServlet();
            setField(s, "authoring", authoring);
            setField(s, "notifications", notifications);
            when(req.getSession()).thenReturn(ses);
            when(ses.getAttribute(WebConst.Attr.USER)).thenReturn(actor);
            when(req.getParameter(WebConst.Param.ID)).thenReturn("Q1");
            when(req.getParameter(WebConst.Param.NEXT)).thenReturn(null);
            when(req.getContextPath()).thenReturn("/app");
            when(resp.encodeRedirectURL(anyString())).thenAnswer(a -> a.getArgument(0));
            when(actor.getRole()).thenReturn(Role.ADMIN);
            when(actor.getUserId()).thenReturn("admin1");
            when(authoring.getFromCatalog("Q1")).thenReturn(Optional.of(quest));
            when(quest.getName()).thenReturn("QuestName");
            when(quest.getOwnerId()).thenReturn("owner1");
            when(authoring.deleteFromCatalogAsAdmin("Q1")).thenReturn(true);
            // when
            s.doPost(req, resp);
            // then
            verify(ses).setAttribute(eq(WebConst.Attr.FLASH), eq("The quest has been deleted."));
            verify(notifications).add(
                    eq("owner1"),
                    eq(NotificationType.QUEST_ADMIN_CHANGED),
                    eq("The administrator deleted your quest.\n"),
                    contains("Quest <b>QuestName</b> was deleted by the administrator.")
            );
            verify(resp).sendRedirect("/app/");
            verifyNoMoreInteractions(notifications);
        }

        @Test
        @DisplayName("given admin & removed=false when doPost then error set")
        void admin_not_removed_error() throws Exception {
            // given
            DeleteQuestServlet s = new DeleteQuestServlet();
            setField(s, "authoring", authoring);
            setField(s, "notifications", notifications);
            setField(s, "users", users);
            when(req.getSession()).thenReturn(ses);
            when(ses.getAttribute(WebConst.Attr.USER)).thenReturn(actor);
            when(req.getParameter(WebConst.Param.ID)).thenReturn("QX");
            when(req.getParameter(WebConst.Param.NEXT)).thenReturn(null);
            when(req.getContextPath()).thenReturn("/app");
            when(resp.encodeRedirectURL(anyString())).thenAnswer(a -> a.getArgument(0));
            when(actor.getRole()).thenReturn(Role.ADMIN);
            when(authoring.getFromCatalog("QX")).thenReturn(Optional.empty());
            when(authoring.deleteFromCatalogAsAdmin("QX")).thenReturn(false);
            // when
            s.doPost(req, resp);
            // then
            verify(ses).setAttribute(eq(WebConst.Attr.ERROR), eq("Cannot be deleted: not found."));
            verify(resp).sendRedirect("/app/");
        }

        @Test
        @DisplayName("given owner & removed=true when doPost then flash only (no notifications)")
        void owner_removed_flash_only() throws Exception {
            // given
            DeleteQuestServlet s = new DeleteQuestServlet();
            setField(s, "authoring", authoring);
            setField(s, "notifications", notifications);
            setField(s, "users", users);
            when(req.getSession()).thenReturn(ses);
            when(ses.getAttribute(WebConst.Attr.USER)).thenReturn(actor);
            when(req.getParameter(WebConst.Param.ID)).thenReturn("Q2");
            when(req.getParameter(WebConst.Param.NEXT)).thenReturn("/app/");
            when(req.getContextPath()).thenReturn("/app");
            when(resp.encodeRedirectURL(anyString())).thenAnswer(a -> a.getArgument(0));
            when(actor.getRole()).thenReturn(Role.USER);
            when(actor.getUserId()).thenReturn("me");
            when(authoring.getFromCatalog("Q2")).thenReturn(Optional.of(quest));
            when(quest.getName()).thenReturn("Quest");
            when(authoring.deleteFromCatalogIfOwner("Q2", "me")).thenReturn(true);
            // when
            s.doPost(req, resp);
            // then
            verify(ses).setAttribute(eq(WebConst.Attr.FLASH), eq("The quest has been deleted."));
            verifyNoInteractions(notifications);
            verify(resp).sendRedirect("/app/");
        }

        @Test
        @DisplayName("given owner & removed=false when doPost then owner-specific error")
        void owner_not_removed_error() throws Exception {
            // given
            DeleteQuestServlet s = new DeleteQuestServlet();
            setField(s, "authoring", authoring);
            when(req.getSession()).thenReturn(ses);
            when(ses.getAttribute(WebConst.Attr.USER)).thenReturn(actor);
            when(req.getParameter(WebConst.Param.ID)).thenReturn("Q3");
            when(req.getParameter(WebConst.Param.NEXT)).thenReturn(null);
            when(req.getContextPath()).thenReturn("/app");
            when(resp.encodeRedirectURL(anyString())).thenAnswer(a -> a.getArgument(0));
            when(actor.getRole()).thenReturn(Role.USER);
            when(actor.getUserId()).thenReturn("uid-1");
            when(authoring.getFromCatalog("Q3")).thenReturn(Optional.empty());
            when(authoring.deleteFromCatalogIfOwner("Q3", "uid-1")).thenReturn(false);
            // when
            s.doPost(req, resp);
            // then
            verify(ses).setAttribute(eq(WebConst.Attr.ERROR),
                    eq("Cannot be deleted: not found or you are not the owner."));
            verify(resp).sendRedirect("/app/");
        }

        @Test
        @DisplayName("given service throws when doPost then sets 'Deletion error: ...'")
        void service_throws_setsDeletionError() throws Exception {
            // given
            DeleteQuestServlet s = new DeleteQuestServlet();
            setField(s, "authoring", authoring);
            when(req.getSession()).thenReturn(ses);
            when(ses.getAttribute(WebConst.Attr.USER)).thenReturn(actor);
            when(req.getParameter(WebConst.Param.ID)).thenReturn("Q4");
            when(req.getParameter(WebConst.Param.NEXT)).thenReturn(null);
            when(req.getContextPath()).thenReturn("/app");
            when(resp.encodeRedirectURL(anyString())).thenAnswer(a -> a.getArgument(0));
            when(actor.getRole()).thenReturn(Role.ADMIN);
            when(authoring.getFromCatalog("Q4")).thenReturn(Optional.empty());
            when(authoring.deleteFromCatalogAsAdmin("Q4"))
                    .thenThrow(new IllegalStateException("boom"));
            // when
            s.doPost(req, resp);
            // then
            verify(ses).setAttribute(eq(WebConst.Attr.ERROR), eq("Deletion error: boom"));
            verify(resp).sendRedirect("/app/");
        }

        @Test
        @DisplayName("given safeNextOrHome returns null when doPost then fallback to /my/quests")
        void safeNext_null_fallback() throws Exception {
            // given
            DeleteQuestServlet s = new DeleteQuestServlet();
            setField(s, "authoring", authoring);
            when(req.getSession()).thenReturn(ses);
            when(ses.getAttribute(WebConst.Attr.USER)).thenReturn(actor);
            when(req.getParameter(WebConst.Param.ID)).thenReturn(null);
            when(req.getParameter(WebConst.Param.NEXT)).thenReturn("/x");
            when(req.getContextPath()).thenReturn("/app");
            when(resp.encodeRedirectURL(anyString())).thenAnswer(a -> a.getArgument(0));
            // when
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.safeNextOrHome(eq(req), any())).thenReturn(null);
                s.doPost(req, resp);
                // then
                verify(ses).setAttribute(eq(WebConst.Attr.ERROR), anyString());
                verify(resp).sendRedirect("/app" + WebConst.Path.MY_QUESTS);
            }
        }
    }
}