package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.quest.model.QuestNode;
import com.javarush.apalinskiy.service.QuestService;
import com.javarush.apalinskiy.service.SaveStateService;
import com.javarush.apalinskiy.user.User;
import com.javarush.apalinskiy.web.listener.AppBootstrap;
import com.javarush.apalinskiy.web.view.SlotView;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class BaseSaveServlet extends HttpServlet {

    protected static final int SLOT_COUNT = 10;
    protected static final String QUEST_PATH = "/quest";
    protected static final String P_OP = "op";
    protected static final String P_SLOT = "slot";
    protected static final String P_NEXT = "next";
    protected static final String P_NODE = "node";
    protected static final String P_PURPOSE = "purpose";
    protected static final String OP_GO = "go";
    protected static final String OP_DELETE = "delete";
    protected static final String OP_CONFIRM = "confirm";
    protected static final String OP_CANCEL = "cancel";
    protected transient SaveStateService saveState;
    protected transient QuestService questService;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        ServletContext ctx = config.getServletContext();
        Object qs = ctx.getAttribute(AppBootstrap.ATTR_QUEST_SERVICE);
        if (!(qs instanceof QuestService)) {
            throw new UnavailableException("QuestService not initialized");
        }
        this.questService = (QuestService) qs;
        Object ss = ctx.getAttribute(AppBootstrap.ATTR_SAVE_STATE_SERVICE);
        if (!(ss instanceof SaveStateService)) {
            throw new UnavailableException("SaveStateService not initialized");
        }
        this.saveState = (SaveStateService) ss;
    }

    protected User requireAuthOrRedirect(HttpServletRequest req, HttpServletResponse resp, String returnPath) throws IOException {
        User u = (User) req.getSession().getAttribute("user");
        if (u == null) {
            String next = req.getContextPath() + (returnPath.startsWith("/") ? returnPath : ("/" + returnPath));
            String loginUrl = req.getContextPath() + "/login?next=" + urlEncode(next);
            resp.sendRedirect(resp.encodeRedirectURL(loginUrl));
            return null;
        }
        return u;
    }

    protected void pullFlash(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s != null) {
            Object flash = s.getAttribute("flash");
            if (flash != null) {
                req.setAttribute("flash", flash.toString());
                s.removeAttribute("flash");
            }
        }
    }

    protected int startId() {
        QuestNode s = questService.getStart();
        if (s == null) {
            throw new IllegalStateException("Quest start is null");
        }
        return s.getId();
    }

    protected String titleFor(int nodeId) {
        QuestNode n = questService.getById(nodeId);
        return (n != null) ? shortTitle(n.getText()) : ("Узел #" + nodeId);
    }

    protected String nextOrDefault(HttpServletRequest req) {
        return Optional.ofNullable(req.getParameter(P_NEXT)).orElse(QUEST_PATH);
    }

    protected String buildQuestUrl(HttpServletRequest req, String next, int nodeId) {
        String base = req.getContextPath() + ((next == null || next.isBlank()) ? QUEST_PATH : next);
        return addParamsEncoded(base, Map.of("id", String.valueOf(nodeId)));
    }

    protected String backToList(HttpServletRequest req, String path) {
        return addParamsFromReqEncoded(req, req.getContextPath() + path, P_NEXT, P_PURPOSE, P_NODE);
    }

    protected List<SlotView> buildSlots(String userId) {
        List<SlotView> list = new ArrayList<>(SLOT_COUNT);
        for (int i = 0; i < SLOT_COUNT; i++) {
            Optional<Integer> opt = saveState.getSlot(userId, i);
            if (opt.isPresent()) {
                int nodeId = opt.get();
                QuestNode node = questService.getById(nodeId);
                String title = (node != null) ? shortTitle(node.getText()) : ("Узел #" + nodeId + " (не найден)");
                list.add(SlotView.filled(i, nodeId, title, null, "main"));
            } else {
                list.add(SlotView.empty(i, "main"));
            }
        }
        return list;
    }

    protected static String addParamsFromReqEncoded(HttpServletRequest req, String base, String... names) {
        Map<String, String> m = new LinkedHashMap<>();
        for (String n : names) {
            String v = req.getParameter(n);
            if (v != null && !v.isBlank()) m.put(n, v);
        }
        return addParamsEncoded(base, m);
    }

    protected static String addParamsEncoded(String base, Map<String, String> params) {
        if (params == null || params.isEmpty()) return base;
        StringBuilder sb = new StringBuilder(base);
        String sep = base.contains("?") ? "&" : "?";
        for (Map.Entry<String, String> e : params.entrySet()) {
            sb.append(sep).append(urlEncode(e.getKey())).append("=").append(urlEncode(e.getValue()));
            sep = "&";
        }
        return sb.toString();
    }

    protected static int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception ignore) {
            return -1;
        }
    }

    protected static String shortTitle(String s) {
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

    protected static String urlEncode(String v) {
        try {
            return URLEncoder.encode(v, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return v;
        }
    }
}
