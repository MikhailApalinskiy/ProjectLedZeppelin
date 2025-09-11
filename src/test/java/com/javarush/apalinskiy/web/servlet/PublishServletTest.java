package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.app.WebConst;
import com.javarush.apalinskiy.domain.quest.custom.CustomQuest;
import com.javarush.apalinskiy.domain.user.Role;
import com.javarush.apalinskiy.domain.user.User;
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
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PublishServlet")
class PublishServletTest {

    @Mock
    QuestAuthoringService authoring;
    @Mock
    HttpServletRequest req;
    @Mock
    HttpServletResponse resp;
    @Mock
    HttpSession session;
    @Mock
    User user;
    @Mock
    CustomQuest cq;

    PublishServlet subject;

    @BeforeEach
    void setUp() throws Exception {
        subject = Mockito.spy(new PublishServlet());
        Field f = BaseQuestAdminServlet.class.getDeclaredField("authoring");
        f.setAccessible(true);
        f.set(subject, authoring);
    }

    private void withSession() {
        when(req.getSession()).thenReturn(session);
        when(req.getSession(false)).thenReturn(session);
    }

    @Nested
    @DisplayName("doGet")
    class DoGet {

        @Test
        @DisplayName("given validation errors when doGet then redirectErr to GRAPH_SVG")
        void should_RedirectErr_When_ValidationFails() throws Exception {
            // given
            when(authoring.validateCurrentDraft()).thenReturn(List.of("err1", "err2"));
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // when
                subject.doGet(req, resp);
                // then
                web.verify(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.GRAPH_SVG),
                        argThat(msg -> msg.startsWith("You can't go to the publication:"))));
                web.verifyNoMoreInteractions();
                verifyNoInteractions(resp);
            }
        }

        @Test
        @DisplayName("given no validation errors when doGet then copyParamsToAttrs + forward to PUBLISH")
        void should_Forward_When_ValidationOk() throws Exception {
            // given
            when(authoring.validateCurrentDraft()).thenReturn(Collections.emptyList());
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // when
                subject.doGet(req, resp);
                // then
                web.verify(() -> Web.copyParamsToAttrs(req, WebConst.Attr.ERROR, WebConst.Attr.OK));
                web.verify(() -> Web.forward(req, resp, WebConst.Jsp.PUBLISH));
                web.verifyNoMoreInteractions();
            }
        }
    }

    @Nested
    @DisplayName("doPost: editing existing quest")
    class DoPostEditing {

        @Test
        @DisplayName("given editingId present & non-blank and not admin when doPost then updateExisting(false) and redirectOk('Changes submitted...')")
        void should_SubmitChanges_For_NonAdmin() throws Exception {
            // given
            withSession();
            when(session.getAttribute(WebConst.Attr.EDITING_QUEST_ID)).thenReturn("Q1");
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(user);
            when(user.getUserLogin()).thenReturn("uLogin");
            when(user.getRole()).thenReturn(Role.USER);
            when(authoring.getFromCatalog("Q1")).thenReturn(Optional.empty());
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // when
                subject.doPost(req, resp);
                // then
                verify(authoring).updateExisting("Q1", false);
                web.verify(() -> Web.redirectOk(req, resp, WebConst.Path.HOME, "Changes submitted for moderation"));
                web.verifyNoMoreInteractions();
                verify(subject, never()).notifyQuestAdminChanged(any(), anyString(), anyString());
            }
        }

        @Test
        @DisplayName("given editingId present & admin changing other owner's quest when doPost then notifyQuestAdminChanged and redirectOk('Changes saved')")
        void should_NotifyOwner_For_AdminChange() throws Exception {
            // given
            withSession();
            when(session.getAttribute(WebConst.Attr.EDITING_QUEST_ID)).thenReturn("Q2");
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(user);
            when(user.getUserLogin()).thenReturn("adminLogin");
            when(user.getRole()).thenReturn(Role.ADMIN);
            when(authoring.getFromCatalog("Q2")).thenReturn(Optional.of(cq));
            when(cq.getName()).thenReturn("QuestName");
            when(cq.getOwnerLogin()).thenReturn("ownerLogin");
            doNothing().when(subject).notifyQuestAdminChanged(eq(user), eq("ownerLogin"), eq("QuestName"));
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // when
                subject.doPost(req, resp);
                // then
                verify(authoring).updateExisting("Q2", true);
                verify(subject).notifyQuestAdminChanged(user, "ownerLogin", "QuestName");
                web.verify(() -> Web.redirectOk(req, resp, WebConst.Path.HOME, "Changes saved"));
                web.verifyNoMoreInteractions();
            }
        }

        @Test
        @DisplayName("given editingId present & admin editing own quest when doPost then no notification and 'Changes saved'")
        void should_NotNotify_When_AdminOwnsQuest() throws Exception {
            // given
            withSession();
            when(session.getAttribute(WebConst.Attr.EDITING_QUEST_ID)).thenReturn("Q3");
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(user);
            when(user.getUserLogin()).thenReturn("adminLogin");
            when(user.getRole()).thenReturn(Role.ADMIN);
            when(authoring.getFromCatalog("Q3")).thenReturn(Optional.of(cq));
            when(cq.getName()).thenReturn("QuestName");
            when(cq.getOwnerLogin()).thenReturn("adminLogin");
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // when
                subject.doPost(req, resp);
                // then
                verify(authoring).updateExisting("Q3", true);
                verify(subject, never()).notifyQuestAdminChanged(any(), anyString(), anyString());
                web.verify(() -> Web.redirectOk(req, resp, WebConst.Path.HOME, "Changes saved"));
                web.verifyNoMoreInteractions();
            }
        }
    }

    @Nested
    @DisplayName("doPost: create new quest")
    class DoPostNew {

        @Test
        @DisplayName("edge: blank name -> redirect back to PUBLISH with error and original questName")
        void should_Redirect_When_NameBlank() throws IOException {
            // given
            withSession();
            when(session.getAttribute(WebConst.Attr.EDITING_QUEST_ID)).thenReturn(null);
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(user);
            when(user.getRole()).thenReturn(Role.USER);
            when(req.getParameter("questName")).thenReturn("   ");
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // when
                subject.doPost(req, resp);
                // then
                web.verify(() -> Web.redirect(eq(req), eq(resp), eq(WebConst.Path.PUBLISH),
                        argThat(map -> "Specify the name of the quest".equals(map.get(WebConst.Attr.ERROR))
                                && "   ".equals(map.get("questName")))));
                web.verifyNoMoreInteractions();
                verifyNoInteractions(authoring);
            }
        }

        @Test
        @DisplayName("edge: too long name (>100) -> redirect back to PUBLISH with specific error")
        void should_Redirect_When_NameTooLong() throws IOException {
            // given
            withSession();
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(user);
            when(user.getRole()).thenReturn(Role.USER);
            String longName = "x".repeat(101);
            when(req.getParameter("questName")).thenReturn(longName);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // when
                subject.doPost(req, resp);
                // then
                web.verify(() -> Web.redirect(eq(req), eq(resp), eq(WebConst.Path.PUBLISH),
                        argThat(map -> "The name is too long (maximum 100 characters)".equals(map.get(WebConst.Attr.ERROR))
                                && longName.equals(map.get("questName")))));
                web.verifyNoMoreInteractions();
                verifyNoInteractions(authoring);
            }
        }

        @Test
        @DisplayName("given admin with valid name when doPost then publish + incCreated + notifyFriends + redirectOk('published')")
        void should_Publish_For_Admin() throws IOException {
            // given
            withSession();
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(user);
            when(user.getUserLogin()).thenReturn("adminLogin");
            when(user.getUserId()).thenReturn("adminId");
            when(user.getRole()).thenReturn(Role.ADMIN);
            when(req.getParameter("questName")).thenReturn("Quest OK");
            doNothing().when(subject).incCreatedByUserId("adminId");
            doNothing().when(subject).notifyFriendsPublishedByUserId("adminId", "Quest OK");
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // when
                subject.doPost(req, resp);
                // then
                verify(authoring).publish("adminLogin", "Quest OK");
                verify(subject).incCreatedByUserId("adminId");
                verify(subject).notifyFriendsPublishedByUserId("adminId", "Quest OK");
                web.verify(() -> Web.redirectOk(req, resp, WebConst.Path.HOME, "The quest has been published"));
                web.verifyNoMoreInteractions();
            }
        }

        @Test
        @DisplayName("given non-admin with valid name when doPost then submitNewForModeration + redirectOk('submitted for moderation')")
        void should_Submit_For_NonAdmin() throws IOException {
            // given
            withSession();
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(user);
            when(user.getUserLogin()).thenReturn("userLogin");
            when(user.getRole()).thenReturn(Role.USER);
            when(req.getParameter("questName")).thenReturn("New Quest");
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // when
                subject.doPost(req, resp);
                // then
                verify(authoring).submitNewForModeration("userLogin", "New Quest");
                web.verify(() -> Web.redirectOk(req, resp, WebConst.Path.HOME, "The quest has been submitted for moderation"));
                web.verifyNoMoreInteractions();
            }
        }

        @Test
        @DisplayName("given no user in session when doPost then owner is 'anonymous' and flow uses role!=ADMIN")
        void should_UseAnonymous_When_NoUser() throws IOException {
            // given
            withSession();
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(null);
            when(req.getParameter("questName")).thenReturn("Anon Quest");
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // when
                subject.doPost(req, resp);
                // then
                verify(authoring).submitNewForModeration("anonymous", "Anon Quest");
                web.verify(() -> Web.redirectOk(req, resp, WebConst.Path.HOME, "The quest has been submitted for moderation"));
                web.verifyNoMoreInteractions();
            }
        }
    }

    @Nested
    @DisplayName("doPost: exception handling")
    class DoPostExceptions {

        @Test
        @DisplayName("given IllegalStateException from authoring when doPost then redirect back with 'Publication failed: ...' and preserve questName")
        void should_RedirectWithError_When_AuthoringThrows() throws IOException {
            // given
            withSession();
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(user);
            when(user.getUserLogin()).thenReturn("userLogin");
            when(user.getRole()).thenReturn(Role.USER);
            when(req.getParameter("questName")).thenReturn("X");
            doThrow(new IllegalStateException("Start node is not set."))
                    .when(authoring).submitNewForModeration("userLogin", "X");
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // when
                subject.doPost(req, resp);
                // then
                web.verify(() -> Web.redirect(eq(req), eq(resp), eq(WebConst.Path.PUBLISH),
                        argThat(map -> {
                            String err = map.get(WebConst.Attr.ERROR);
                            Object name = map.get("questName");
                            return (err != null) && err.startsWith("Publication failed: Start node is not set.")
                                    && "X".equals(name);
                        })));
                web.verifyNoMoreInteractions();
            }
        }
    }
}