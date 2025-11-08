package com.javarush.apalinskiy.web.util;

import com.javarush.apalinskiy.app.WebConst;
import com.javarush.apalinskiy.domain.quest.Option;
import com.javarush.apalinskiy.domain.quest.QuestNode;
import com.javarush.apalinskiy.domain.quest.custom.CustomQuest;
import com.javarush.apalinskiy.domain.user.Role;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.service.quest.QuestAuthoringService;
import com.javarush.apalinskiy.service.user.UserService;
import com.javarush.apalinskiy.web.view.EdgeSeg;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Web")
@SuppressWarnings({"ALL"})
class WebTest {

    @Mock
    HttpServletRequest req;
    @Mock
    HttpServletResponse resp;
    @Mock
    HttpSession session;
    @Mock
    RequestDispatcher dispatcher;
    @Mock
    ServletContext ctx;
    @Mock
    QuestAuthoringService authoring;
    @Mock
    CustomQuest customQuest;
    @Mock
    QuestNode nodeA;
    @Mock
    QuestNode nodeB;
    @Mock
    Option optAB;
    @Mock
    User userBefore;
    @Mock
    User userAfter;
    @Mock
    UserService userService;

    @Nested
    class TrimOrNull {
        @Test
        @DisplayName("given null when trimOrNull then return null")
        void nullInput() {
            // given
            String s = null;
            // when
            String r = Web.trimOrNull(s);
            // then
            assertNull(r);
        }

        @Test
        @DisplayName("given blank when trimOrNull then return null")
        void blankInput() {
            // given
            String s = "  \t \n ";
            // when
            String r = Web.trimOrNull(s);
            // then
            assertNull(r);
        }

        @Test
        @DisplayName("given spaced text when trimOrNull then trimmed")
        void normal() {
            // given
            String s = "  hello  ";
            // when
            String r = Web.trimOrNull(s);
            // then
            assertEquals("hello", r);
        }
    }

    @Nested
    class ParseIntOrNull {
        @Test
        @DisplayName("given null/blank when parseIntOrNull then null")
        void nullOrBlank() {
            // given
            // when
            Integer a = Web.parseIntOrNull(null);
            Integer b = Web.parseIntOrNull("   ");
            // then
            assertNull(a);
            assertNull(b);
        }

        @Test
        @DisplayName("given numeric string when parseIntOrNull then Integer")
        void valid() {
            // given
            String s = " 123 ";
            // when
            Integer r = Web.parseIntOrNull(s);
            // then
            assertEquals(123, r);
        }

        @Test
        @DisplayName("given non-numeric when parseIntOrNull then null")
        void invalid() {
            // given
            String s = "12a3";
            // when
            Integer r = Web.parseIntOrNull(s);
            // then
            assertNull(r);
        }
    }

    @Nested
    class ParseIntOrDefault {
        @Test
        @DisplayName("given valid when parseIntOrDefault then parsed")
        void valid() {
            // given
            String s = "7";
            int def = 42;
            // when
            int r = Web.parseIntOrDefault(s, def);
            // then
            assertEquals(7, r);
        }

        @Test
        @DisplayName("given invalid when parseIntOrDefault then default")
        void invalid() {
            // given
            String s = "x";
            int def = 42;
            // when
            int r = Web.parseIntOrDefault(s, def);
            // then
            assertEquals(42, r);
        }
    }

    @Nested
    class FirstIntParam {
        @Test
        @DisplayName("given several names when firstIntParam then first parsed")
        void picksFirst() {
            // given
            when(req.getParameter("a")).thenReturn(null);
            when(req.getParameter("b")).thenReturn(" 17 ");
            // when
            Integer r = Web.firstIntParam(req, "a", "b", "c");
            // then
            assertEquals(17, r);
        }

        @Test
        @DisplayName("given all invalid when firstIntParam then null")
        void none() {
            // given
            when(req.getParameter("a")).thenReturn("x");
            when(req.getParameter("b")).thenReturn(null);
            // when
            Integer r = Web.firstIntParam(req, "a", "b");
            // then
            assertNull(r);
        }
    }

    @Nested
    class UrlEncode {
        @Test
        @DisplayName("given null when urlEncode then encode empty")
        void nullValue() {
            // given
            String v = null;
            // when
            String r = Web.urlEncode(v);
            // then
            assertEquals(URLEncoder.encode("", StandardCharsets.UTF_8), r);
        }

        @Test
        @DisplayName("given spaces when urlEncode then plus-separated")
        void spaces() {
            // given
            String v = "hello world";
            // when
            String r = Web.urlEncode(v);
            // then
            assertEquals("hello+world", r);
        }
    }

    @Nested
    class AddParamsEncoded {
        @Test
        @DisplayName("given null/empty params when addParamsEncoded then base")
        void noParams() {
            // given
            // when
            String r1 = Web.addParamsEncoded("/x", null);
            String r2 = Web.addParamsEncoded("/x", Map.of());
            // then
            assertEquals("/x", r1);
            assertEquals("/x", r2);
        }

