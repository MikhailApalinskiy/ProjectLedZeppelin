package com.javarush.apalinskiy.app;

import java.nio.charset.StandardCharsets;

/**
 * Central place for application-wide constants used in servlets, JSPs,
 * and service initialization.
 * <p>
 * This class contains nested groups of constants organized by purpose:
 * <ul>
 *   <li>{@link Charset} – encoding definitions.</li>
 *   <li>{@link Param} – request parameter names.</li>
 *   <li>{@link Op} – common operation values for form parameters.</li>
 *   <li>{@link Attr} – request/session attribute keys.</li>
 *   <li>{@link Ctx} – servlet context attribute keys for services.</li>
 *   <li>{@link Path} – servlet path mappings.</li>
 *   <li>{@link Jsp} – JSP view locations.</li>
 *   <li>{@link App} – application defaults and configuration values.</li>
 *   <li>{@link Msg} – common user-facing messages.</li>
 *   <li>{@link InitParam} – servlet context init parameter names.</li>
 *   <li>{@link ParamGroup} – predefined groups of parameters.</li>
 * </ul>
 * <p>
 * This class is {@code final} and cannot be instantiated.
 */
public final class WebConst {

    private WebConst() {
    }

    /**
     * Character set constants.
     */
    public static final class Charset {
        public static final String UTF8 = StandardCharsets.UTF_8.name();

        private Charset() {
        }
    }

    /**
     * Common HTTP request parameter names used throughout the application.
     */
    public static final class Param {
        public static final String ROLE = "role";
        public static final String CURRENT_PASSWORD = "currentPassword";
        public static final String NEW_PASSWORD = "newPassword";
        public static final String CONFIRM_PASSWORD = "confirmPassword";
        public static final String TEXT = "text";
        public static final String OP = "op";
        public static final String SLOT = "slot";
        public static final String NEXT = "next";
        public static final String NODE = "node";
        public static final String PURPOSE = "purpose";
        public static final String ID = "id";
        public static final String FROM_ID = "fromId";
        public static final String ANSWER = "answer";
        public static final String CUSTOM = "custom";
        public static final String ACTION = "action";
        public static final String FINAL = "final";
        public static final String OPTIONS = "options";
        public static final String IMAGE_FILE = "imageFile";
        public static final String CLEAR = "clear";
        public static final String LOAD = "load";
        public static final String NEW = "new";
        public static final String USER_LOGIN = "userLogin";
        public static final String USER_NAME = "userName";
        public static final String PASSWORD = "password";

        private Param() {
        }
    }

    /**
     * Operation codes used in forms and request parameters.
     */
    public static final class Op {
        public static final String GO = "go";
        public static final String DELETE = "delete";
        public static final String CONFIRM = "confirm";
        public static final String CANCEL = "cancel";

        private Op() {
        }
    }

    /**
     * Request/session attribute keys.
     */
    public static final class Attr {
        public static final String EDITING_QUEST_ID = "editingQuestId";
        public static final String EDITING_QUEST_NAME = "editingQuestName";
        public static final String USER = "user";
        public static final String FLASH = "flash";
        public static final String ERROR = "error";
        public static final String OK = "ok";
        public static final String NODE = "node";
        public static final String VERSION = "version";
        public static final String CUSTOM = "custom";

        private Attr() {
        }
    }

    /**
     * Keys for objects stored in {@link jakarta.servlet.ServletContext}.
     */
    public static final class Ctx {
        public static final String USER_STATS_SERVICE = "userStatsService";
        public static final String FRIEND_REPOSITORY = "friendRepository";
        public static final String NOTIFY_REPO = "notifyRepo";
        public static final String NOTIFY_SERVICE = "notifyService";
        public static final String USER_SERVICE = "userService";
        public static final String QUEST_SERVICE = "questService";
        public static final String SAVE_STATE_SERVICE = "saveStateService";
        public static final String PROD_REPOSITORY = "prodQuestRepository";
        public static final String EDITOR_REPOSITORY = "editorQuestRepository";
        public static final String AUTHORING_SERVICE = "authoringService";
        public static final String FRIEND_SERVICE = "friendService";

        private Ctx() {
        }
    }

