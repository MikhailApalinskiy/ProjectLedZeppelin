package com.javarush.apalinskiy.web.servlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Optional;

public class GoLoadsServlet extends AbstractSlotsServlet {

    private static final String PATH = "/loads";
    private static final String JSP = "/WEB-INF/jsp/loads.jsp";

    @Override
    protected String path() {
        return PATH;
    }

    @Override
    protected String listJsp() {
        return JSP;
    }

    @Override
    protected void handleGo(HttpServletRequest req, HttpServletResponse resp,
                            String userId, int slot, String next)
            throws IOException {
        Optional<Integer> opt = saveState.getSlot(userId, slot);
        if (opt.isEmpty()) {
            String back = backToList(req, path());
            resp.sendRedirect(resp.encodeRedirectURL(back));
            return;
        }
        int nodeId = opt.get();
        if (questService.getById(nodeId) == null) {
            nodeId = startId();
            saveState.setSlot(userId, slot, nodeId);
        }
        String target = buildQuestUrl(req, next, nodeId);
        resp.sendRedirect(resp.encodeRedirectURL(target));
    }

    @Override
    protected void handleDelete(HttpServletRequest req, HttpServletResponse resp,
                                String userId, int slot)
            throws IOException {
        saveState.clearSlot(userId, slot);
        String back = backToList(req, path());
        resp.sendRedirect(resp.encodeRedirectURL(back));
    }
}