        @Test
        @DisplayName("given base without query when addParamsEncoded then '?' then pairs")
        void addQuery() {
            // given
            Map<String, String> p = new LinkedHashMap<>();
            p.put("a b", "c d");
            p.put("e", "1+2");
            // when
            String r = Web.addParamsEncoded("/x", p);
            // then
            assertTrue(r.startsWith("/x?"));
            assertTrue(r.contains("a+b=c+d"));
            assertTrue(r.contains("e=1%2B2"));
        }

        @Test
        @DisplayName("given base with query when addParamsEncoded then '&'")
        void appendQuery() {
            // given
            Map<String, String> p = Map.of("k", "v");
            // when
            String r = Web.addParamsEncoded("/x?z=1", p);
            // then
            assertEquals("/x?z=1&k=v", r);
        }
    }

    @Nested
    class AddParamsFromReqEncoded {
        @Test
        @DisplayName("given non-blank params in req when addParamsFromReqEncoded then propagate")
        void propagate() {
            // given
            when(req.getParameter("q")).thenReturn(" hello world ");
            when(req.getParameter("empty")).thenReturn("   ");
            when(req.getParameter("miss")).thenReturn(null);
            // when
            String r = Web.addParamsFromReqEncoded(req, "/s", "q", "empty", "miss");
            // then
            assertEquals("/s?q=+hello+world+", r);
        }
    }

    @Nested
    class QuestUrls {
        @Test
        @DisplayName("given null/blank next when buildQuestUrlFromNext then fallback to Path.QUEST")
        void fallback() {
            // given
            when(req.getParameter(WebConst.Param.CUSTOM)).thenReturn(null);
            when(req.getContextPath()).thenReturn("/app");
            // when
            String r1 = Web.buildQuestUrlFromNext(req, null, 5);
            String r2 = Web.buildQuestUrlFromNext(req, "   ", 5);
            // then
            assertTrue(r1.startsWith("/app" + WebConst.Path.QUEST));
            assertTrue(r2.startsWith("/app" + WebConst.Path.QUEST));
            assertTrue(r1.contains(WebConst.Param.ID + "=5"));
        }

        @Test
        @DisplayName("given custom=main when buildQuestUrlFromNext then omit custom param")
        void mainOmitted() {
            // given
            when(req.getParameter(WebConst.Param.CUSTOM)).thenReturn("MaIn");
            // when
            String r = Web.buildQuestUrlFromNext(req, "/app/next", 77);
            // then
            assertFalse(r.contains(WebConst.Param.CUSTOM + "="));
            assertTrue(r.contains(WebConst.Param.ID + "=77"));
        }

        @Test
        @DisplayName("given custom via attribute when buildQuestUrlFromNext then use it")
        void attributeCustom() {
            // given
            when(req.getParameter(WebConst.Param.CUSTOM)).thenReturn(null);
            when(req.getAttribute(WebConst.Param.CUSTOM)).thenReturn("abc");
            // when
            String r = Web.buildQuestUrlFromNext(req, "/app/next", 1);
            // then
            assertTrue(r.contains("custom=abc"));
        }

        @Test
        @DisplayName("questUrl: base is /app/quest + id [+ custom]")
        void questUrl() {
            // given
            when(req.getContextPath()).thenReturn("/app");
            // when
            String r = Web.questUrl(req, 3, "xyz");
            // then
            assertTrue(r.startsWith("/app" + WebConst.Path.QUEST));
            assertTrue(r.contains("id=3"));
            assertTrue(r.contains("custom=xyz"));
        }
    }

    @Nested
    class Forward {
        @Test
        @DisplayName("given dispatcher when forward then RequestDispatcher.forward called")
        void forwards() throws Exception {
            // given
            when(req.getRequestDispatcher("/WEB-INF/p.jsp")).thenReturn(dispatcher);
            // when
            Web.forward(req, resp, "/WEB-INF/p.jsp");
            // then
            verify(dispatcher).forward(req, resp);
        }
    }

    @Nested
    class SafeNext {
        @Test
        @DisplayName("given null/blank when isSafeNext then false")
        void nullBlank() {
            // given
            // when
            boolean a = Web.isSafeNext(req, null);
            boolean b = Web.isSafeNext(req, " ");
            // then
            assertFalse(a);
            assertFalse(b);
        }

        @Test
        @DisplayName("given path starting with ctx/ when isSafeNext then true else false")
        void check() {
            // given
            when(req.getContextPath()).thenReturn("/app");
            // when
            boolean ok = Web.isSafeNext(req, "/app/ok");
            boolean bad1 = Web.isSafeNext(req, "/other/ok");
            boolean bad2 = Web.isSafeNext(req, "http://evil/app/ok");
            // then
            assertTrue(ok);
            assertFalse(bad1);
            assertFalse(bad2);
        }

        @Test
        @DisplayName("given unsafe next when safeNextOrHome then ctx+HOME")
        void safeOrHome() {
            // given
            when(req.getContextPath()).thenReturn("/app");
            // when
            String ok = Web.safeNextOrHome(req, "/app/abc");
            String bad = Web.safeNextOrHome(req, "http://x");
            // then
            assertEquals("/app/abc", ok);
            assertEquals("/app" + WebConst.Path.HOME, bad);
        }
    }

