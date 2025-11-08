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

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("PublishServlet (unit)")
@ExtendWith(MockitoExtension.class)
class PublishServletTest {

    @Mock
    HttpServletRequest req;
    @Mock
    HttpServletResponse resp;
    @Mock
    HttpSession session;
    @Mock
    QuestAuthoringService authoring;

    private PublishServlet sut;

    @BeforeEach
    void setUp() throws Exception {
        sut = Mockito.spy(new PublishServlet());
        injectField(sut, authoring);
    }

    private static void injectField(Object target, Object value) throws Exception {
        Class<?> c = target.getClass();
        Field f = null;
        while (c != null) {
            try {
                f = c.getDeclaredField("authoring");
                break;
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        assertNotNull(f, "Field '" + "authoring" + "' not found on class hierarchy");
        f.setAccessible(true);
        f.set(target, value);
    }

    private static User user(String id, Role role) {
        User u = new User();
        u.setUserId(id);
        u.setRole(role);
        return u;
    }

    private static CustomQuest quest(String ownerId, String name) {
        CustomQuest q = new CustomQuest();
        q.setId("q1");
        q.setOwnerId(ownerId);
        q.setName(name);
        return q;
    }

    @Nested
    @DisplayName("doGet(req, resp)")
    class DoGet {

        @Test
        @DisplayName("Given validation fails — When doGet — Then redirectErr to GRAPH_SVG with joined errors")
        void validationFails_redirects() throws Exception {
            // Given
            when(authoring.validateCurrentDraft()).thenReturn(List.of("bad", "oops"));
            when(req.getSession(false)).thenReturn(session);
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(user("u1", Role.USER));
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // When
                sut.doGet(req, resp);
                // Then
                web.verify(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.GRAPH_SVG),
                        argThat(msg -> msg.contains("You can't go to the publication")
                                && msg.contains("bad") && msg.contains("oops"))));
                verify(authoring).validateCurrentDraft();
            }
        }

