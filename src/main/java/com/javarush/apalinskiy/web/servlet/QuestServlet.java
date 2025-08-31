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

import java.io.IOException;

public class QuestServlet extends HttpServlet {

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
        String idParam = req.getParameter("id");
        Integer id = null;
        if (idParam != null && !idParam.isBlank()) {
            try {
                id = Integer.valueOf(idParam.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        QuestNode node;
        if (id == null) {
            node = service.getStart();
        } else {
            node = service.getById(id);
            if (node == null) {
                req.setAttribute("error", "Узел не найден: id=" + idParam);
                node = service.getStart();
            }
        }
        forwardQuest(req, resp, node, null);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String fromParam = req.getParameter("fromId");
        Integer fromId = null;
        if (fromParam != null && !fromParam.isBlank()) {
            try {
                fromId = Integer.valueOf(fromParam.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        if (fromId == null) {
            forwardQuest(req, resp, service.getStart(), "Некорректный fromId");
            return;
        }
        String answer = req.getParameter("answer");
        ChooseResult result = service.choose(fromId, answer);
        if (result.isOk()) {
            int nextId = result.getNext().getId();
            resp.sendRedirect(resp.encodeRedirectURL(req.getContextPath() + "/quest?id=" + nextId)); // PRG
        } else {
            QuestNode node = service.getById(fromId);
            if (node == null) {
                node = service.getStart();
            }
            forwardQuest(req, resp, node, result.getMessage());
        }
    }

    private void forwardQuest(HttpServletRequest req, HttpServletResponse resp,
                              QuestNode node, String error) throws ServletException, IOException {
        req.setAttribute("node", node);
        req.setAttribute("version", service.version());
        if (error != null && !error.isBlank()) {
            req.setAttribute("error", error);
        }
        req.getRequestDispatcher("/WEB-INF/jsp/quest.jsp").forward(req, resp);
    }
}
