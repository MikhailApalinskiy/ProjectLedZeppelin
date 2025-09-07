package com.javarush.apalinskiy.web.util;

import com.javarush.apalinskiy.application.quests.QuestAuthoringService;
import com.javarush.apalinskiy.domain.quest.QuestNode;
import com.javarush.apalinskiy.quest.CustomQuest;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Web {

    private Web() {
    }

    public static String trimOrNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

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

    public static int parseIntOrDefault(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return def;
        }
    }

    public static Integer firstIntParam(HttpServletRequest req, String... names) {
        for (String n : names) {
            Integer v = parseIntOrNull(req.getParameter(n));
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    public static String urlEncode(String v) {
        try {
            return URLEncoder.encode(v == null ? "" : v, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return v;
        }
    }

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

    public static String buildQuestUrlFromNext(HttpServletRequest req, String next, int nodeId) {
        String base = req.getContextPath() + ((next == null || next.isBlank()) ? WebConst.Path.QUEST : next);
        return addParamsEncoded(base, Map.of(WebConst.Param.ID, String.valueOf(nodeId)));
    }

    public static String questUrl(HttpServletRequest req, int id, String customIdOrNull) {
        String base = req.getContextPath() + WebConst.Path.QUEST;
        Map<String, String> p = new LinkedHashMap<>();
        p.put(WebConst.Param.ID, String.valueOf(id));
        if (customIdOrNull != null && !customIdOrNull.isBlank()) {
            p.put(WebConst.Param.CUSTOM, customIdOrNull);
        }
        return addParamsEncoded(base, p);
    }

    public static void forward(HttpServletRequest req, HttpServletResponse resp, String jsp)
            throws ServletException, IOException {
        req.getRequestDispatcher(jsp).forward(req, resp);
    }

    public static String safeNextOrHome(HttpServletRequest req, String next) {
        return isSafeNext(req, next) ? next : (req.getContextPath() + WebConst.Path.HOME);
    }

    public static boolean isSafeNext(HttpServletRequest req, String next) {
        if (next == null || next.isBlank()) {
            return false;
        }
        String ctx = req.getContextPath();
        return next.startsWith(ctx + "/");
    }

    public static void redirect(HttpServletRequest req, HttpServletResponse resp, String path, Map<String, String> params) throws IOException {
        String url = addParamsEncoded(req.getContextPath() + path, params);
        resp.sendRedirect(resp.encodeRedirectURL(url));
    }

    public static void redirectOk(HttpServletRequest req, HttpServletResponse resp, String path, String msg) throws IOException {
        redirect(req, resp, path, Map.of(WebConst.Attr.OK, Objects.toString(msg, "")));
    }

    public static void redirectErr(HttpServletRequest req, HttpServletResponse resp, String path, String msg) throws IOException {
        redirect(req, resp, path, Map.of(WebConst.Attr.ERROR, Objects.toString(msg, "")));
    }

    public static void renewSessionAndPut(HttpServletRequest req, String key, Object value) {
        HttpSession old = req.getSession(false);
        if (old != null) {
            old.invalidate();
        }
        HttpSession fresh = req.getSession(true);
        fresh.setAttribute(Objects.requireNonNull(key), value);
    }

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

    public static void redirectWith(HttpServletResponse resp, String path, String key, String msg) throws IOException {
        String encoded = urlEncode(Objects.toString(msg, ""));
        resp.sendRedirect(resp.encodeRedirectURL(path + "?" + key + "=" + encoded));
    }

    public static <T> T ctxBean(ServletContext ctx, String key, Class<T> type) {
        Object obj = ctx.getAttribute(key);
        if (type.isInstance(obj)) {
            return type.cast(obj);
        }
        throw new IllegalStateException("Context bean not found: " + key);
    }

    public static void copyParamsToAttrs(HttpServletRequest req, String... keys) {
        for (String key : keys) {
            String val = req.getParameter(key);
            if (val != null && !val.isBlank()) {
                req.setAttribute(key, val);
            }
        }
    }

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
}