    /**
     * Servlet path mappings for controllers.
     */
    public static final class Path {
        public static final String QUESTS_MOD = "/quests/moderation";
        public static final String USER_EDIT = "/user/edit";
        public static final String FRIENDS = "/friends";
        public static final String NOTIFICATIONS = "/notifications";
        public static final String USERS = "/users";
        public static final String PROFILE = "/profile";
        public static final String GRAPH_SVG = "/quest/graph_svg";
        public static final String HOME = "/";
        public static final String QUEST = "/quest";
        public static final String SAVES = "/saves";
        public static final String LOADS = "/loads";
        public static final String LOGIN = "/login";
        public static final String REGISTER = "/register";
        public static final String CREATE = "/create_quest";
        public static final String PUBLISH = "/quest/publish";
        public static final String MY_QUESTS = "/my/quests";

        private Path() {
        }
    }

    /**
     * JSP view paths.
     */
    public static final class Jsp {
        public static final String USER_PUBLIC = "/WEB-INF/jsp/user_public.jsp";
        public static final String USER_QUESTS = "/WEB-INF/jsp/user_quests.jsp";
        public static final String QUESTS_MOD_PREVIEW = "/WEB-INF/jsp/quests_mod_preview.jsp";
        public static final String QUESTS_MOD = "/WEB-INF/jsp/quests_moderation.jsp";
        public static final String USER_EDIT = "/WEB-INF/jsp/user_edit.jsp";
        public static final String FRIENDS = "/WEB-INF/jsp/friends.jsp";
        public static final String NOTIFICATIONS = "/WEB-INF/jsp/notifications.jsp";
        public static final String USERS = "/WEB-INF/jsp/users.jsp";
        public static final String PROFILE = "/WEB-INF/jsp/profile.jsp";
        public static final String INDEX = "/WEB-INF/jsp/index.jsp";
        public static final String QUEST = "/WEB-INF/jsp/quest.jsp";
        public static final String SAVES = "/WEB-INF/jsp/saves.jsp";
        public static final String LOADS = "/WEB-INF/jsp/loads.jsp";
        public static final String CONFIRM = "/WEB-INF/jsp/confirm-overwrite.jsp";
        public static final String LOGIN = "/WEB-INF/jsp/login.jsp";
        public static final String REGISTER = "/WEB-INF/jsp/register.jsp";
        public static final String CREATE = "/WEB-INF/jsp/create_quest.jsp";
        public static final String GRAPH_SVG = "/WEB-INF/jsp/graph_svg.jsp";
        public static final String QUESTS_LIST = "/WEB-INF/jsp/quests_list.jsp";
        public static final String PUBLISH = "/WEB-INF/jsp/publish_confirm.jsp";

        private Jsp() {
        }
    }

    /**
     * Application defaults and configuration constants.
     */
    public static final class App {
        public static final String QUEST_RESOURCE = "quest.json";
        public static final int QUEST_START_ID = 1;
        public static final String DEFAULT_ADMIN_NAME = "Admin";
        public static final String DEFAULT_ADMIN_LOGIN = "admin";
        public static final String DEFAULT_ADMIN_PASS = "admin";

        private App() {
        }
    }

    /**
     * Number of save slots available per user.
     */
    public static final int SLOT_COUNT = 10;

    /**
     * Common localized or user-facing messages.
     */
    public static final class Msg {
        public static final String BAD_CREDENTIALS = "Incorrect login or password";
        public static final String INTERNAL_ERROR = "Internal error. Please try again.";
        public static final String BAD_FROM_ID = "Некорректный fromId";
        public static final String NODE_NOT_FOUND_PREFIX = "Узел не найден: id=";

        private Msg() {
        }
    }

    /**
     * Servlet context initialization parameter names.
     */
    public static final class InitParam {
        public static final String CTX_UPLOADS_DIR = "uploads.base.dir";

        private InitParam() {
        }
    }

    /**
     * Predefined groups of request parameters.
     */
    public static final class ParamGroup {

        /**
         * Parameters related to slot navigation (next step, purpose, node, custom).
         */
        public static final String[] SLOT_NAV = {
                Param.NEXT, Param.PURPOSE, Param.NODE, Param.CUSTOM
        };

        private ParamGroup() {
        }
    }
}