    @Nested
    class Redirects {
        @Test
        @DisplayName("given params when redirect then encoded URL and sendRedirect")
        void redirect() throws IOException {
            // given
            when(req.getContextPath()).thenReturn("/app");
            when(resp.encodeRedirectURL(anyString())).thenAnswer(a -> a.getArgument(0));
            Map<String, String> p = Map.of("a", "b c");
            // when
            Web.redirect(req, resp, WebConst.Path.HOME, p);
            // then
            ArgumentCaptor<String> cap = ArgumentCaptor.forClass(String.class);
            verify(resp).sendRedirect(cap.capture());
            String url = cap.getValue();
            assertTrue(url.startsWith("/app" + WebConst.Path.HOME));
            assertTrue(url.contains("a=b+c"));
        }

        @Test
        @DisplayName("given msg when redirectOk then puts OK param")
        void redirectOk() throws IOException {
            // given
            when(resp.encodeRedirectURL(anyString())).thenAnswer(a -> a.getArgument(0));
            // when
            Web.redirectOk(req, resp, WebConst.Path.HOME, "done");
            // then
            verify(resp).sendRedirect(contains(WebConst.Attr.OK + "=done"));
        }

        @Test
        @DisplayName("given msg when redirectErr then puts ERROR param")
        void redirectErr() throws IOException {
            // given
            when(resp.encodeRedirectURL(anyString())).thenAnswer(a -> a.getArgument(0));
            // when
            Web.redirectErr(req, resp, WebConst.Path.HOME, "");
            // then
            verify(resp).sendRedirect(contains(WebConst.Attr.ERROR + "="));
        }
    }

    @Nested
    class RenewSessionAndPut {
        @Test
        @DisplayName("given existing session when renewSessionAndPut then invalidate and set in fresh")
        void renews() {
            // given
            when(req.getSession(false)).thenReturn(session);
            HttpSession fresh = mock(HttpSession.class);
            when(req.getSession(true)).thenReturn(fresh);
            // when
            Web.renewSessionAndPut(req, "k", 123);
            // then
            verify(session).invalidate();
            verify(fresh).setAttribute("k", 123);
        }

        @Test
        @DisplayName("given no session when renewSessionAndPut then create and set")
        void creates() {
            // given
            when(req.getSession(false)).thenReturn(null);
            HttpSession fresh = mock(HttpSession.class);
            when(req.getSession(true)).thenReturn(fresh);
            // when
            Web.renewSessionAndPut(req, "k", "v");
            // then
            verify(fresh).setAttribute("k", "v");
        }
    }

    @Nested
    class PullFlash {
        @Test
        @DisplayName("given flash in session when pullFlash then move to req and remove")
        void moves() {
            // given
            when(req.getSession(false)).thenReturn(session);
            when(session.getAttribute("f")).thenReturn("hello");
            // when
            Web.pullFlash(req, "f");
            // then
            verify(req).setAttribute("f", "hello");
            verify(session).removeAttribute("f");
        }

        @Test
        @DisplayName("given no session/flash when pullFlash then no-op")
        void noop() {
            // given
            when(req.getSession(false)).thenReturn(null);
            // when
            Web.pullFlash(req, "f");
            // then
            verify(req, never()).setAttribute(eq("f"), any());

            // given
            when(req.getSession(false)).thenReturn(session);
            when(session.getAttribute("f")).thenReturn(null);
            // when
            Web.pullFlash(req, "f");
            // then
            verify(req, never()).setAttribute(eq("f"), any());
        }
    }

    @Nested
    class DisplayNameCases {
        @Test
        @DisplayName("given null/blank or 'main' when displayName then 'main'")
        void main() {
            // given
            // when
            String a = Web.displayName(null, authoring);
            String b = Web.displayName("  ", authoring);
            String c = Web.displayName("MAIN", authoring);
            // then
            assertEquals("main", a);
            assertEquals("main", b);
            assertEquals("main", c);
        }

        @Test
        @DisplayName("given authoring null when displayName then 'Custom quest'")
        void noAuthoring() {
            // given
            // when
            String r = Web.displayName("x", null);
            // then
            assertEquals("Custom quest", r);
        }

        @Test
        @DisplayName("given catalog hit with name when displayName then that name")
        void withName() {
            // given
            when(authoring.getFromCatalog("q")).thenReturn(Optional.of(customQuest));
            when(customQuest.getName()).thenReturn("QuestName");
            // when
            String r = Web.displayName("q", authoring);
            // then
            assertEquals("QuestName", r);
        }

        @Test
        @DisplayName("given catalog hit with blank name when displayName then 'Custom quest'")
        void blankName() {
            // given
            when(authoring.getFromCatalog("q")).thenReturn(Optional.of(customQuest));
            when(customQuest.getName()).thenReturn("  ");
            // when
            String r = Web.displayName("q", authoring);
            // then
            assertEquals("Custom quest", r);
        }

        @Test
        @DisplayName("given catalog miss when displayName then 'Custom quest'")
        void miss() {
            // given
            when(authoring.getFromCatalog("q")).thenReturn(Optional.empty());
            // when
            String r = Web.displayName("q", authoring);
            // then
            assertEquals("Custom quest", r);
        }
    }

