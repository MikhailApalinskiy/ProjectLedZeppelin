package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.quest.model.QuestNode;
import com.javarush.apalinskiy.service.QuestService;
import com.javarush.apalinskiy.service.dto.ChooseResult;
import com.javarush.apalinskiy.web.listener.AppBootstrap;
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

public class QuestServlet extends HttpServlet {

    private static final String PATH_QUEST = "/quest";
    private static final String JSP_QUEST = "/WEB-INF/jsp/quest.jsp";
    private static final String P_ID = "id";
    private static final String P_NODE = "node";
    private static final String P_FROM_ID = "fromId";
    private static final String P_ANSWER = "answer";
    private static final String A_NODE = "node";
    private static final String A_VERSION = "version";
    private static final String A_ERROR = "error";
    private static final String A_FLASH = "flash";
    private static final String MSG_BAD_FROM_ID = "Некорректный fromId";
    private static final String MSG_NODE_NOT_FOUND_PREFIX = "Узел не найден: id=";

    private transient QuestService service;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        ServletContext ctx = config.getServletContext();
        Object attr = ctx.getAttribute(AppBootstrap.ATTR_QUEST_SERVICE);
        if (!(attr instanceof QuestService qs)) {
            throw new UnavailableException("QuestService is not initialized in ServletContext");
        }
        this.service = qs;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Integer id = firstIntParam(req, P_ID, P_NODE);
        QuestNode node = (id == null) ? service.getStart() : service.getById(id);
        if (node == null) {
            req.setAttribute(A_ERROR, MSG_NODE_NOT_FOUND_PREFIX + id);
            node = service.getStart();
        }
        HttpSession s = req.getSession(false);
        if (s != null) {
            Object flash = s.getAttribute(A_FLASH);
            if (flash != null) {
                req.setAttribute(A_FLASH, flash.toString());
                s.removeAttribute(A_FLASH);
            }
        }
        forwardQuest(req, resp, node, null);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Integer fromId = parseIntOrNull(req.getParameter(P_FROM_ID));
        if (fromId == null) {
            forwardQuest(req, resp, service.getStart(), MSG_BAD_FROM_ID);
            return;
        }
        String answer = req.getParameter(P_ANSWER);
        ChooseResult result = service.choose(fromId, answer);
        if (result.isOk()) {
            int nextId = result.getNext().getId();
            resp.sendRedirect(resp.encodeRedirectURL(questUrl(req, nextId)));
        } else {
            QuestNode node = service.getById(fromId);
            if (node == null) node = service.getStart();
            forwardQuest(req, resp, node, result.getMessage());
        }
    }

    private void forwardQuest(HttpServletRequest req, HttpServletResponse resp,
                              QuestNode node, String error) throws ServletException, IOException {
        req.setAttribute(A_NODE, node);
        req.setAttribute(A_VERSION, service.version());
        if (error != null && !error.isBlank()) {
            req.setAttribute(A_ERROR, error);
        }
        req.getRequestDispatcher(JSP_QUEST).forward(req, resp);
    }

    private static Integer firstIntParam(HttpServletRequest req, String... names) {
        for (String n : names) {
            Integer v = parseIntOrNull(req.getParameter(n));
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    private static Integer parseIntOrNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(s.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String questUrl(HttpServletRequest req, int id) {
        String base = req.getContextPath() + PATH_QUEST;
        String q = P_ID + "=" + urlEncode(String.valueOf(id));
        return base + "?" + q;
    }

    private static String urlEncode(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }
}