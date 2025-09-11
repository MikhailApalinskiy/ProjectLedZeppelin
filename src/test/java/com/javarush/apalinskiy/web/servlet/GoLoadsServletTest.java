package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.app.WebConst;
import com.javarush.apalinskiy.service.save.SaveStateService;
import com.javarush.apalinskiy.web.util.Web;
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

import java.io.IOException;
import java.util.Optional;

import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GoLoadsServlet")
class GoLoadsServletTest {

    @Mock
    HttpServletRequest req;
    @Mock
    HttpServletResponse resp;
    @Mock
    HttpSession session;
    @Mock
    SaveStateService saveState;
    @Mock
    SaveStateService.GlobalSlot gslot;

    private static void setField(Object target, Object value) {
        Class<?> c = target.getClass();
        while (c != null) {
            try {
                var f = c.getDeclaredField("saveState");
                f.setAccessible(true);
                f.set(target, value);
                return;
            } catch (NoSuchFieldException ignored) {
                c = c.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        throw new RuntimeException(new NoSuchFieldException("saveState"));
    }

    @Nested
    @DisplayName("handleGo")
    class HandleGo {

        @Test
        @DisplayName("given empty slot when handleGo then redirect back with SLOT_NAV")
        void emptySlot_redirectsBack() throws Exception {
            // given
            GoLoadsServlet s = new GoLoadsServlet();
            setField(s, saveState);
            when(saveState.getGlobalSlot("u1", 3)).thenReturn(Optional.empty());
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                // when
                s.handleGo(req, resp, "u1", null, 3, "/quest");
                // then
                web.verify(() -> Web.redirectKeep(req, resp, WebConst.Path.LOADS, WebConst.ParamGroup.SLOT_NAV));
            }
        }

        @Test
        @DisplayName("given custom slot data when handleGo then flash and redirect to questUrl with custom id")
        void filled_custom_redirectsToQuest() throws Exception {
            // given
            GoLoadsServlet s = new GoLoadsServlet();
            setField(s, saveState);
            when(saveState.getGlobalSlot("u1", 0)).thenReturn(Optional.of(gslot));
            when(gslot.questName()).thenReturn("Custom Name");
            when(gslot.title()).thenReturn("Title X");
            when(gslot.questId()).thenReturn("q123");
            when(gslot.nodeId()).thenReturn(7);
            when(req.getSession()).thenReturn(session);
            when(resp.encodeRedirectURL(anyString())).thenAnswer(a -> a.getArgument(0));
            String questTarget = "/ctx/quest?id=7&custom=q123";
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.questUrl(eq(req), eq(7), eq("q123"))).thenReturn(questTarget);
                // when
                s.handleGo(req, resp, "u1", null, 0, "/next");
                // then
                verify(session).setAttribute(eq(WebConst.Attr.FLASH),
                        eq("Slot № 1 is loaded — Custom Name • Title X."));
                verify(resp).sendRedirect(eq(questTarget));
            }
        }

        @Test
        @DisplayName("given main quest slot with missing fields when handleGo then fallbacks and questUrl without custom")
        void filled_main_fallbacksAndNoCustom() throws IOException {
            // given
            GoLoadsServlet s = new GoLoadsServlet();
            setField(s, saveState);
            when(saveState.getGlobalSlot("u1", 2)).thenReturn(Optional.of(gslot));
            when(gslot.questName()).thenReturn(null);
            when(gslot.title()).thenReturn(null);
            when(gslot.questId()).thenReturn("main");
            when(gslot.nodeId()).thenReturn(42);
            when(req.getSession()).thenReturn(session);
            when(resp.encodeRedirectURL(anyString())).thenAnswer(a -> a.getArgument(0));
            String questTarget = "/ctx/quest?id=42";
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.questUrl(eq(req), eq(42), isNull())).thenReturn(questTarget);
                // when
                s.handleGo(req, resp, "u1", null, 2, "/next");
                // then
                verify(session).setAttribute(eq(WebConst.Attr.FLASH),
                        eq("Slot № 3 is loaded — Main quest • Node #42."));
                verify(resp).sendRedirect(eq(questTarget));
            }
        }
    }
}