    @Nested
    class AttachQuestLists {
        @Test
        @DisplayName("given items when attachQuestLists then items + created/updated maps")
        void attach() {
            // given
            CustomQuest a = mock(CustomQuest.class);
            CustomQuest b = mock(CustomQuest.class);
            when(a.getId()).thenReturn("A");
            when(b.getId()).thenReturn("B");
            when(a.getCreatedAt()).thenReturn(Instant.parse("2024-01-01T00:00:00Z"));
            when(b.getUpdatedAt()).thenReturn(Instant.parse("2024-02-02T00:00:00Z"));
            List<CustomQuest> items = List.of(a, b);
            // when
            Web.attachQuestLists(req, items);
            // then
            verify(req).setAttribute("items", items);
            ArgumentCaptor<Object> v = ArgumentCaptor.forClass(Object.class);
            verify(req, atLeast(1)).setAttribute(eq("createdMap"), v.capture());
            @SuppressWarnings("unchecked")
            Map<String, Date> created = (Map<String, Date>) v.getValue();
            assertTrue(created.containsKey("A"));
        }
    }

    @Nested
    class CtxBean {
        static class Foo {
        }

        static class Bar {
        }

        @Test
        @DisplayName("given matching type when ctxBean then cast returned")
        void ok() {
            // given
            Foo foo = new Foo();
            when(ctx.getAttribute("k")).thenReturn(foo);
            // when
            Foo r = Web.ctxBean(ctx, "k", Foo.class);
            // then
            assertSame(foo, r);
        }

        @Test
        @DisplayName("given wrong type when ctxBean then throws")
        void wrongType() {
            // given
            when(ctx.getAttribute("k")).thenReturn(new Bar());
            // when / then
            assertThrows(IllegalStateException.class, () -> Web.ctxBean(ctx, "k", Foo.class));
        }
    }

    @Nested
    class CopyParamsToAttrs {
        @Test
        @DisplayName("given non-blank params when copyParamsToAttrs then set")
        void copy() {
            // given
            when(req.getParameter("a")).thenReturn(" 1 ");
            when(req.getParameter("b")).thenReturn("   ");
            when(req.getParameter("c")).thenReturn(null);
            // when
            Web.copyParamsToAttrs(req, "a", "b", "c");
            // then
            verify(req).setAttribute("a", " 1 ");
            verify(req, never()).setAttribute(eq("b"), any());
            verify(req, never()).setAttribute(eq("c"), any());
        }
    }

    @Nested
    class ShortTitle {
        @Test
        @DisplayName("given null when shortTitle then empty")
        void nullInput() {
            // given
            String s = null;
            // when
            String r = Web.shortTitle(s);
            // then
            assertEquals("", r);
        }

        @Test
        @DisplayName("given <=64 chars when shortTitle then trim only")
        void shortEnough() {
            // given
            String s = "  hello  ";
            // when
            String r = Web.shortTitle(s);
            // then
            assertEquals("hello", r);
        }

        @Test
        @DisplayName("given dot in 20..80 when shortTitle then cut at dot+1")
        void dotCut() {
            // given
            String s = "x".repeat(25) + ". " + "y".repeat(100);
            // when
            String r = Web.shortTitle(s);
            // then
            assertTrue(r.endsWith("."), "should cut at dot+1 when length > 64 and dot in range");
        }

        @Test
        @DisplayName("given long no dot when shortTitle then 64 + ellipsis")
        void truncate() {
            // given
            String s = "x".repeat(100);
            // when
            String r = Web.shortTitle(s);
            // then
            assertEquals(65, r.length());
            assertTrue(r.endsWith("…"));
        }
    }

    @Nested
    class IsEffectivelyEmpty {
        @Test
        @DisplayName("given null/empty when isEffectivelyEmpty then true")
        void nullEmpty() {
            // given
            // when
            boolean a = Web.isEffectivelyEmpty(null);
            boolean b = Web.isEffectivelyEmpty(List.of());
            // then
            assertTrue(a);
            assertTrue(b);
        }

        @Test
        @DisplayName("given single final blank node when isEffectivelyEmpty then true")
        void singleFinalBlank() {
            // given
            QuestNode n = mock(QuestNode.class);
            when(n.getFin()).thenReturn(true);
            when(n.getText()).thenReturn("   ");
            assertTrue(Web.isEffectivelyEmpty(List.of(n)));
            // when
            boolean r = Web.isEffectivelyEmpty(List.of(n));
            // then
            assertTrue(r);
        }

        @Test
        @DisplayName("given non-final/with text when isEffectivelyEmpty then false")
        void notEmpty() {
            // given
            QuestNode n = mock(QuestNode.class);
            when(n.getFin()).thenReturn(false);
            when(n.getText()).thenReturn(null);
            assertFalse(Web.isEffectivelyEmpty(List.of(n)));
            // when
            boolean r = Web.isEffectivelyEmpty(List.of(n));
            // then
            assertFalse(r);
        }
    }

    @Nested
    class Wrapping {
        @Test
        @DisplayName("given long words when wrapByWords then <=3 lines width<=24 truncating")
        void wrapByWords() {
            // given
            String s = "averyveryveryverylongword word2 word3 word4";
            // when
            List<String> lines = Web.wrapByWords(s);
            // then
            assertTrue(lines.size() >= 1 && lines.size() <= 3);
            assertTrue(lines.get(0).length() <= 24);
        }

