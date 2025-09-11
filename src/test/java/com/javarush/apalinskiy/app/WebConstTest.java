package com.javarush.apalinskiy.app;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("WebConst")
class WebConstTest {

    private static void assertPrivateCtor(Class<?> cls) {
        // Given / When
        Constructor<?> c = assertDoesNotThrow(() -> cls.getDeclaredConstructor());
        // Then
        assertTrue(Modifier.isPrivate(c.getModifiers()));
    }

    @Test
    @DisplayName("has private ctor (root)")
    void rootHasPrivateCtor() {
        // Given / When / Then
        assertPrivateCtor(WebConst.class);
    }

    @Nested
    @DisplayName("Charset")
    class CharsetTests {
        @Test
        @DisplayName("UTF8 == StandardCharsets.UTF_8.name()")
        void utf8ConstMatchesJdk() {
            // Given / When
            String actual = WebConst.Charset.UTF8;
            // Then
            assertEquals(StandardCharsets.UTF_8.name(), actual);
        }

        @Test
        @DisplayName("has private ctor")
        void privateCtor() {
            // Given / When / Then
            assertPrivateCtor(WebConst.Charset.class);
        }
    }

    @Nested
    @DisplayName("Param")
    class ParamTests {
        @Test
        @DisplayName("ROLE == 'role'")
        void role() {
            // Given / When
            String v = WebConst.Param.ROLE;
            // Then
            assertEquals("role", v);
        }

        @Test
        @DisplayName("PASSWORD == 'password'")
        void password() {
            // Given / When
            String v = WebConst.Param.PASSWORD;
            // Then
            assertEquals("password", v);
        }

        @Test
        @DisplayName("has private ctor")
        void privateCtor() {
            // Given / When / Then
            assertPrivateCtor(WebConst.Param.class);
        }
    }

    @Nested
    @DisplayName("Op")
    class OpTests {
        @Test
        @DisplayName("GO == 'go'")
        void go() {
            // Given / When
            String v = WebConst.Op.GO;
            // Then
            assertEquals("go", v);
        }

        @Test
        @DisplayName("CANCEL == 'cancel'")
        void cancel() {
            // Given / When
            String v = WebConst.Op.CANCEL;
            // Then
            assertEquals("cancel", v);
        }

        @Test
        @DisplayName("has private ctor")
        void privateCtor() {
            // Given / When / Then
            assertPrivateCtor(WebConst.Op.class);
        }
    }

    @Nested
    @DisplayName("Attr")
    class AttrTests {
        @Test
        @DisplayName("USER == 'user'")
        void user() {
            // Given / When
            String v = WebConst.Attr.USER;
            // Then
            assertEquals("user", v);
        }

        @Test
        @DisplayName("ERROR == 'error'")
        void error() {
            // Given / When
            String v = WebConst.Attr.ERROR;
            // Then
            assertEquals("error", v);
        }

        @Test
        @DisplayName("has private ctor")
        void privateCtor() {
            // Given / When / Then
            assertPrivateCtor(WebConst.Attr.class);
        }
    }

    @Nested
    @DisplayName("Ctx")
    class CtxTests {
        @Test
        @DisplayName("USER_SERVICE == 'userService'")
        void userService() {
            // Given / When
            String v = WebConst.Ctx.USER_SERVICE;
            // Then
            assertEquals("userService", v);
        }

        @Test
        @DisplayName("QUEST_SERVICE == 'questService'")
        void questService() {
            // Given / When
            String v = WebConst.Ctx.QUEST_SERVICE;
            // Then
            assertEquals("questService", v);
        }

        @Test
        @DisplayName("has private ctor")
        void privateCtor() {
            // Given / When / Then
            assertPrivateCtor(WebConst.Ctx.class);
        }
    }

    @Nested
    @DisplayName("Path")
    class PathTests {
        @Test
        @DisplayName("LOGIN == '/login'")
        void login() {
            // Given / When
            String v = WebConst.Path.LOGIN;
            // Then
            assertEquals("/login", v);
        }

        @Test
        @DisplayName("PUBLISH == '/quest/publish'")
        void publish() {
            // Given / When
            String v = WebConst.Path.PUBLISH;
            // Then
            assertEquals("/quest/publish", v);
        }

        @Test
        @DisplayName("has private ctor")
        void privateCtor() {
            // Given / When / Then
            assertPrivateCtor(WebConst.Path.class);
        }
    }

