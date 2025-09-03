package com.javarush.apalinskiy.web.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Optional;

public class GoSavesServlet extends AbstractSlotsServlet {

    private static final String PATH = "/saves";
    private static final String JSP = "/WEB-INF/jsp/saves.jsp";
    private static final String JSP_CONFIRM = "/WEB-INF/jsp/confirm-overwrite.jsp";

    @Override
    protected String path() {
        return PATH;
    }

    @Override
    protected String listJsp() {
        return JSP;
    }

    @Override
    protected String confirmJsp() {
        return JSP_CONFIRM;
    }

    @Override
    protected void handleGo(HttpServletRequest req, HttpServletResponse resp,
                            String userId, int slot, String next)
            throws IOException, ServletException {
        int nodeId = parseInt(req.getParameter(P_NODE));
        if (nodeId <= 0 || questService.getById(nodeId) == null) nodeId = startId();
        Optional<Integer> existing = saveState.getSlot(userId, slot);
        if (existing.isPresent()) {
            int oldId = existing.get();
            req.setAttribute("slotIndex", slot);
            req.setAttribute("newNodeId", nodeId);
            req.setAttribute("newNodeTitle", titleFor(nodeId));
            req.setAttribute("oldNodeId", oldId);
            req.setAttribute("oldNodeTitle", titleFor(oldId));
            req.setAttribute("next", next);
            req.setAttribute("purpose", Optional.ofNullable(req.getParameter(P_PURPOSE)).orElse("save"));
            req.getRequestDispatcher(confirmJsp()).forward(req, resp);
            return;
        }
        saveState.setSlot(userId, slot, nodeId);
        resp.sendRedirect(resp.encodeRedirectURL(buildQuestUrl(req, next, nodeId)));
    }

    @Override
    protected void handleConfirm(HttpServletRequest req, HttpServletResponse resp,
                                 String userId, int slot, String next)
            throws IOException {
        int nodeId = parseInt(req.getParameter(P_NODE));
        if (nodeId <= 0 || questService.getById(nodeId) == null) {
            nodeId = startId();
        }
        saveState.setSlot(userId, slot, nodeId);
        req.getSession().setAttribute("flash", "Слот №" + (slot + 1) + " перезаписан.");
        resp.sendRedirect(resp.encodeRedirectURL(buildQuestUrl(req, next, nodeId)));
    }

    @Override
    protected void handleDelete(HttpServletRequest req, HttpServletResponse resp,
                                String userId, int slot)
            throws IOException {
        saveState.clearSlot(userId, slot);
        req.getSession().setAttribute("flash", "Слот №" + (slot + 1) + " удалён.");
        String back = backToList(req, path());
        resp.sendRedirect(resp.encodeRedirectURL(back));
    }
}