        @Test
        @DisplayName("given null/blank when makeSnippet then empty")
        void snippetEmpty() {
            // given
            // when
            String a = Web.makeSnippet(null);
            String b = Web.makeSnippet("  \n ");
            // then
            assertEquals("", a);
            assertEquals("", b);
        }

        @Test
        @DisplayName("given long text when makeSnippet then <=120 and wrapped")
        void snippetNormal() {
            // given
            String s = ("word ".repeat(60)).replace(" ", "\n");
            // when
            String snippet = Web.makeSnippet(s);
            // then
            assertTrue(snippet.length() <= 120 + 5);
            assertTrue(snippet.contains("\n"));
        }
    }

    @Nested
    class CustomHelpers {
        @Test
        @DisplayName("normalizeCustom: null/blank/main -> null, else same")
        void normalize() {
            // given
            // when
            String a = Web.normalizeCustom(null);
            String b = Web.normalizeCustom("   ");
            String c = Web.normalizeCustom("MAIN");
            String d = Web.normalizeCustom("abc");
            // then
            assertNull(a);
            assertNull(b);
            assertNull(c);
            assertEquals("abc", d);
        }

        @Test
        @DisplayName("normalizedCustomParam reads from req and normalizes")
        void normalizedParam() {
            // given
            when(req.getParameter(WebConst.Param.CUSTOM)).thenReturn("main");
            // when
            String a = Web.normalizedCustomParam(req);
            // then
            assertNull(a);
            // given
            when(req.getParameter(WebConst.Param.CUSTOM)).thenReturn("x");
            // when
            String b = Web.normalizedCustomParam(req);
            // then
            assertEquals("x", b);
        }

        @Test
        @DisplayName("isMissingCustomId rules")
        void missing() {
            // given
            // when / then
            assertFalse(Web.isMissingCustomId(null, authoring));
            assertTrue(Web.isMissingCustomId("x", null));
            // given
            when(authoring.getFromCatalog("x")).thenReturn(Optional.empty());
            // when / then
            assertTrue(Web.isMissingCustomId("x", authoring));
            // given
            when(authoring.getFromCatalog("x")).thenReturn(Optional.of(customQuest));
            // when / then
            assertFalse(Web.isMissingCustomId("x", authoring));
        }
    }

    @Nested
    class RedirectKeep {
        @Test
        @DisplayName("given param names when redirectKeep then carry non-blank")
        void keep() throws IOException {
            // given
            when(req.getParameter("a")).thenReturn(" 1 ");
            when(req.getParameter("b")).thenReturn("  ");
            when(req.getParameter("c")).thenReturn(null);
            when(resp.encodeRedirectURL(anyString())).thenAnswer(a -> a.getArgument(0));
            // when
            Web.redirectKeep(req, resp, WebConst.Path.HOME, "a", "b", "c");
            // then
            ArgumentCaptor<String> cap = ArgumentCaptor.forClass(String.class);
            verify(resp).sendRedirect(cap.capture());
            String url = cap.getValue();
            assertTrue(url.contains("a=+1+"));
            assertFalse(url.contains("b="));
            assertFalse(url.contains("c="));
        }
    }

    @Nested
    class SensitiveChanged {
        @Test
        @DisplayName("given before=null when sensitiveChanged then equals passwordChanged")
        void beforeNull() {
            // given
            // when
            boolean a = Web.sensitiveChanged(null, userAfter, true);
            boolean b = Web.sensitiveChanged(null, userAfter, false);
            // then
            assertTrue(a);
            assertFalse(b);
        }

        @Test
        @DisplayName("given diffs (role/login/password) when sensitiveChanged then true else false")
        void diffs() {
            // given
            when(userBefore.getRole()).thenReturn(Role.ADMIN);
            when(userAfter.getRole()).thenReturn(Role.USER);
            when(userBefore.getUserLogin()).thenReturn("old");
            when(userAfter.getUserLogin()).thenReturn("old");
            // when
            boolean roleDiff = Web.sensitiveChanged(userBefore, userAfter, false);
            // then
            assertTrue(roleDiff);
            // given
            when(userAfter.getRole()).thenReturn(Role.ADMIN);
            when(userAfter.getUserLogin()).thenReturn("new");
            // when
            boolean loginDiff = Web.sensitiveChanged(userBefore, userAfter, false);
            // then
            assertTrue(loginDiff);
            // given
            when(userAfter.getUserLogin()).thenReturn("old");
            // when
            boolean pwdDiff = Web.sensitiveChanged(userBefore, userAfter, true);
            boolean allSame = Web.sensitiveChanged(userBefore, userAfter, false);
            // then
            assertTrue(pwdDiff);
            assertFalse(allSame);
        }
    }

    @Nested
    class AdminChanges {
        @Test
        @DisplayName("given before=null when summary then 'Profile fields were updated' [+password]")
        void summaryInit() {
            // given
            // when
            String s1 = Web.buildAdminChangeSummary(null, userAfter, false);
            String s2 = Web.buildAdminChangeSummary(null, userAfter, true);
            // then
            assertEquals("Profile fields were updated", s1);
            assertTrue(s2.contains("password: changed"));
        }