    @Nested
    @DisplayName("Jsp")
    class JspTests {
        @Test
        @DisplayName("LOGIN == '/WEB-INF/jsp/login.jsp'")
        void loginJsp() {
            // Given / When
            String v = WebConst.Jsp.LOGIN;
            // Then
            assertEquals("/WEB-INF/jsp/login.jsp", v);
        }

        @Test
        @DisplayName("PUBLISH == '/WEB-INF/jsp/publish_confirm.jsp'")
        void publishJsp() {
            // Given / When
            String v = WebConst.Jsp.PUBLISH;
            // Then
            assertEquals("/WEB-INF/jsp/publish_confirm.jsp", v);
        }

        @Test
        @DisplayName("has private ctor")
        void privateCtor() {
            // Given / When / Then
            assertPrivateCtor(WebConst.Jsp.class);
        }
    }

    @Nested
    @DisplayName("App")
    class AppTests {
        @Test
        @DisplayName("QUEST_RESOURCE == 'quest.json'")
        void resource() {
            // Given / When
            String v = WebConst.App.QUEST_RESOURCE;
            // Then
            assertEquals("quest.json", v);
        }

        @Test
        @DisplayName("DEFAULT_ADMIN_LOGIN == 'admin'")
        void adminLogin() {
            // Given / When
            String v = WebConst.App.DEFAULT_ADMIN_LOGIN;
            // Then
            assertEquals("admin", v);
        }

        @Test
        @DisplayName("QUEST_START_ID == 1")
        void startId() {
            // Given / When
            int v = WebConst.App.QUEST_START_ID;
            // Then
            assertEquals(1, v);
        }

        @Test
        @DisplayName("has private ctor")
        void privateCtor() {
            // Given / When / Then
            assertPrivateCtor(WebConst.App.class);
        }
    }

    @Test
    @DisplayName("SLOT_COUNT == 10")
    void slotCount() {
        // Given / When
        int v = WebConst.SLOT_COUNT;
        // Then
        assertEquals(10, v);
    }

    @Nested
    @DisplayName("Msg")
    class MsgTests {
        @Test
        @DisplayName("BAD_CREDENTIALS == 'Incorrect login or password'")
        void badCreds() {
            // Given / When
            String v = WebConst.Msg.BAD_CREDENTIALS;
            // Then
            assertEquals("Incorrect login or password", v);
        }

        @Test
        @DisplayName("NODE_NOT_FOUND_PREFIX starts with 'Узел не найден: id='")
        void nodePrefix() {
            // Given / When
            String v = WebConst.Msg.NODE_NOT_FOUND_PREFIX;
            // Then
            assertTrue(v.startsWith("Узел не найден: id="));
        }

        @Test
        @DisplayName("has private ctor")
        void privateCtor() {
            // Given / When / Then
            assertPrivateCtor(WebConst.Msg.class);
        }
    }

    @Nested
    @DisplayName("InitParam")
    class InitParamTests {
        @Test
        @DisplayName("CTX_UPLOADS_DIR == 'uploads.base.dir'")
        void uploadsDir() {
            // Given / When
            String v = WebConst.InitParam.CTX_UPLOADS_DIR;
            // Then
            assertEquals("uploads.base.dir", v);
        }

        @Test
        @DisplayName("has private ctor")
        void privateCtor() {
            // Given / When / Then
            assertPrivateCtor(WebConst.InitParam.class);
        }
    }

    @Nested
    @DisplayName("ParamGroup")
    class ParamGroupTests {
        @Test
        @DisplayName("SLOT_NAV length == 4")
        void slotNavLen() {
            // Given / When
            int len = WebConst.ParamGroup.SLOT_NAV.length;
            // Then
            assertEquals(4, len);
        }

        @Test
        @DisplayName("SLOT_NAV order == [NEXT, PURPOSE, NODE, CUSTOM]")
        void slotNavOrder() {
            // Given / When
            String[] actual = WebConst.ParamGroup.SLOT_NAV;
            // Then
            assertArrayEquals(
                    new String[]{WebConst.Param.NEXT, WebConst.Param.PURPOSE, WebConst.Param.NODE, WebConst.Param.CUSTOM},
                    actual
            );
        }

        @Test
        @DisplayName("has private ctor")
        void privateCtor() {
            // Given / When / Then
            assertPrivateCtor(WebConst.ParamGroup.class);
        }
    }
}