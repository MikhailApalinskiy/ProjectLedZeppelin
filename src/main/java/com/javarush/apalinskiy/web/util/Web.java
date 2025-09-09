package com.javarush.apalinskiy.web.util;

import com.javarush.apalinskiy.application.quests.QuestAuthoringService;
import com.javarush.apalinskiy.domain.quest.Option;
import com.javarush.apalinskiy.domain.quest.QuestNode;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.quest.CustomQuest;
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

    public static String questUrl(HttpServletRequest req, int id, String customIdOrNull) {
        String base = req.getContextPath() + WebConst.Path.QUEST;
        return appendQuestParams(base, id, customIdOrNull);
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

    public static String normalizeCustom(String custom) {
        if (custom == null || custom.isBlank()) {
            return null;
        }
        return "main".equalsIgnoreCase(custom) ? null : custom;
    }

    public static String normalizedCustomParam(HttpServletRequest req) {
        return normalizeCustom(req.getParameter(WebConst.Param.CUSTOM));
    }

    public static boolean isMissingCustomId(String customId, QuestAuthoringService authoring) {
        if (customId == null) {
            return false;
        }
        if (authoring == null) {
            return true;
        }
        return authoring.getFromCatalog(customId).isEmpty();
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

    public static boolean sensitiveChanged(User before, User after, boolean passwordChanged) {
        if (before == null) {
            return passwordChanged;
        }
        return passwordChanged
                || !Objects.equals(before.getRole(), after.getRole())
                || !Objects.equals(before.getUserLogin(), after.getUserLogin());
    }

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

    public static void filterAndAttachQuests(HttpServletRequest req, List<CustomQuest> items) {
        List<CustomQuest> filtered = filterQuestsByName(req, items, "q");
        attachQuestLists(req, filtered);
    }

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

    private static String appendQuestParams(String base, int nodeId, String customIdOrNull) {
        Map<String, String> p = new LinkedHashMap<>();
        p.put(WebConst.Param.ID, String.valueOf(nodeId));
        if (customIdOrNull != null && !customIdOrNull.isBlank()) {
            p.put(WebConst.Param.CUSTOM, customIdOrNull);
        }
        return addParamsEncoded(base, p);
    }

    public static void buildQuestSvgModel(HttpServletRequest req, List<QuestNode> nodes, int startId, boolean includeImages) {
        buildQuestSvgModel(req, nodes, startId, includeImages, 180, 80, 80, 120, 40);
    }

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