        @Test
        @DisplayName("given diffs when summary then aggregates, else 'No visible changes'")
        void summaryDiffs() {
            // given
            when(userBefore.getRole()).thenReturn(Role.ADMIN);
            when(userAfter.getRole()).thenReturn(Role.USER);
            when(userBefore.getUserLogin()).thenReturn("o");
            when(userAfter.getUserLogin()).thenReturn("n");
            when(userBefore.getUserName()).thenReturn("O");
            when(userAfter.getUserName()).thenReturn("N");
            // when
            String s = Web.buildAdminChangeSummary(userBefore, userAfter, true);
            // then
            assertTrue(s.contains("role: ADMIN → USER"));
            assertTrue(s.contains("login: o → n"));
            assertTrue(s.contains("name: O → N"));
            assertTrue(s.contains("password: changed"));
            // given
            when(userAfter.getRole()).thenReturn(Role.ADMIN);
            when(userAfter.getUserLogin()).thenReturn("o");
            when(userAfter.getUserName()).thenReturn("O");
            // when
            String none = Web.buildAdminChangeSummary(userBefore, userAfter, false);
            // then
            assertEquals("No visible changes", none);
        }

        @Test
        @DisplayName("given users when buildMachineReadableDiff then 'what' + deltas + flags")
        void diff() {
            // given
            when(userBefore.getRole()).thenReturn(Role.USER);
            when(userAfter.getRole()).thenReturn(Role.ADMIN);
            when(userBefore.getUserLogin()).thenReturn("old");
            when(userAfter.getUserLogin()).thenReturn("new");
            when(userBefore.getUserName()).thenReturn("A");
            when(userAfter.getUserName()).thenReturn("B");
            // when
            Map<String, String> d = Web.buildMachineReadableDiff(userBefore, userAfter, true);
            Map<String, String> d2 = Web.buildMachineReadableDiff(null, userAfter, false);
            // then
            assertTrue(d.containsKey("what"));
            assertEquals("USER", d.get("role.before"));
            assertEquals("ADMIN", d.get("role.after"));
            assertEquals("old", d.get("login.before"));
            assertEquals("new", d.get("login.after"));
            assertEquals("A", d.get("name.before"));
            assertEquals("B", d.get("name.after"));
            assertEquals("true", d.get("password.changed"));
            assertEquals("profile-initialized-by-admin", d2.get("info"));
        }
    }

    @Nested
    class BuildQuestSvgModel {
        @Test
        @DisplayName("given empty/effectively-empty when buildQuestSvgModel then isEmpty=true and minimal canvas")
        void emptyModel() {
            // given
            List<QuestNode> nodes = List.of();
            // when
            Web.buildQuestSvgModel(req, nodes, 1, true);
            // then
            verify(req).setAttribute("width", 800);
            verify(req).setAttribute("height", 300);
            verify(req).setAttribute("isEmpty", Boolean.TRUE);
        }

        @Test
        @DisplayName("given small graph when buildQuestSvgModel then positions & edges computed")
        void smallGraph() {
            // given
            when(nodeA.getId()).thenReturn(1);
            when(nodeA.getFin()).thenReturn(false);
            when(nodeA.getText()).thenReturn("Start node");
            when(nodeA.getOptions()).thenReturn(List.of(optAB));
            when(nodeA.getImage()).thenReturn(null);
            when(optAB.getNext()).thenReturn(2);
            when(optAB.getChoice()).thenReturn("Go");
            when(nodeB.getId()).thenReturn(2);
            when(nodeB.getFin()).thenReturn(true);
            when(nodeB.getText()).thenReturn("Finish");
            when(nodeB.getImage()).thenReturn(null);
            List<QuestNode> nodes = List.of(nodeA, nodeB);
            // when
            Web.buildQuestSvgModel(req, nodes, 1, true, 180, 80, 80, 120, 40);
            // then
            verify(req).setAttribute(eq("nodeW"), eq(180));
            verify(req).setAttribute(eq("nodeH"), eq(80));
            verify(req).setAttribute(eq("isEmpty"), eq(Boolean.FALSE));
            ArgumentCaptor<Object> edgesCap = ArgumentCaptor.forClass(Object.class);
            verify(req, atLeast(1)).setAttribute(eq("edges"), edgesCap.capture());
            List<EdgeSeg> edges = (List<EdgeSeg>) edgesCap.getValue();
            assertEquals(1, edges.size());
            EdgeSeg e = edges.getFirst();
            assertEquals(1, e.getFrom());
            assertEquals(2, e.getTo());
            assertEquals("Go", e.getLabel());
        }
    }

    @Nested
    @DisplayName("filterQuestsByName")
    class FilterQuestsByName {

        @Test
        @DisplayName("given q=null when filterQuestsByName then return original list and set attr q=null")
        void noFilterReturnsOriginal() {
            // given
            when(req.getParameter("q")).thenReturn(null);
            CustomQuest a = mock(CustomQuest.class);
            CustomQuest b = mock(CustomQuest.class);
            List<CustomQuest> items = Arrays.asList(a, b);
            // when
            List<CustomQuest> result = Web.filterQuestsByName(req, items, "q");
            // then
            assertSame(items, result);
            verify(req).setAttribute("q", null);
        }

