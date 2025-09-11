package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.app.WebConst;
import com.javarush.apalinskiy.domain.quest.QuestNode;
import com.javarush.apalinskiy.domain.quest.custom.CustomQuest;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.service.quest.QuestAuthoringService;
import com.javarush.apalinskiy.service.quest.QuestService;
import com.javarush.apalinskiy.service.save.SaveStateService;
import com.javarush.apalinskiy.web.util.Web;
import com.javarush.apalinskiy.web.view.SlotView;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.Mockito.*;

@DisplayName("BaseSaveServlet")
@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class BaseSaveServletTest {

    @Mock
    ServletConfig config;
    @Mock
    ServletContext ctx;
    @Mock
    HttpServletRequest req;
    @Mock
    HttpServletResponse resp;
    @Mock
    HttpSession session;
    @Mock
    QuestService questService;
    @Mock
    SaveStateService saveState;
    @Mock
    QuestAuthoringService authoring;
    @Mock
    QuestNode qn1, qn2;
    @Mock
    CustomQuest customQuest;
    @Mock
    SaveStateService.GlobalSlot slot0;
    @Mock
    SaveStateService.GlobalSlot slot1;

    static class TestServlet extends BaseSaveServlet {
    }

    @Nested
    @DisplayName("init")
    class Init {

        @Test
        @DisplayName("given beans in ctx when init then fields set; authoring via ctx.getAttribute optional")
        void init_ok() throws Exception {
            // given
            BaseSaveServletTest.TestServlet s = new BaseSaveServletTest.TestServlet();
            when(config.getServletContext()).thenReturn(ctx);
            when(ctx.getAttribute(WebConst.Ctx.QUEST_SERVICE)).thenReturn(questService);
            when(ctx.getAttribute(WebConst.Ctx.SAVE_STATE_SERVICE)).thenReturn(saveState);
            when(ctx.getAttribute(WebConst.Ctx.AUTHORING_SERVICE)).thenReturn(authoring);
            // when
            s.init(config);
            // then
            assertSame(questService, s.questService);
            assertSame(saveState, s.saveState);
            assertSame(authoring, s.authoring);
        }

        @Test
        @DisplayName("given missing required bean when init then UnavailableException")
        void init_missing_required() {
            // given
            BaseSaveServletTest.TestServlet s = new BaseSaveServletTest.TestServlet();
            when(config.getServletContext()).thenReturn(ctx);
            when(ctx.getAttribute(WebConst.Ctx.QUEST_SERVICE)).thenReturn(null);
            // when / then
            assertThrows(UnavailableException.class, () -> s.init(config));
        }

        @Test
        @DisplayName("given authoring not in ctx when init then authoring stays null")
        void init_no_authoring_attr() throws Exception {
            // given
            BaseSaveServletTest.TestServlet s = new BaseSaveServletTest.TestServlet();
            when(config.getServletContext()).thenReturn(ctx);
            when(ctx.getAttribute(WebConst.Ctx.QUEST_SERVICE)).thenReturn(questService);
            when(ctx.getAttribute(WebConst.Ctx.SAVE_STATE_SERVICE)).thenReturn(saveState);
            when(ctx.getAttribute(WebConst.Ctx.AUTHORING_SERVICE)).thenReturn(null);
            // when
            s.init(config);
            // then
            assertSame(questService, s.questService);
            assertSame(saveState, s.saveState);
            assertNull(s.authoring);
        }

        @Nested
        @DisplayName("requireAuthOrRedirect")
        class RequireAuth {

            @Test
            @DisplayName("given logged-in user when require then returns user and no redirect")
            void user_present() throws IOException {
                // given
                TestServlet s = new TestServlet();
                User u = mock(User.class);
                when(req.getSession()).thenReturn(session);
                when(session.getAttribute(WebConst.Attr.USER)).thenReturn(u);
                // when
                User got = s.requireAuthOrRedirect(req, resp, WebConst.Path.SAVES);
                // then
                assertSame(u, got);
                verify(resp, never()).sendRedirect(anyString());
            }

            @Test
            @DisplayName("given no user and absolute returnPath when require then redirect to /login?next=<ctx+path>")
            void no_user_absolute_path() throws IOException {
                // given
                TestServlet s = new TestServlet();
                when(req.getSession()).thenReturn(session);
                when(session.getAttribute(WebConst.Attr.USER)).thenReturn(null);
                when(req.getContextPath()).thenReturn("/app");
                when(resp.encodeRedirectURL(anyString())).thenAnswer(a -> a.getArgument(0));
                // when
                User got = s.requireAuthOrRedirect(req, resp, "/saves");
                // then
                assertNull(got);
                ArgumentCaptor<String> cap = ArgumentCaptor.forClass(String.class);
                verify(resp).sendRedirect(cap.capture());
                String url = cap.getValue();
                assertEquals("/app/login?next=%2Fapp%2Fsaves", url);
            }

            @Test
            @DisplayName("given no user and relative returnPath when require then redirect to /login?next=<ctx+\"/\"+path>")
            void no_user_relative_path() throws IOException {
                // given
                TestServlet s = new TestServlet();
                when(req.getSession()).thenReturn(session);
                when(session.getAttribute(WebConst.Attr.USER)).thenReturn(null);
                when(req.getContextPath()).thenReturn("/ctx");
                when(resp.encodeRedirectURL(anyString())).thenAnswer(a -> a.getArgument(0));
                // when
                User got = s.requireAuthOrRedirect(req, resp, "loads");
                // then
                assertNull(got);
                ArgumentCaptor<String> cap = ArgumentCaptor.forClass(String.class);
                verify(resp).sendRedirect(cap.capture());
                assertEquals("/ctx/login?next=%2Fctx%2Floads", cap.getValue());
            }
        }

        @Test
        @DisplayName("given questId & authoring when resolveQuestName then delegates to Web.displayName")
        void resolveQuestName_delegates() {
            // given
            TestServlet s = new TestServlet();
            s.authoring = authoring;
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.displayName("custom", authoring)).thenReturn("Shown Name");
                // when
                String name = s.resolveQuestName("custom");
                // then
                assertEquals("Shown Name", name);
                web.verify(() -> Web.displayName("custom", authoring));
            }
        }

        @Nested
        @DisplayName("titleFor")
        class TitleFor {

            @Test
            @DisplayName("given main questId=null when titleFor then shortTitle(getById.text) or Node #id")
            void main_branch() {
                // given
                TestServlet s = new TestServlet();
                s.questService = questService;
                when(questService.getById(5)).thenReturn(qn1);
                when(qn1.getText()).thenReturn(" Hello ");
                // when
                String t1 = s.titleFor(5, null);
                // then
                assertEquals("Hello", t1);
                // given
                when(questService.getById(6)).thenReturn(null);
                // when
                String t2 = s.titleFor(6, null);
                // then
                assertEquals("Node #6", t2);
            }

            @Test
            @DisplayName("given custom quest with authoring when titleFor then picks node text via catalog")
            void custom_with_authoring() {
                // given
                TestServlet s = new TestServlet();
                s.questService = questService;
                s.authoring = authoring;
                when(authoring.getFromCatalog("qid")).thenReturn(Optional.of(customQuest));
                when(customQuest.getNodes()).thenReturn(List.of(qn1, qn2));
                when(qn1.getId()).thenReturn(41);
                when(qn2.getId()).thenReturn(42);
                when(qn2.getText()).thenReturn("Node 42 text");
                // when
                String t = s.titleFor(42, "qid");
                // then
                assertEquals("Node 42 text", t);
            }

            @Test
            @DisplayName("given custom quest absent or authoring=null when titleFor then 'Node #id'")
            void custom_absent_or_no_authoring() {
                // given
                TestServlet s1 = new TestServlet();
                s1.questService = questService;
                s1.authoring = authoring;
                when(authoring.getFromCatalog("qid")).thenReturn(Optional.empty());
                // when
                String t1 = s1.titleFor(7, "qid");
                // then
                assertEquals("Node #7", t1);
                // given
                TestServlet s2 = new TestServlet();
                s2.questService = questService;
                s2.authoring = null;
                // when
                String t2 = s2.titleFor(8, "qid");
                // then
                assertEquals("Node #8", t2);
            }
        }

        @Nested
        @DisplayName("formatUpdated")
        class FormatUpdated {

            @Test
            @DisplayName("given null ts when formatUpdated then null")
            void null_ts() {
                // given
                TestServlet s = new TestServlet();
                // when / then
                assertNull(s.formatUpdated(null));
            }

            @Test
            @DisplayName("given instant when formatUpdated then dd.MM.yyyy HH:mm in system zone")
            void ok_format() {
                // given
                TestServlet s = new TestServlet();
                Instant ts = Instant.parse("2023-01-02T03:04:05Z");
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                        .withZone(ZoneId.systemDefault());
                String expected = fmt.format(ts);
                // when
                String actual = s.formatUpdated(ts);
                // then
                assertEquals(expected, actual);
            }
        }

        @Nested
        @DisplayName("buildSlotsAll")
        class BuildSlotsAll {

            @Test
            @DisplayName("given mixture of present/empty slots when buildSlotsAll then fills defaults and formats")
            void mix_slots() {
                // given
                TestServlet s = new TestServlet();
                s.saveState = saveState;
                when(slot0.questId()).thenReturn(null);
                when(slot0.questName()).thenReturn("  ");
                when(slot0.title()).thenReturn("");
                when(slot0.nodeId()).thenReturn(11);
                Instant ts0 = Instant.parse("2024-06-01T10:15:30Z");
                when(slot0.updatedAt()).thenReturn(ts0);
                when(slot1.questId()).thenReturn("custom1");
                when(slot1.questName()).thenReturn("My Quest");
                when(slot1.title()).thenReturn("Title 1");
                when(slot1.nodeId()).thenReturn(22);
                Instant ts1 = Instant.parse("2024-07-02T08:00:00Z");
                when(slot1.updatedAt()).thenReturn(ts1);
                when(saveState.getGlobalSlot(eq("U"), anyInt())).thenAnswer(inv -> {
                    int i = inv.getArgument(1, Integer.class);
                    if (i == 0) return Optional.of(slot0);
                    if (i == 1) return Optional.of(slot1);
                    return Optional.empty();
                });
                // when
                List<SlotView> list = s.buildSlotsAll("U");
                // then
                assertEquals(WebConst.SLOT_COUNT, list.size());
                SlotView v0 = list.getFirst();
                assertEquals(0, v0.getIndex());
                assertEquals(Integer.valueOf(11), v0.getNodeId());
                assertEquals("Node #11", v0.getTitle());
                String exp0 = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                        .withZone(ZoneId.systemDefault())
                        .format(ts0);
                assertEquals(exp0, v0.getUpdatedAtText());
                assertEquals("main", v0.getQuestId());
                assertEquals("Main quest", v0.getQuestName());
                SlotView v1 = list.get(1);
                assertEquals(1, v1.getIndex());
                assertEquals(Integer.valueOf(22), v1.getNodeId());
                assertEquals("Title 1", v1.getTitle());
                String exp1 = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                        .withZone(ZoneId.systemDefault())
                        .format(ts1);
                assertEquals(exp1, v1.getUpdatedAtText());
                assertEquals("custom1", v1.getQuestId());
                assertEquals("My Quest", v1.getQuestName());
                SlotView v2 = list.get(2);
                assertEquals(2, v2.getIndex());
                assertNull(v2.getNodeId());
                assertNull(v2.getTitle());
                assertNull(v2.getUpdatedAtText());
                assertEquals("main", v2.getQuestId());
                assertEquals("Main quest", v2.getQuestName());
            }
        }
    }
}