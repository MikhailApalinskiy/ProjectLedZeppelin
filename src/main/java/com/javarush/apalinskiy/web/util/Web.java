package com.javarush.apalinskiy.web.util;

import com.javarush.apalinskiy.app.WebConst;
import com.javarush.apalinskiy.service.quest.QuestAuthoringService;
import com.javarush.apalinskiy.domain.quest.Option;
import com.javarush.apalinskiy.domain.quest.QuestNode;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.domain.quest.custom.CustomQuest;
import com.javarush.apalinskiy.service.user.UserService;
import com.javarush.apalinskiy.web.view.EdgeSeg;
import com.javarush.apalinskiy.web.view.NodePos;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Web utility helpers for Servlet/JSP layer.
 * <p>
 * Responsibilities include:
 * <ul>
 *   <li>Parsing and normalizing request parameters;</li>
 *   <li>Building URLs/redirects and carrying flash messages;</li>
 *   <li>Preparing common JSP attributes (lists, pagination, names, dates);</li>
 *   <li>Constructing a lightweight SVG view-model of a quest graph.</li>
 * </ul>
 * <p>
 * This class is not meant to be instantiated.
 */
public class Web {

    private Web() {
    }

    /**
     * Trims the string and returns {@code null} if the result is empty.
     *
     * @param s source string, may be {@code null}
     * @return trimmed string or {@code null} if empty/blank
     */
    public static String trimOrNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /**
     * Parses an integer value or returns {@code null} if parsing fails.
     *
     * @param s input string, may be {@code null}/blank
     * @return parsed {@link Integer} or {@code null} on failure
     */
    public static Integer parseIntOrNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parses an integer value or returns the provided default.
     *
     * @param s   input string
     * @param def default value to return on failure
     * @return parsed integer or {@code def} when parsing fails
     */
    public static int parseIntOrDefault(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return def;
        }
    }

    /**
     * Returns the first successfully parsed int parameter from the given names.
     *
     * @param req   HTTP request
     * @param names parameter names to probe in order
     * @return first parsed integer or {@code null} if none
     */
    public static Integer firstIntParam(HttpServletRequest req, String... names) {
        for (String n : names) {
            Integer v = parseIntOrNull(req.getParameter(n));
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    /**
     * Immutable holder for common paging/search parameters.
     */
    public static class Params {

        /**
         * Optional query text (may be {@code null}).
         */
        public final String q;

        /**
         * 1-based page index.
         */
        public final int page;

        /**
         * Page size (fixed).
         */
        public final int size;

        /**
         * Creates a new parameter bundle.
         *
         * @param q    search query (nullable)
         * @param page 1-based page index
         * @param size page size
         */
        public Params(String q, int page, int size) {
            this.q = q;
            this.page = page;
            this.size = size;
        }
    }

    /**
     * Extracts common paging/search parameters from the request.
     * <p>Defaults: {@code size=12}, {@code page=1} (1-based).</p>
     *
     * @param req HTTP request
     * @return filled {@link Params} instance
     */
    public static Params extract(HttpServletRequest req) {
        final String q = Web.trimOrNull(req.getParameter("q"));
        final int size = 12;
        int page = 1;
        try {
            String p = req.getParameter("page");
            if (p != null) {
                page = Math.max(1, Integer.parseInt(p));
            }
        } catch (NumberFormatException ignore) {
        }
        return new Params(q, page, size);
    }

    /**
     * Applies paged list attributes for JSP rendering and attaches owner names map.
     *
     * @param req         HTTP request
     * @param paged       paged data
     * @param userService user service for name resolution
     * @param q           original query string (nullable)
     */
    @SuppressWarnings("unchecked")
    public static void applyPagedList(HttpServletRequest req,
                                      QuestAuthoringService.Paged<?> paged,
                                      UserService userService,
                                      String q) {
        Web.attachOwnerNamesById(req, (List<CustomQuest>) paged.getItems(), userService);
        req.setAttribute("items", paged.getItems());
        req.setAttribute("total", paged.getTotal());
        req.setAttribute("pages", paged.getPages());
        req.setAttribute("page", paged.getPage());
        req.setAttribute("q", q);
    }

    /**
     * Resolves and attaches a map {@code ownerNameById} (ownerId -&gt; userName).
     *
     * @param req         HTTP request
     * @param items       custom quests (ownerId must be present)
     * @param userService user service for lookups
     */
    public static void attachOwnerNamesById(HttpServletRequest req, List<CustomQuest> items, UserService userService) {
        Set<String> ids = new LinkedHashSet<>();
        for (CustomQuest q : items) ids.add(q.getOwnerId());
        Map<String, String> map = new HashMap<>();
        for (String id : ids) {
            userService.findById(id).ifPresent(u -> map.put(id, u.getUserName()));
        }
        req.setAttribute("ownerNameById", map);
    }

    /**
     * URL-encodes a value in UTF-8. Returns the input when encoding unexpectedly fails.
     *
     * @param v value to encode, {@code null} treated as empty string
     * @return encoded value
     */
    public static String urlEncode(String v) {
        try {
            return URLEncoder.encode(v == null ? "" : v, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return v;
        }
    }

    /**
     * Appends encoded query parameters to a base URL.
     *
     * @param base   base URL (with or without existing query)
     * @param params parameters to append (may be {@code null}/empty)
     * @return resulting URL
     */
    public static String addParamsEncoded(String base, Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return base;
        }
        StringBuilder sb = new StringBuilder(base);
        String sep = base.contains("?") ? "&" : "?";
        for (Map.Entry<String, String> e : params.entrySet()) {
            sb.append(sep).append(urlEncode(e.getKey())).append("=").append(urlEncode(e.getValue()));
            sep = "&";
        }
        return sb.toString();
    }

    /**
     * Appends parameters (found in the request by the given names) to the base URL.
     *
     * @param req   HTTP request
     * @param base  base URL
     * @param names parameter names to copy
     * @return resulting URL with encoded copied params
     */
    public static String addParamsFromReqEncoded(HttpServletRequest req, String base, String... names) {
        Map<String, String> m = new LinkedHashMap<>();
        for (String n : names) {
            String v = req.getParameter(n);
            if (v != null && !v.isBlank()) {
                m.put(n, v);
            }
        }
        return addParamsEncoded(base, m);
    }

    /**
     * Builds a quest URL using the {@code next} parameter fallback and the current {@code custom} value.
     *
     * @param req    HTTP request
     * @param next   preferred path (may be {@code null}/blank)
     * @param nodeId quest node id
     * @return absolute context-relative quest URL
     */
    public static String buildQuestUrlFromNext(HttpServletRequest req, String next, int nodeId) {
        String base = req.getContextPath() + ((next == null || next.isBlank()) ? WebConst.Path.QUEST : next);
        String custom = trimOrNull(req.getParameter(WebConst.Param.CUSTOM));
        if (custom == null) {
            Object attr = req.getAttribute(WebConst.Param.CUSTOM);
            if (attr instanceof String s && !s.isBlank()) {
                custom = s;
            }
        }
        if ("main".equalsIgnoreCase(custom)) {
            custom = null;
        }
        return appendQuestParams(base, nodeId, custom);
    }

    /**
     * Builds a quest URL for a specific node and custom quest id.
     *
     * @param req            HTTP request
     * @param id             quest node id
     * @param customIdOrNull custom quest id or {@code null} for main
     * @return absolute context-relative quest URL
     */
    public static String questUrl(HttpServletRequest req, int id, String customIdOrNull) {
        String base = req.getContextPath() + WebConst.Path.QUEST;
        return appendQuestParams(base, id, customIdOrNull);
    }

    /**
     * Forwards to a JSP.
     *
     * @param req  request
     * @param resp response
     * @param jsp  JSP path under {@code /WEB-INF}
     * @throws ServletException if forwarding fails
     * @throws IOException      if I/O fails
     */
    public static void forward(HttpServletRequest req, HttpServletResponse resp, String jsp)
            throws ServletException, IOException {
        req.getRequestDispatcher(jsp).forward(req, resp);
    }

    /**
     * Returns {@code next} when it is safe (context-local), otherwise the home path.
     *
     * @param req  request
     * @param next candidate URL
     * @return safe URL
     */
    public static String safeNextOrHome(HttpServletRequest req, String next) {
        return isSafeNext(req, next) ? next : (req.getContextPath() + WebConst.Path.HOME);
    }

    /**
     * Checks if a URL is safe to redirect to (must start with current context path).
     *
     * @param req  request
     * @param next candidate URL
     * @return {@code true} if safe
     */
    public static boolean isSafeNext(HttpServletRequest req, String next) {
        if (next == null || next.isBlank()) {
            return false;
        }
        String ctx = req.getContextPath();
        return next.startsWith(ctx + "/");
    }

    /**
     * Sends a redirect with encoded parameters.
     *
     * @param req    request
     * @param resp   response
     * @param path   context-relative path
     * @param params query parameters to append
     * @throws IOException if sending redirect fails
     */
    public static void redirect(HttpServletRequest req, HttpServletResponse resp, String path, Map<String, String> params) throws IOException {
        String url = addParamsEncoded(req.getContextPath() + path, params);
        resp.sendRedirect(resp.encodeRedirectURL(url));
    }

    /**
     * Redirect with an OK flash message.
     *
     * @param req  request
     * @param resp response
     * @param path destination path
     * @param msg  message text (nullable)
     * @throws IOException if redirect fails
     */
    public static void redirectOk(HttpServletRequest req, HttpServletResponse resp, String path, String msg) throws IOException {
        redirect(req, resp, path, Map.of(WebConst.Attr.OK, Objects.toString(msg, "")));
    }

    /**
     * Redirect with an error flash message.
     *
     * @param req  request
     * @param resp response
     * @param path destination path
     * @param msg  message text (nullable)
     * @throws IOException if redirect fails
     */
    public static void redirectErr(HttpServletRequest req, HttpServletResponse resp, String path, String msg) throws IOException {
        redirect(req, resp, path, Map.of(WebConst.Attr.ERROR, Objects.toString(msg, "")));
    }

    /**
     * Invalidates the current session (if any), creates a fresh one, and stores the provided attribute.
     *
     * @param req   request
     * @param key   attribute name (must not be {@code null})
     * @param value attribute value (may be {@code null})
     */
    public static void renewSessionAndPut(HttpServletRequest req, String key, Object value) {
        HttpSession old = req.getSession(false);
        if (old != null) {
            old.invalidate();
        }
        HttpSession fresh = req.getSession(true);
        fresh.setAttribute(Objects.requireNonNull(key), value);
    }

    /**
     * Moves a flash message from session to request scope and removes it from session.
     *
     * @param req request
     * @param key session attribute name
     */
    public static void pullFlash(HttpServletRequest req, String key) {
        HttpSession s = req.getSession(false);
        if (s != null) {
            Object flash = s.getAttribute(key);
            if (flash != null) {
                req.setAttribute(key, flash.toString());
                s.removeAttribute(key);
            }
        }
    }

    /**
     * Resolves a human-readable quest display name by id using the authoring catalog.
     *
     * @param questIdOrNull {@code null} or custom quest id; {@code null} and {@code "main"} return {@code "main"}
     * @param authoring     authoring service (nullable)
     * @return display name, e.g., {@code "Custom quest"} fallback
     */
    public static String displayName(String questIdOrNull, QuestAuthoringService authoring) {
        String qid = (questIdOrNull == null || questIdOrNull.isBlank()) ? "main" : questIdOrNull;
        if ("main".equalsIgnoreCase(qid)) {
            return "main";
        }
        if (authoring != null) {
            return authoring.getFromCatalog(qid)
                    .map(cq -> {
                        String n = cq.getName();
                        return (n == null || n.isBlank()) ? "Custom quest" : n;
                    })
                    .orElse("Custom quest");
        }
        return "Custom quest";
    }

    /**
     * Attaches quest lists and created/updated date maps to the request attributes.
     * <ul>
     *   <li>{@code items} - the list as-is;</li>
     *   <li>{@code createdMap} - questId -&gt; created date;</li>
     *   <li>{@code updatedMap} - questId -&gt; updated date.</li>
     * </ul>
     *
     * @param req   request
     * @param items custom quests
     */
    public static void attachQuestLists(HttpServletRequest req, List<CustomQuest> items) {
        req.setAttribute("items", items);
        Map<String, Date> createdMap = new LinkedHashMap<>();
        Map<String, Date> updatedMap = new LinkedHashMap<>();
        for (CustomQuest q : items) {
            if (q.getCreatedAt() != null) {
                createdMap.put(q.getId(), Date.from(q.getCreatedAt()));
            }
            if (q.getUpdatedAt() != null) {
                updatedMap.put(q.getId(), Date.from(q.getUpdatedAt()));
            }
        }
        req.setAttribute("createdMap", createdMap);
        req.setAttribute("updatedMap", updatedMap);
    }

    /**
     * Retrieves a typed object stored in {@link ServletContext} by key.
     *
     * @param ctx  servlet context
     * @param key  attribute key
     * @param type required type
     * @param <T>  generic type
     * @return the object cast to {@code type}
     * @throws IllegalStateException if not found or of incompatible type
     */
    public static <T> T ctxBean(ServletContext ctx, String key, Class<T> type) {
        Object obj = ctx.getAttribute(key);
        if (type.isInstance(obj)) {
            return type.cast(obj);
        }
        throw new IllegalStateException("Context bean not found: " + key);
    }

    /**
     * Copies selected non-blank request parameters into request attributes.
     *
     * @param req  request
     * @param keys parameter names to copy
     */
    public static void copyParamsToAttrs(HttpServletRequest req, String... keys) {
        for (String key : keys) {
            String val = req.getParameter(key);
            if (val != null && !val.isBlank()) {
                req.setAttribute(key, val);
            }
        }
    }

    /**
     * Produces a short, sentence-like title from a text.
     * <p>Heuristics: prefer first sentence if it's between 20 and 80 chars; otherwise cut to 64 chars.</p>
     *
     * @param s input text
     * @return short title (never {@code null})
     */
    public static String shortTitle(String s) {
        if (s == null) {
            return "";
        }
        s = s.trim();
        if (s.length() <= 64) {
            return s;
        }
        int dot = s.indexOf('.');
        if (dot > 20 && dot < 80) {
            return s.substring(0, dot + 1);
        }
        return s.substring(0, 64) + "…";
    }

    /**
     * Returns {@code true} if the quest nodes list is effectively empty:
     * <ul>
     *   <li>{@code null} or empty;</li>
     *   <li>single node that is {@code fin} and has blank text.</li>
     * </ul>
     *
     * @param nodes quest nodes
     * @return {@code true} if effectively empty
     */
    public static boolean isEffectivelyEmpty(List<QuestNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return true;
        }
        if (nodes.size() == 1) {
            QuestNode n = nodes.getFirst();
            String t = n.getText();
            boolean blank = (t == null || t.isBlank());
            return n.getFin() && blank;
        }
        return false;
    }

    /**
     * Creates a multi-line snippet (max ~120 chars, wrapped to ≤3 lines, ~24 chars each).
     *
     * @param text source text
     * @return wrapped snippet (may be empty string)
     */
    public static String makeSnippet(String text) {
        if (text == null) {
            return "";
        }
        String t = text.replace('\n', ' ').replace('\r', ' ').trim();
        if (t.isEmpty()) {
            return "";
        }
        int hardLimit = Math.min(120, t.length());
        t = t.substring(0, hardLimit);
        List<String> lines = wrapByWords(t);
        return String.join("\n", lines);
    }

    /**
     * Normalizes custom quest id: returns {@code null} for {@code null}/blank or {@code "main"}.
     *
     * @param custom custom id
     * @return normalized id or {@code null}
     */
    public static String normalizeCustom(String custom) {
        if (custom == null || custom.isBlank()) {
            return null;
        }
        return "main".equalsIgnoreCase(custom) ? null : custom;
    }

    /**
     * Reads and normalizes the {@code custom} request parameter.
     *
     * @param req request
     * @return normalized id or {@code null}
     */
    public static String normalizedCustomParam(HttpServletRequest req) {
        return normalizeCustom(req.getParameter(WebConst.Param.CUSTOM));
    }

    /**
     * Checks whether a provided custom quest id is missing from the catalog.
     *
     * @param customId  custom id (nullable)
     * @param authoring authoring service (nullable)
     * @return {@code true} when {@code authoring} is {@code null} or id not found; {@code false} for {@code null} id
     */
    public static boolean isMissingCustomId(String customId, QuestAuthoringService authoring) {
        if (customId == null) {
            return false;
        }
        if (authoring == null) {
            return true;
        }
        return authoring.getFromCatalog(customId).isEmpty();
    }

    /**
     * Wraps text by words into lines limited to ~24 characters, up to 3 lines.
     * Adds ellipsis to the last line if the original text was longer.
     *
     * @param s text to wrap
     * @return list of 1–3 lines
     */
    public static List<String> wrapByWords(String s) {
        String[] words = s.split("\\s+");
        List<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String w : words) {
            String candidate = cur.isEmpty() ? w : (cur + " " + w);
            if (candidate.length() <= 24) {
                cur.setLength(0);
                cur.append(candidate);
            } else {
                if (!cur.isEmpty()) {
                    lines.add(cur.toString());
                    if (lines.size() == 3) break;
                }
                if (w.length() > 24) {
                    lines.add(w.substring(0, 24 - 1) + "…");
                    if (lines.size() == 3) break;
                    cur.setLength(0);
                } else {
                    cur.setLength(0);
                    cur.append(w);
                }
            }
        }
        if (lines.size() < 3 && !cur.isEmpty()) {
            lines.add(cur.toString());
        }
        if (lines.size() == 3 && s.length() > String.join(" ", lines).length()) {
            int last = lines.size() - 1;
            String L = lines.get(last);
            if (!L.endsWith("…")) {
                if (L.length() >= 24) L = L.substring(0, Math.max(0, 24 - 1)) + "…";
                else L = L + "…";
                lines.set(last, L);
            }
        }
        return lines;
    }

    /**
     * Redirects to a path preserving given non-blank request parameters.
     *
     * @param req          request
     * @param resp         response
     * @param pathRelative context-relative path
     * @param paramNames   parameter names to preserve
     * @throws IOException if redirect fails
     */
    public static void redirectKeep(HttpServletRequest req,
                                    HttpServletResponse resp,
                                    String pathRelative,
                                    String... paramNames) throws IOException {
        Map<String, String> p = new LinkedHashMap<>();
        if (paramNames != null) {
            for (String n : paramNames) {
                String v = req.getParameter(n);
                if (v != null && !v.isBlank()) {
                    p.put(n, v);
                }
            }
        }
        redirect(req, resp, pathRelative, p);
    }

    /**
     * Detects whether sensitive fields were changed between two {@link User} instances.
     * <p>Sensitive fields: role, login, password (via {@code passwordChanged}).</p>
     *
     * @param before          previous user state (nullable)
     * @param after           new user state (non-null)
     * @param passwordChanged explicit password change flag
     * @return {@code true} if any sensitive field changed
     */
    public static boolean sensitiveChanged(User before, User after, boolean passwordChanged) {
        if (before == null) {
            return passwordChanged;
        }
        return passwordChanged
                || !Objects.equals(before.getRole(), after.getRole())
                || !Objects.equals(before.getUserLogin(), after.getUserLogin());
    }

    /**
     * Filters quests by name using a request parameter and sets {@code q} attribute.
     *
     * @param req       request
     * @param items     quests to filter
     * @param paramName request parameter name holding the query
     * @return filtered list (original when query is {@code null})
     */
    public static List<CustomQuest> filterQuestsByName(HttpServletRequest req,
                                                       List<CustomQuest> items,
                                                       String paramName) {
        String q = trimOrNull(req.getParameter(paramName));
        req.setAttribute("q", q);
        if (q == null) {
            return items;
        }
        final String qLower = q.toLowerCase(Locale.ROOT);
        return items.stream()
                .filter(it -> {
                    String name = (it == null) ? null : it.getName();
                    return name != null && name.toLowerCase(Locale.ROOT).contains(qLower);
                })
                .collect(Collectors.toList());
    }

    /**
     * Applies filtering by name ({@code q}) and attaches list/date attributes for JSP.
     *
     * @param req   request
     * @param items quests to process
     */
    public static void filterAndAttachQuests(HttpServletRequest req, List<CustomQuest> items) {
        List<CustomQuest> filtered = filterQuestsByName(req, items, "q");
        attachQuestLists(req, filtered);
    }

    /**
     * Builds a machine-readable diff map for admin changes.
     * <ul>
     *   <li>{@code what} - human summary produced by {@link #buildAdminChangeSummary(User, User, boolean)}</li>
     *   <li>Additional keys: {@code role.before/after}, {@code login.before/after}, {@code name.before/after}, {@code password.changed}</li>
     * </ul>
     *
     * @param before          previous user (nullable)
     * @param after           new user (non-null)
     * @param passwordChanged password changed flag
     * @return ordered map with diff data
     */
    public static Map<String, String> buildMachineReadableDiff(User before, User after, boolean passwordChanged) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("what", buildAdminChangeSummary(before, after, passwordChanged));
        if (before != null) {
            if (!Objects.equals(before.getRole(), after.getRole())) {
                data.put("role.before", String.valueOf(before.getRole()));
                data.put("role.after", String.valueOf(after.getRole()));
            }
            if (!Objects.equals(before.getUserLogin(), after.getUserLogin())) {
                data.put("login.before", before.getUserLogin());
                data.put("login.after", after.getUserLogin());
            }
            if (!Objects.equals(before.getUserName(), after.getUserName())) {
                data.put("name.before", before.getUserName());
                data.put("name.after", after.getUserName());
            }
        } else {
            data.put("info", "profile-initialized-by-admin");
        }
        if (passwordChanged) {
            data.put("password.changed", "true");
        }
        return data;
    }

    /**
     * Builds a compact human-readable summary of admin changes.
     *
     * @param before          previous user (nullable)
     * @param after           new user (non-null)
     * @param passwordChanged password changed flag
     * @return short one-line summary (or {@code "No visible changes"})
     */
    public static String buildAdminChangeSummary(User before, User after, boolean passwordChanged) {
        List<String> parts = new ArrayList<>();
        if (before == null) {
            parts.add("Profile fields were updated");
            if (passwordChanged) parts.add("password: changed");
            return String.join("; ", parts);
        }
        if (!Objects.equals(before.getRole(), after.getRole())) {
            parts.add("role: " + before.getRole() + " → " + after.getRole());
        }
        if (!Objects.equals(before.getUserLogin(), after.getUserLogin())) {
            parts.add("login: " + before.getUserLogin() + " → " + after.getUserLogin());
        }
        if (!Objects.equals(before.getUserName(), after.getUserName())) {
            parts.add("name: " + before.getUserName() + " → " + after.getUserName());
        }
        if (passwordChanged) {
            parts.add("password: changed");
        }
        if (parts.isEmpty()) {
            return "No visible changes";
        }
        String joined = String.join("; ", parts);
        return Web.shortTitle(joined);
    }

    /**
     * Appends quest id parameters to a base URL.
     *
     * @param base           base URL
     * @param nodeId         quest node id
     * @param customIdOrNull custom quest id or {@code null}
     * @return resulting URL
     */
    private static String appendQuestParams(String base, int nodeId, String customIdOrNull) {
        Map<String, String> p = new LinkedHashMap<>();
        p.put(WebConst.Param.ID, String.valueOf(nodeId));
        if (customIdOrNull != null && !customIdOrNull.isBlank()) {
            p.put(WebConst.Param.CUSTOM, customIdOrNull);
        }
        return addParamsEncoded(base, p);
    }

    /**
     * Builds and attaches an SVG-like model of a quest graph into request attributes.
     * <p>Attributes set: {@code width}, {@code height}, {@code nodeW}, {@code nodeH}, {@code positions}, {@code edges}, {@code isEmpty}.</p>
     * <p>When nodes are empty/effectively empty, sets a minimal canvas and marks {@code isEmpty=true}.</p>
     *
     * @param req           request
     * @param nodes         quest nodes
     * @param startId       start node id
     * @param includeImages whether to include node image URLs in positions
     */
    public static void buildQuestSvgModel(HttpServletRequest req, List<QuestNode> nodes, int startId, boolean includeImages) {
        buildQuestSvgModel(req, nodes, startId, includeImages, 180, 80, 80, 120, 40);
    }

    /**
     * Builds and attaches an SVG-like model of a quest graph into request attributes with layout parameters.
     *
     * @param req           request
     * @param nodes         quest nodes
     * @param startId       start node id
     * @param includeImages whether to include node image URLs in positions
     * @param nodeW         node width (px)
     * @param nodeH         node height (px)
     * @param hGap          horizontal gap between nodes (px)
     * @param vGap          vertical gap between layers (px)
     * @param padding       canvas padding (px)
     */
    public static void buildQuestSvgModel(HttpServletRequest req,
                                          List<QuestNode> nodes,
                                          int startId,
                                          boolean includeImages,
                                          int nodeW, int nodeH,
                                          int hGap, int vGap,
                                          int padding) {
        if (nodes == null || nodes.isEmpty() || isEffectivelyEmpty(nodes)) {
            req.setAttribute("width", 800);
            req.setAttribute("height", 300);
            req.setAttribute("nodeW", nodeW);
            req.setAttribute("nodeH", nodeH);
            req.setAttribute("positions", List.<NodePos>of());
            req.setAttribute("edges", List.<EdgeSeg>of());
            req.setAttribute("isEmpty", Boolean.TRUE);
            return;
        }
        Map<Integer, QuestNode> byId = new HashMap<>();
        for (QuestNode n : nodes) {
            if (n != null) byId.put(n.getId(), n);
        }
        Map<Integer, Integer> dist = new HashMap<>();
        Deque<Integer> dq = new ArrayDeque<>();
        if (byId.containsKey(startId)) {
            dist.put(startId, 0);
            dq.add(startId);
        }
        while (!dq.isEmpty()) {
            int v = dq.pollFirst();
            QuestNode qn = byId.get(v);
            if (qn == null || qn.getFin()) {
                continue;
            }
            for (Option o : qn.getOptions()) {
                Integer to = (o == null) ? null : o.getNext();
                if (to == null || !byId.containsKey(to)) {
                    continue;
                }
                if (!dist.containsKey(to)) {
                    dist.put(to, dist.get(v) + 1);
                    dq.addLast(to);
                }
            }
        }
        Map<Integer, List<Integer>> layers = new TreeMap<>();
        for (var e : dist.entrySet()) {
            layers.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
        }
        List<Integer> unreachable = new ArrayList<>();
        for (QuestNode n : nodes) {
            if (n != null && !dist.containsKey(n.getId())) {
                unreachable.add(n.getId());
            }
        }
        int lastLayerIndex = layers.isEmpty() ? 0 : Collections.max(layers.keySet()) + 1;
        if (!unreachable.isEmpty()) {
            layers.put(lastLayerIndex, unreachable);
        }
        for (List<Integer> l : layers.values()) {
            l.sort(Integer::compare);
        }
        int colsMax = 1;
        for (List<Integer> l : layers.values()) {
            colsMax = Math.max(colsMax, l.size());
        }
        int width = padding * 2 + colsMax * nodeW + Math.max(0, colsMax - 1) * hGap;
        int height = padding * 2 + layers.size() * nodeH + Math.max(0, layers.size() - 1) * vGap;
        Map<Integer, NodePos> pos = new HashMap<>();
        int row = 0;
        for (List<Integer> l : layers.values()) {
            int count = l.size();
            if (count == 0) {
                row++;
                continue;
            }
            int rowWidth = count * nodeW + (count - 1) * hGap;
            int x0 = (width - rowWidth) / 2;
            int y = padding + row * (nodeH + vGap);
            for (int i = 0; i < count; i++) {
                int id = l.get(i);
                QuestNode qn = byId.get(id);
                String snippet = makeSnippet(qn == null ? null : qn.getText());
                List<String> labelLines = Arrays.asList(snippet.split("\n", -1));
                String img = (includeImages && qn != null) ? qn.getImage() : null;
                pos.put(
                        id,
                        new NodePos(
                                id,
                                x0 + i * (nodeW + hGap),
                                y,
                                qn != null && qn.getFin(),
                                id == startId,
                                labelLines,
                                img
                        )
                );
            }
            row++;
        }
        List<EdgeSeg> edges = new ArrayList<>();
        double pad = 12.0;
        for (QuestNode from : nodes) {
            if (from == null || from.getFin()) {
                continue;
            }
            NodePos npFrom = pos.get(from.getId());
            if (npFrom == null) {
                continue;
            }
            double cx1 = npFrom.getX() + nodeW / 2.0;
            double cy1 = npFrom.getY() + nodeH / 2.0;
            for (Option o : from.getOptions()) {
                Integer to = (o == null) ? null : o.getNext();
                if (to == null) {
                    continue;
                }
                NodePos npTo = pos.get(to);
                if (npTo == null) {
                    continue;
                }
                double cx2 = npTo.getX() + nodeW / 2.0;
                double cy2 = npTo.getY() + nodeH / 2.0;
                double dx = cx2 - cx1, dy = cy2 - cy1, len = Math.hypot(dx, dy);
                if (len == 0) {
                    len = 1;
                }
                double nx = dx / len, ny = dy / len;
                double sx = cx1 + nx * (nodeW / 2.0 - pad);
                double sy = cy1 + ny * (nodeH / 2.0 - pad);
                double tx = cx2 - nx * (nodeW / 2.0 - pad);
                double ty = cy2 - ny * (nodeH / 2.0 - pad);
                edges.add(new EdgeSeg(from.getId(), to, o.getChoice(), sx, sy, tx, ty));
            }
        }
        req.setAttribute("width", width);
        req.setAttribute("height", height);
        req.setAttribute("nodeW", nodeW);
        req.setAttribute("nodeH", nodeH);
        req.setAttribute("positions", pos.values());
        req.setAttribute("edges", edges);
        req.setAttribute("isEmpty", Boolean.FALSE);
    }
}