        @Test
        @DisplayName("given mixed case query when filterQuestsByName then case-insensitive contains and null-safe items")
        void caseInsensitiveAndNullSafe() {
            // given
            when(req.getParameter("term")).thenReturn("  Be  ");
            CustomQuest q1 = mock(CustomQuest.class);
            when(q1.getName()).thenReturn("Alpha");
            CustomQuest q2 = mock(CustomQuest.class);
            when(q2.getName()).thenReturn("BETA");
            CustomQuest q3 = mock(CustomQuest.class);
            when(q3.getName()).thenReturn(null);
            List<CustomQuest> items = Arrays.asList(q1, q2, q3, null);
            // when
            List<CustomQuest> result = Web.filterQuestsByName(req, items, "term");
            // then
            verify(req).setAttribute("q", "Be");
            assertEquals(1, result.size());
            assertSame(q2, result.get(0));
        }

        @Test
        @DisplayName("given query with spaces when filterQuestsByName then trims before filtering")
        void trimsQuery() {
            // given
            when(req.getParameter("search")).thenReturn("   alp   ");
            CustomQuest a = mock(CustomQuest.class);
            when(a.getName()).thenReturn("Alpha");
            CustomQuest b = mock(CustomQuest.class);
            when(b.getName()).thenReturn("Beta");
            List<CustomQuest> items = Arrays.asList(a, b);
            // when
            List<CustomQuest> result = Web.filterQuestsByName(req, items, "search");
            // then
            verify(req).setAttribute("q", "alp");
            assertEquals(1, result.size());
            assertSame(a, result.get(0));
        }
    }

    @Nested
    @DisplayName("filterAndAttachQuests")
    class FilterAndAttachQuests {

        @Test
        @DisplayName("given q=null when filterAndAttachQuests then attaches original items and maps")
        void attachesAllWhenNoFilter() {
            // given
            when(req.getParameter("q")).thenReturn(null);
            CustomQuest a = mock(CustomQuest.class);
            CustomQuest b = mock(CustomQuest.class);
            List<CustomQuest> items = Arrays.asList(a, b);
            // when
            Web.filterAndAttachQuests(req, items);
            // then
            verify(req).setAttribute("q", null);
            verify(req).setAttribute(eq("items"), any());
            verify(req).setAttribute(eq("createdMap"), any());
            verify(req).setAttribute(eq("updatedMap"), any());
        }

        @Test
        @DisplayName("given q filters when filterAndAttachQuests then attaches filtered subset only")
        void attachesFilteredSubset() {
            // given
            when(req.getParameter("q")).thenReturn("  be  ");
            CustomQuest alpha = mock(CustomQuest.class);
            when(alpha.getName()).thenReturn("Alpha");
            CustomQuest beta = mock(CustomQuest.class);
            when(beta.getName()).thenReturn("BETA");
            List<CustomQuest> items = Arrays.asList(alpha, beta);
            // when
            Web.filterAndAttachQuests(req, items);
            // then
            verify(req).setAttribute("q", "be");
            ArgumentCaptor<Object> cap = ArgumentCaptor.forClass(Object.class);
            verify(req).setAttribute(eq("items"), cap.capture());
            List<CustomQuest> attached = (List<CustomQuest>) cap.getValue();
            assertEquals(1, attached.size());
            assertSame(beta, attached.get(0));
            verify(req).setAttribute(eq("createdMap"), any());
            verify(req).setAttribute(eq("updatedMap"), any());
        }
    }
    @Nested
    @DisplayName("Params (value object)")
    class ParamsVo {

        @Test
        @DisplayName("given values — when construct — then fields exposed as-is")
        void ctorStoresValues() {
            // Given
            String q = "Mike";
            int page = 3;
            int size = 12;
            // When
            Web.Params p = new Web.Params(q, page, size);
            // Then
            assertEquals("Mike", p.q);
            assertEquals(3, p.page);
            assertEquals(12, p.size);
        }
    }

    @Nested
    @DisplayName("extract(req)")
    class ExtractParams {

        @BeforeEach
        void lenientDefaults() {
            lenient().when(req.getParameter("q")).thenReturn(null);
            lenient().when(req.getParameter("page")).thenReturn(null);
        }

        @Test
        @DisplayName("given no params — when extract — then q=null, page=1, size=12")
        void defaults() {
            // when
            Web.Params p = Web.extract(req);
            // then
            assertNull(p.q);
            assertEquals(1, p.page);
            assertEquals(12, p.size);
        }

        @Test
        @DisplayName("given q with spaces — when extract — then q trimmed")
        void trimsQ() {
            // given
            when(req.getParameter("q")).thenReturn("  He llo  ");
            // when
            Web.Params p = Web.extract(req);
            // then
            assertEquals("He llo", p.q);
            assertEquals(1, p.page);
            assertEquals(12, p.size);
        }

        @Test
        @DisplayName("given page='5' — when extract — then page=5 (1-based), size=12")
        void parsesPage() {
            // given
            when(req.getParameter("page")).thenReturn("5");
            // when
            Web.Params p = Web.extract(req);
            // then
            assertEquals(5, p.page);
            assertEquals(12, p.size);
        }

        @Test
        @DisplayName("given page is invalid — when extract — then page=1")
        void invalidPageToOne() {
            // given
            when(req.getParameter("page")).thenReturn("x");
            // when
            Web.Params p = Web.extract(req);
            // then
            assertEquals(1, p.page);
        }