        @Test
        @DisplayName("Given validation ok — When doGet — Then copyParamsToAttrs and forward to JSP.PUBLISH")
        void validationOk_forwards() throws Exception {
            // Given
            when(authoring.validateCurrentDraft()).thenReturn(List.of());
            // When/Then
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.copyParamsToAttrs(eq(req), anyString(), anyString())).thenAnswer(inv -> null);
                sut.doGet(req, resp);
                web.verify(() -> Web.copyParamsToAttrs(eq(req), eq(WebConst.Attr.ERROR), eq(WebConst.Attr.OK)));
                web.verify(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.PUBLISH)));
            }
        }
    }

    @Nested
    @DisplayName("doPost(req, resp)")
    class DoPost {

        @BeforeEach
        void baseSession() {
            when(req.getSession()).thenReturn(session);
            when(req.getSession(false)).thenReturn(session);
        }

        @Test
        @DisplayName("Given editingId present & admin — When doPost — Then updateExisting(true), notify owner if different, redirectOk 'Changes saved'")
        void updateExistingAsAdmin_notifies() throws Exception {
            // Given
            User admin = user("admin", Role.ADMIN);
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(admin);
            when(session.getAttribute(WebConst.Attr.EDITING_QUEST_ID)).thenReturn("q1");
            var existing = quest("owner-42", "QuestName");
            when(authoring.getFromCatalog("q1")).thenReturn(Optional.of(existing));
            doNothing().when(sut).notifyQuestAdminChangedById(eq(admin), eq("owner-42"), eq("QuestName"));
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // When
                sut.doPost(req, resp);
                // Then
                verify(authoring).updateExisting("q1", true);
                verify(sut).notifyQuestAdminChangedById(eq(admin), eq("owner-42"), eq("QuestName"));
                web.verify(() -> Web.redirectOk(eq(req), eq(resp), eq(WebConst.Path.HOME), eq("Changes saved")));
            }
        }

        @Test
        @DisplayName("Given editingId present & admin but owner same — When doPost — Then no owner notify, redirectOk 'Changes saved'")
        void updateExistingAdmin_sameOwner_noNotify() throws Exception {
            // Given
            User admin = user("admin", Role.ADMIN);
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(admin);
            when(session.getAttribute(WebConst.Attr.EDITING_QUEST_ID)).thenReturn("q1");
            var existing = quest("admin", "QuestName");
            when(authoring.getFromCatalog("q1")).thenReturn(Optional.of(existing));
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // When
                sut.doPost(req, resp);
                // Then
                verify(authoring).updateExisting("q1", true);
                verify(sut, never()).notifyQuestAdminChangedById(any(), anyString(), anyString());
                web.verify(() -> Web.redirectOk(eq(req), eq(resp), eq(WebConst.Path.HOME), eq("Changes saved")));
            }
        }

        @Test
        @DisplayName("Given editingId present & non-admin — When doPost — Then updateExisting(false), redirectOk 'Changes submitted for moderation'")
        void updateExistingNonAdmin() throws Exception {
            // Given
            User u = user("u1", Role.USER);
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(u);
            when(session.getAttribute(WebConst.Attr.EDITING_QUEST_ID)).thenReturn("q1");
            when(authoring.getFromCatalog("q1")).thenReturn(Optional.of(quest("u1", "Q")));
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // When
                sut.doPost(req, resp);
                // Then
                verify(authoring).updateExisting("q1", false);
                web.verify(() -> Web.redirectOk(eq(req), eq(resp), eq(WebConst.Path.HOME),
                        eq("Changes submitted for moderation")));
            }
        }

        @Test
        @DisplayName("Given new quest — When name empty — Then redirect back to /publish with error and echo questName")
        void newQuest_emptyName() throws Exception {
            // Given
            User admin = user("a1", Role.ADMIN);
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(admin);
            when(session.getAttribute(WebConst.Attr.EDITING_QUEST_ID)).thenReturn(null);
            when(req.getParameter("questName")).thenReturn("   ");
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // When
                sut.doPost(req, resp);
                // Then
                web.verify(() -> Web.redirect(eq(req), eq(resp), eq(WebConst.Path.PUBLISH),
                        argThat(map -> "Specify the name of the quest".equals(map.get(WebConst.Attr.ERROR))
                                && "   ".equals(map.get("questName")))));
                verifyNoInteractions(authoring);
            }
        }

        @Test
        @DisplayName("Given new quest — When name >100 — Then redirect back to /publish with length error")
        void newQuest_tooLongName() throws Exception {
            User admin = user("a1", Role.ADMIN);
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(admin);
            when(session.getAttribute(WebConst.Attr.EDITING_QUEST_ID)).thenReturn(null);
            String big = "X".repeat(101);
            when(req.getParameter("questName")).thenReturn(big);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // When
                sut.doPost(req, resp);
                // Then
                web.verify(() -> Web.redirect(eq(req), eq(resp), eq(WebConst.Path.PUBLISH),
                        argThat(map -> "The name is too long, maximum length is 50 characters".equals(map.get(WebConst.Attr.ERROR))
                                && big.equals(map.get("questName")))));
                verifyNoInteractions(authoring);
            }
        }

        @Test
        @DisplayName("Given new quest & admin — When valid name — Then publish, incCreatedByUserId, notifyFriends, redirectOk 'published'")
        void publishAsAdmin() throws Exception {
            // Given
            User admin = user("a1", Role.ADMIN);
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(admin);
            when(session.getAttribute(WebConst.Attr.EDITING_QUEST_ID)).thenReturn(null);
            when(req.getParameter("questName")).thenReturn("  My Quest  ");
            doNothing().when(sut).incCreatedByUserId("a1");
            doNothing().when(sut).notifyFriendsPublishedByUserId("a1", "My Quest");
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // When
                sut.doPost(req, resp);
                // Then
                verify(authoring).publish("a1", "My Quest"); // имя с trim()
                verify(sut).incCreatedByUserId("a1");
                verify(sut).notifyFriendsPublishedByUserId("a1", "My Quest");
                web.verify(() -> Web.redirectOk(eq(req), eq(resp), eq(WebConst.Path.HOME),
                        eq("The quest has been published")));
            }
        }

        @Test
        @DisplayName("Given new quest & non-admin — When valid name — Then submitNewForModeration and redirectOk 'submitted for moderation'")
        void submitForModeration() throws Exception {
            // Given
            User user = user("u1", Role.USER);
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(user);
            when(session.getAttribute(WebConst.Attr.EDITING_QUEST_ID)).thenReturn(null);
            when(req.getParameter("questName")).thenReturn("Name");
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // When
                sut.doPost(req, resp);
                // Then
                verify(authoring).submitNewForModeration("u1", "Name");
                web.verify(() -> Web.redirectOk(eq(req), eq(resp), eq(WebConst.Path.HOME),
                        eq("The quest has been submitted for moderation")));
                verify(sut, never()).incCreatedByUserId(anyString());
                verify(sut, never()).notifyFriendsPublishedByUserId(anyString(), anyString());
            }
        }

        @Test
        @DisplayName("Given business exception — When doPost — Then redirect back to /publish with 'Publication failed: ...'")
        void businessException_redirectsBack() throws Exception {
            // Given
            User admin = user("a1", Role.ADMIN);
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(admin);
            when(session.getAttribute(WebConst.Attr.EDITING_QUEST_ID)).thenReturn(null);
            when(req.getParameter("questName")).thenReturn("Demo");
            doThrow(new IllegalStateException("bad state")).when(authoring).publish("a1", "Demo");
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // When
                sut.doPost(req, resp);
                // Then
                web.verify(() -> Web.redirect(eq(req), eq(resp), eq(WebConst.Path.PUBLISH),
                        argThat(map -> String.valueOf(map.get(WebConst.Attr.ERROR)).contains("Publication failed: bad state")
                                && "Demo".equals(map.get("questName")))));
            }
        }
    }
}