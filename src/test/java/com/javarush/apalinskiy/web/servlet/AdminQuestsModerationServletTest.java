package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.app.WebConst;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.repository.quest.CustomQuestRepository;
import com.javarush.apalinskiy.service.notify.NotificationService;
import com.javarush.apalinskiy.service.quest.QuestAuthoringService;
import com.javarush.apalinskiy.web.util.Web;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.util.List;

import static org.mockito.Mockito.*;

@DisplayName("AdminQuestsModerationServlet")
@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class AdminQuestsModerationServletTest {

    @Mock
    HttpServletRequest req;
    @Mock
    HttpServletResponse resp;
    @Mock
    HttpSession session;
    @Mock
    QuestAuthoringService authoring;
    @Mock
    CustomQuestRepository.PendingNew pn;
    @Mock
    CustomQuestRepository.PendingEdit pe;
    @Mock
    NotificationService notify;

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getSuperclass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (NoSuchFieldException e1) {
            try {
                Field f = target.getClass().getDeclaredField(fieldName);
                f.setAccessible(true);
                f.set(target, value);
            } catch (Exception e2) {
                throw new RuntimeException(e2);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private AdminQuestsModerationServlet newServletWithDeps() {
        AdminQuestsModerationServlet s = spy(new AdminQuestsModerationServlet());
        setField(s, "authoring", authoring);
        setField(s, "notify", notify);
        return s;
    }

    @Nested
    @DisplayName("doGet")
    class DoGet {

        @Test
        @DisplayName("given authoring lists when doGet then sets attributes and forwards to moderation jsp")
        void forwardsAndSetsAttrs() throws Exception {
            // given
            AdminQuestsModerationServlet s = newServletWithDeps();
            List<CustomQuestRepository.PendingNew> news = List.of(pn);
            List<CustomQuestRepository.PendingEdit> edits = List.of(pe);
            when(authoring.listPendingNew()).thenReturn(news);
            when(authoring.listPendingEdits()).thenReturn(edits);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.pullFlash(req, WebConst.Attr.FLASH)).then(inv -> null);
                web.when(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.QUESTS_MOD))).then(inv -> null);
                // when
                s.doGet(req, resp);
                // then
                verify(req).setAttribute("pendingNew", news);
                verify(req).setAttribute("pendingEdit", edits);
                web.verify(() -> Web.forward(req, resp, WebConst.Jsp.QUESTS_MOD));
            }
        }
    }

    @Nested
    @DisplayName("doPost validations")
    class DoPostValidation {

        @Test
        @DisplayName("given missing action or id when doPost then 400")
        void missingParams400() throws Exception {
            // given
            AdminQuestsModerationServlet s = newServletWithDeps();
            when(req.getParameter("action")).thenReturn(null);
            when(req.getParameter("id")).thenReturn("x");
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.trimOrNull(null)).thenReturn(null);
                web.when(() -> Web.trimOrNull("x")).thenReturn("x");
                // when
                s.doPost(req, resp);
                // then
                verify(resp).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), eq("Missing parameters"));
            }
        }

        @Test
        @DisplayName("given unknown action when doPost then 400")
        void unknownAction400() throws Exception {
            // given
            AdminQuestsModerationServlet s = newServletWithDeps();
            when(req.getParameter("action")).thenReturn("weird");
            when(req.getParameter("id")).thenReturn("id1");
            when(req.getSession()).thenReturn(session);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.trimOrNull("weird")).thenReturn("weird");
                web.when(() -> Web.trimOrNull("id1")).thenReturn("id1");
                // when
                s.doPost(req, resp);
                // then
                verify(resp).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), eq("Unknown action"));
            }
        }
    }

    @Nested
    @DisplayName("doPost actions")
    class DoPostActions {

        @BeforeEach
        void common() {
            when(req.getSession()).thenReturn(session);
            when(req.getContextPath()).thenReturn("/app");
            when(resp.encodeRedirectURL(anyString())).thenAnswer(a -> a.getArgument(0));
            User admin = mock(User.class);
            when(admin.getUserId()).thenReturn("777");
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(admin);
        }

        @Test
        @DisplayName("approveCreate: publishes, notifies, flashes, redirects")
        void approveCreate_ok() throws Exception {
            // given
            AdminQuestsModerationServlet s = newServletWithDeps();
            doReturn("UID-42").when(s).resolveUserIdByLogin("owner");
            doNothing().when(s).incCreatedByLogin("owner");
            doNothing().when(s).notifyFriendsPublishedByLogin("owner", "QuestName");
            when(req.getParameter("action")).thenReturn("approveCreate");
            when(req.getParameter("id")).thenReturn("p1");
            when(pn.getPendingId()).thenReturn("p1");
            when(pn.getName()).thenReturn("QuestName");
            when(pn.getOwnerLogin()).thenReturn("owner");
            when(authoring.listPendingNew()).thenReturn(List.of(pn));
            when(authoring.approveCreate("p1")).thenReturn("NEW-ID");
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.trimOrNull("approveCreate")).thenReturn("approveCreate");
                web.when(() -> Web.trimOrNull("p1")).thenReturn("p1");
                // when
                s.doPost(req, resp);
                // then
                verify(authoring).approveCreate("p1");
                verify(s).incCreatedByLogin("owner");
                verify(s).notifyFriendsPublishedByLogin("owner", "QuestName");
                verify(session).setAttribute(eq(WebConst.Attr.FLASH),
                        eq("The quest has been published (id=NEW-ID)."));
                verify(resp).sendRedirect("/app" + WebConst.Path.QUESTS_MOD);
                verify(notify, atLeastOnce()).notify(any());
            }
        }

        @Test
        @DisplayName("rejectCreate: rejects, notifies, flashes, redirects")
        void rejectCreate_ok() throws Exception {
            // given
            AdminQuestsModerationServlet s = newServletWithDeps();
            doReturn("UID-77").when(s).resolveUserIdByLogin("own");
            when(req.getParameter("action")).thenReturn("rejectCreate");
            when(req.getParameter("id")).thenReturn("pid");
            when(pn.getPendingId()).thenReturn("pid");
            when(pn.getName()).thenReturn("Q");
            when(pn.getOwnerLogin()).thenReturn("own");
            when(authoring.listPendingNew()).thenReturn(List.of(pn));
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.trimOrNull("rejectCreate")).thenReturn("rejectCreate");
                web.when(() -> Web.trimOrNull("pid")).thenReturn("pid");
                // when
                s.doPost(req, resp);
                // then
                verify(authoring).rejectCreate("pid");
                verify(session).setAttribute(eq(WebConst.Attr.FLASH), eq("New publication rejected."));
                verify(resp).sendRedirect("/app" + WebConst.Path.QUESTS_MOD);
                verify(notify, atLeastOnce()).notify(any());
            }
        }

        @Test
        @DisplayName("approveEdit: approves, notifies, flashes, redirects")
        void approveEdit_ok() throws Exception {
            // given
            AdminQuestsModerationServlet s = newServletWithDeps();
            doReturn("UID-1").when(s).resolveUserIdByLogin("ol");
            when(req.getParameter("action")).thenReturn("approveEdit");
            when(req.getParameter("id")).thenReturn("q1");
            when(pe.getQuestId()).thenReturn("q1");
            when(pe.getName()).thenReturn("EditedQuest");
            when(pe.getOwnerLogin()).thenReturn("ol");
            when(authoring.listPendingEdits()).thenReturn(List.of(pe));
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.trimOrNull("approveEdit")).thenReturn("approveEdit");
                web.when(() -> Web.trimOrNull("q1")).thenReturn("q1");
                // when
                s.doPost(req, resp);
                // then
                verify(authoring).approveEdit("q1");
                verify(session).setAttribute(eq(WebConst.Attr.FLASH), eq("Edits approved and applied."));
                verify(resp).sendRedirect("/app" + WebConst.Path.QUESTS_MOD);
                verify(notify, atLeastOnce()).notify(any());
            }
        }

        @Test
        @DisplayName("rejectEdit: rejects, notifies, flashes, redirects")
        void rejectEdit_ok() throws Exception {
            // given
            AdminQuestsModerationServlet s = newServletWithDeps();
            doReturn("UID-2").when(s).resolveUserIdByLogin("own2");
            when(req.getParameter("action")).thenReturn("rejectEdit");
            when(req.getParameter("id")).thenReturn("q2");
            when(pe.getQuestId()).thenReturn("q2");
            when(pe.getName()).thenReturn("Name2");
            when(pe.getOwnerLogin()).thenReturn("own2");
            when(authoring.listPendingEdits()).thenReturn(List.of(pe));
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.trimOrNull("rejectEdit")).thenReturn("rejectEdit");
                web.when(() -> Web.trimOrNull("q2")).thenReturn("q2");
                // when
                s.doPost(req, resp);
                // then
                verify(authoring).rejectEdit("q2");
                verify(session).setAttribute(eq(WebConst.Attr.FLASH), eq("The edits were rejected."));
                verify(resp).sendRedirect("/app" + WebConst.Path.QUESTS_MOD);
                verify(notify, atLeastOnce()).notify(any());
            }
        }
    }

    @Nested
    @DisplayName("doPost exception")
    class DoPostException {

        @Test
        @DisplayName("given runtime exception when processing then Web.redirect called with error")
        void redirectsWithErrorOnException() throws Exception {
            // given
            AdminQuestsModerationServlet s = newServletWithDeps();
            when(req.getSession()).thenReturn(session);
            when(req.getParameter("action")).thenReturn("approveCreate");
            when(req.getParameter("id")).thenReturn("boom");
            when(pn.getPendingId()).thenReturn("boom");
            when(authoring.listPendingNew()).thenReturn(List.of(pn));
            when(authoring.approveCreate("boom")).thenThrow(new RuntimeException("fail"));
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.trimOrNull("approveCreate")).thenReturn("approveCreate");
                web.when(() -> Web.trimOrNull("boom")).thenReturn("boom");
                web.when(() -> Web.redirect(eq(req), eq(resp), eq(WebConst.Path.QUESTS_MOD), anyMap()))
                        .then(inv -> null);
                // when
                s.doPost(req, resp);
                // then
                web.verify(() -> Web.redirect(eq(req), eq(resp), eq(WebConst.Path.QUESTS_MOD),
                        argThat(map -> "fail".equals(map.get(WebConst.Attr.ERROR)))));
                verify(resp, never()).sendRedirect(anyString());
            }
        }
    }
}