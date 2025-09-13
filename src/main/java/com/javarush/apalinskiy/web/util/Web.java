package com.javarush.apalinskiy.web.util;

import com.javarush.apalinskiy.app.WebConst;
import com.javarush.apalinskiy.service.quest.QuestAuthoringService;
import com.javarush.apalinskiy.domain.quest.Option;
import com.javarush.apalinskiy.domain.quest.QuestNode;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.domain.quest.custom.CustomQuest;
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
 * Web utility helpers for servlet/JSP layer.
 * <p>
 * Contains small stateless helpers for parsing/normalizing parameters,
 * building URLs, forwarding/redirecting, session/flash handling, simple
 * string utilities, and preparing view models for JSPs.
 * </p>
 *
 * <h3>Thread-safety</h3>
 * <p>Class is stateless and all methods are static.</p>
 */
public class Web {

    private Web() {
    }

    /**
     * Trims a string and returns {@code null} if it becomes empty.
     *
     * @param s input string (nullable)
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
     * Parses an integer or returns {@code null} if blank/invalid.
     *
     * @param s raw string
     * @return {@link Integer} value or {@code null} on failure
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
     * Parses an integer or returns a default value on error.
     *
     * @param s   raw string
     * @param def default value
     * @return parsed integer or {@code def} if parsing fails
     */
    public static int parseIntOrDefault(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return def;
        }
    }

    /**
     * Returns the first non-null integer parameter from the request among the given names.
     *
     * @param req   request
     * @param names candidate parameter names (checked in order)
     * @return first parsed integer or {@code null} if none present/parsable
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
     * URL-encodes a value using UTF-8 (null becomes empty string).
     *
     * @param v value to encode
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
     * Appends query parameters (already properly encoded by this method) to a base URL.
     *
     * @param base   base URL (may already contain {@code ?})
     * @param params map of params (keys/values will be UTF-8 encoded)
     * @return URL with appended parameters
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
     * Builds a URL by taking selected request parameters and appending them to {@code base}.
     *
     * @param req   request
     * @param base  base URL (absolute or context-relative)
     * @param names parameter names to copy if present and non-blank
     * @return URL with encoded parameters
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
     * Builds a quest URL based on the "next" path (if provided) and a node id,
     * optionally including a custom quest id (taken from request param/attr).
     *
     * @param req    request
     * @param next   optional next path (falls back to {@link WebConst.Path#QUEST})
     * @param nodeId node id to include in query
     * @return fully built context-relative URL
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
     * Builds a canonical quest URL {@code /quest?id=... [&custom=...]}.
     *
     * @param req            request
     * @param id             node id
     * @param customIdOrNull optional custom quest id
     * @return URL string
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
     * @param jsp  JSP path (typically under {@code /WEB-INF/jsp/...})
     * @throws ServletException on forward error
     * @throws IOException      on I/O error
     */
    public static void forward(HttpServletRequest req, HttpServletResponse resp, String jsp)
            throws ServletException, IOException {
        req.getRequestDispatcher(jsp).forward(req, resp);
    }

    /**
     * Returns {@code next} if it is a safe, same-context URL (starts with {@code ctx + "/"}),
     * otherwise returns a link to home.
     *
     * @param req  request (used to get context path)
     * @param next candidate redirect target
     * @return safe next or home URL
     */
    public static String safeNextOrHome(HttpServletRequest req, String next) {
        return isSafeNext(req, next) ? next : (req.getContextPath() + WebConst.Path.HOME);
    }

    /**
     * Checks that {@code next} is a safe, same-context URL.
     *
     * @param req  request
     * @param next candidate URL
     * @return {@code true} if safe and context-relative
     */
    public static boolean isSafeNext(HttpServletRequest req, String next) {
        if (next == null || next.isBlank()) {
            return false;
        }
        String ctx = req.getContextPath();
        return next.startsWith(ctx + "/");
    }

    /**
     * Sends a redirect to the given context-relative path with query parameters.
     *
     * @param req    request
     * @param resp   response
     * @param path   context-relative servlet path
     * @param params parameters to append
     * @throws IOException on I/O error
     */
    public static void redirect(HttpServletRequest req, HttpServletResponse resp, String path, Map<String, String> params) throws IOException {
        String url = addParamsEncoded(req.getContextPath() + path, params);
        resp.sendRedirect(resp.encodeRedirectURL(url));
    }

    /**
     * Redirects with an OK flash message.
     *
     * @param req  request
     * @param resp response
     * @param path path to redirect to
     * @param msg  message to include under {@link WebConst.Attr#OK}
     * @throws IOException on I/O error
     */
    public static void redirectOk(HttpServletRequest req, HttpServletResponse resp, String path, String msg) throws IOException {
        redirect(req, resp, path, Map.of(WebConst.Attr.OK, Objects.toString(msg, "")));
    }

    /**
     * Redirects with an error flash message.
     *
     * @param req  request
     * @param resp response
     * @param path path to redirect to
     * @param msg  message to include under {@link WebConst.Attr#ERROR}
     * @throws IOException on I/O error
     */
    public static void redirectErr(HttpServletRequest req, HttpServletResponse resp, String path, String msg) throws IOException {
        redirect(req, resp, path, Map.of(WebConst.Attr.ERROR, Objects.toString(msg, "")));
    }

    /**
     * Invalidates current session (if any), creates a new one and stores an attribute in it.
     * <p>Useful after login for session fixation protection.</p>
     *
     * @param req   request
     * @param key   attribute name (must not be null)
     * @param value attribute value
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
     * Moves a flash attribute from session to request and removes it from session.
     *
     * @param req request
     * @param key flash key (e.g. {@link WebConst.Attr#OK} / {@link WebConst.Attr#ERROR})
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
     * Resolves a user-friendly quest display name for a given quest id.
     * <ul>
     *   <li>{@code null} or {@code "main"} → returns {@code "main"}.</li>
     *   <li>Otherwise tries to read from catalog via {@code authoring}.</li>
     * </ul>
     *
     * @param questIdOrNull quest id or null
     * @param authoring     authoring service (nullable)
     * @return display name
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
     * Attaches quest list and created/updated date maps as request attributes for JSPs.
     * <ul>
     *   <li>{@code "items"} → original list</li>
     *   <li>{@code "createdMap"} and {@code "updatedMap"}</li>
     * </ul>
     *
     * @param req   request
     * @param items quests to attach
     */
    public static void attachQuestLists(HttpServletRequest req, List<CustomQuest> items) {
        req.setAttribute("items", items);
        Map<String, java.util.Date> createdMap = new LinkedHashMap<>();
        Map<String, java.util.Date> updatedMap = new LinkedHashMap<>();
        for (CustomQuest q : items) {
            if (q.getCreatedAt() != null) {
                createdMap.put(q.getId(), java.util.Date.from(q.getCreatedAt()));
            }
            if (q.getUpdatedAt() != null) {
                updatedMap.put(q.getId(), java.util.Date.from(q.getUpdatedAt()));
            }
        }
        req.setAttribute("createdMap", createdMap);
        req.setAttribute("updatedMap", updatedMap);
    }

    /**
     * Fetches a context attribute and casts it, or throws if missing/wrong type.
     *
     * @param ctx  servlet context
     * @param key  attribute key
     * @param type expected type
     * @return attribute value
     * @throws IllegalStateException if not found or wrong type
     */
    public static <T> T ctxBean(ServletContext ctx, String key, Class<T> type) {
        Object obj = ctx.getAttribute(key);
        if (type.isInstance(obj)) {
            return type.cast(obj);
        }
        throw new IllegalStateException("Context bean not found: " + key);
    }

    /**
     * Copies non-blank request parameters into request attributes (same keys).
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
     * Produces a short, one-line title: up to 64 chars or first sentence (between 20..80 chars).
     * Adds an ellipsis if trimmed.
     *
     * @param s source string
     * @return short title (never null)
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
     * Heuristically checks whether a quest is effectively empty (no nodes or single blank final node).
     *
     * @param nodes quest nodes
     * @return {@code true} if empty/effectively empty
     */
    public static boolean isEffectivelyEmpty(List<QuestNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return true;
        }
        if (nodes.size() == 1) {
            QuestNode n = nodes.getFirst();
            String t = n.getText();
            boolean blank = (t == null || t.isBlank());
            return n.isFin() && blank;
        }
        return false;
    }

    /**
     * Builds a short multi-line snippet (wrapped by words to ~3 lines, ~120 chars total).
     * Newlines are removed and CR/LF are normalized to spaces.
     *
     * @param text source text
     * @return snippet (may be empty string)
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
     * Normalizes a "custom" quest id: returns {@code null} if blank or equals {@code "main"}.
     *
     * @param custom raw custom value
     * @return normalized value or {@code null}
     */
    public static String normalizeCustom(String custom) {
        if (custom == null || custom.isBlank()) {
            return null;
        }
        return "main".equalsIgnoreCase(custom) ? null : custom;
    }

    /**
     * Reads and normalizes the {@code custom} request parameter via {@link #normalizeCustom(String)}.
     *
     * @param req request
     * @return normalized custom id or {@code null}
     */
    public static String normalizedCustomParam(HttpServletRequest req) {
        return normalizeCustom(req.getParameter(WebConst.Param.CUSTOM));
    }

    /**
     * Checks if a provided custom quest id is missing from the catalog.
     *
     * @param customId  custom quest id (nullable)
     * @param authoring authoring service (nullable)
     * @return {@code true} if a non-null id is not found (or authoring is null)
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
     * Wraps a string into lines up to 24 chars, trying to break on word boundaries.
     * Produces up to 3 lines, adding ellipsis to the last if truncated.
     *
     * @param s text
     * @return list of wrapped lines (size 1..3, may be empty if input blank)
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
     * Redirects to a path keeping selected request parameters.
     *
     * @param req          request
     * @param resp         response
     * @param pathRelative context-relative path
     * @param paramNames   names to keep if present and non-blank
     * @throws IOException on I/O error
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
     * Determines whether sensitive user fields changed (role/login and/or password flag).
     *
     * @param before          old user (nullable)
     * @param after           new user (non-null)
     * @param passwordChanged whether password changed
     * @return {@code true} if role/login changed or password changed flag is set
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
     * Filters a list of quests by name substring from a request parameter.
     * Also sets the used query parameter value as request attribute {@code "q"}.
     *
     * @param req       request
     * @param items     input list (may be returned as-is)
     * @param paramName name of the filter parameter
     * @return filtered list (non-null)
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
     * Filters quests by request parameter {@code q} and attaches lists/maps for JSPs.
     *
     * @param req   request
     * @param items source items
     */
    public static void filterAndAttachQuests(HttpServletRequest req, List<CustomQuest> items) {
        List<CustomQuest> filtered = filterQuestsByName(req, items, "q");
        attachQuestLists(req, filtered);
    }

    /**
     * Builds a machine-readable diff of admin user changes for notifications.
     * <p>Includes keys like {@code role.before}, {@code role.after}, etc., and a {@code what} summary.</p>
     *
     * @param before          previous user (nullable)
     * @param after           new user
     * @param passwordChanged whether password changed
     * @return map of change summary data
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
     * Builds a short, human-readable summary of admin changes (role/login/name/password).
     *
     * @param before          previous user (nullable)
     * @param after           new user
     * @param passwordChanged whether password changed
     * @return compact summary line(s)
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
     * Appends quest parameters ({@code id} and optional {@code custom}) to a base URL.
     * <p>Private helper used by URL builders.</p>
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
     * Builds an SVG-layout model for a quest graph and attaches it to the request attributes
     * (dimensions, node positions, edges, flags). Provides an overload with default sizes.
     *
     * @param req           request to receive attributes
     * @param nodes         quest nodes (may be empty)
     * @param startId       start node id
     * @param includeImages whether to include node image URLs in the model
     */
    public static void buildQuestSvgModel(HttpServletRequest req, List<QuestNode> nodes, int startId, boolean includeImages) {
        buildQuestSvgModel(req, nodes, startId, includeImages, 180, 80, 80, 120, 40);
    }

    /**
     * Builds an SVG-layout model for a quest graph with explicit geometry parameters.
     * <ul>
     *   <li>Attributes set: {@code width}, {@code height}, {@code nodeW}, {@code nodeH},
     *   {@code positions} (collection of node positions), {@code edges} (edge segments),
     *   {@code isEmpty}.</li>
     *   <li>Unreachable nodes are placed in the last layer.</li>
     * </ul>
     *
     * @param req           request
     * @param nodes         quest nodes
     * @param startId       start node id
     * @param includeImages include node images in positions
     * @param nodeW         node width
     * @param nodeH         node height
     * @param hGap          horizontal gap
     * @param vGap          vertical gap
     * @param padding       canvas padding
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
            if (qn == null || qn.isFin()) {
                continue;
            }
            for (Option o : qn.getOptions()) {
                Integer to = (o == null) ? null : o.next();
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
                                qn != null && qn.isFin(),
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
            if (from == null || from.isFin()) {
                continue;
            }
            NodePos npFrom = pos.get(from.getId());
            if (npFrom == null) {
                continue;
            }
            double cx1 = npFrom.getX() + nodeW / 2.0;
            double cy1 = npFrom.getY() + nodeH / 2.0;
            for (Option o : from.getOptions()) {
                Integer to = (o == null) ? null : o.next();
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
                edges.add(new EdgeSeg(from.getId(), to, o.choice(), sx, sy, tx, ty));
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