        @Test
        @DisplayName("given page<1 — when extract — then page=1")
        void clampsPageToOne() {
            // given
            when(req.getParameter("page")).thenReturn("-2");
            // when
            Web.Params p = Web.extract(req);
            // then
            assertEquals(1, p.page);
        }
    }

    @Nested
    @DisplayName("attachOwnerNamesById(req, items, userService)")
    class AttachOwnerNamesById {

        @Test
        @DisplayName("given items with owners — when attachOwnerNamesById — then ownerNameById map with resolved names; duplicates collapsed")
        void resolvesNamesAndSetsAttribute() {
            // Given
            CustomQuest q1 = mock(CustomQuest.class);
            CustomQuest q2 = mock(CustomQuest.class);
            CustomQuest q3 = mock(CustomQuest.class);
            when(q1.getOwnerId()).thenReturn("u1");
            when(q2.getOwnerId()).thenReturn("u2");
            when(q3.getOwnerId()).thenReturn("u1");
            User U1 = mock(User.class);
            when(U1.getUserName()).thenReturn("Alice");
            User U2 = mock(User.class);
            when(U2.getUserName()).thenReturn("Bob");
            when(userService.findById("u1")).thenReturn(Optional.of(U1));
            when(userService.findById("u2")).thenReturn(Optional.of(U2));
            List<CustomQuest> items = List.of(q1, q2, q3);
            // When
            Web.attachOwnerNamesById(req, items, userService);
            // Then
            ArgumentCaptor<Map<String, String>> cap = ArgumentCaptor.forClass(Map.class);
            verify(req).setAttribute(eq("ownerNameById"), cap.capture());
            Map<String, String> map = cap.getValue();
            assertEquals(2, map.size());
            assertEquals("Alice", map.get("u1"));
            assertEquals("Bob", map.get("u2"));
        }

        @Test
        @DisplayName("given userService misses some ids — when attachOwnerNamesById — then absent ids not added")
        void missingUsersSkipped() {
            // Given
            CustomQuest q1 = mock(CustomQuest.class);
            when(q1.getOwnerId()).thenReturn("uX");
            when(userService.findById("uX")).thenReturn(Optional.empty());
            // When
            Web.attachOwnerNamesById(req, List.of(q1), userService);
            // Then
            ArgumentCaptor<Map<String, String>> cap = ArgumentCaptor.forClass(Map.class);
            verify(req).setAttribute(eq("ownerNameById"), cap.capture());
            assertTrue(cap.getValue().isEmpty());
        }
    }

    @Nested
    @DisplayName("applyPagedList(req, paged, userService, q)")
    class ApplyPagedList {

        @Test
        @DisplayName("given paged data — when applyPagedList — then sets list attrs and ownerNameById")
        void appliesAllAttributes() {
            // Given
            CustomQuest a = mock(CustomQuest.class);
            CustomQuest b = mock(CustomQuest.class);
            when(a.getOwnerId()).thenReturn("u1");
            when(b.getOwnerId()).thenReturn("u2");
            List<CustomQuest> items = List.of(a, b);
            QuestAuthoringService.Paged<CustomQuest> paged =
                    new QuestAuthoringService.Paged<>(items,42, 3, 12);
            User U1 = mock(User.class); when(U1.getUserName()).thenReturn("Alice");
            User U2 = mock(User.class); when(U2.getUserName()).thenReturn("Bob");
            when(userService.findById("u1")).thenReturn(Optional.of(U1));
            when(userService.findById("u2")).thenReturn(Optional.of(U2));
            String q = "Mike";
            // When
            Web.applyPagedList(req, paged, userService, q);
            // Then
            ArgumentCaptor<Map<String, String>> ownersCap = ArgumentCaptor.forClass(Map.class);
            verify(req).setAttribute(eq("ownerNameById"), ownersCap.capture());
            Map<String, String> owners = ownersCap.getValue();
            assertEquals("Alice", owners.get("u1"));
            assertEquals("Bob", owners.get("u2"));
            verify(req).setAttribute("items", items);
            verify(req).setAttribute("total", 42);
            verify(req).setAttribute("pages", paged.getPages());
            verify(req).setAttribute("page", 3);
            verify(req).setAttribute("q", "Mike");
        }

        @Test
        @DisplayName("given empty items — when applyPagedList — then still sets attributes")
        void emptyListStillSetsAttrs() {
            // Given
            QuestAuthoringService.Paged<CustomQuest> paged =
                    new QuestAuthoringService.Paged<>(List.of(), 0, 1, 12);
            // When
            Web.applyPagedList(req, paged, userService, null);
            // Then
            verify(req).setAttribute("items", paged.getItems());
            verify(req).setAttribute("total", 0);
            verify(req).setAttribute("pages", paged.getPages());
            verify(req).setAttribute("page", 1);
            verify(req).setAttribute("q", null);
            ArgumentCaptor<Map<String, String>> ownersCap = ArgumentCaptor.forClass(Map.class);
            verify(req).setAttribute(eq("ownerNameById"), ownersCap.capture());
            assertTrue(ownersCap.getValue().isEmpty());
        }
    }
}
