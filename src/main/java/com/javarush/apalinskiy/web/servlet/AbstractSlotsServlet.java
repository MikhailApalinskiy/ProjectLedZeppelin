package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.user.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public abstract class AbstractSlotsServlet extends BaseSaveServlet {

    protected abstract String path();

    protected abstract String listJsp();

    protected String confirmJsp() {
        return null;
    }

    @Override
    protected final void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User u = requireAuthOrRedirect(req, resp, path());
        if (u == null) return;
        pullFlash(req);
        req.setAttribute("slots", buildSlots(String.valueOf(u.getUserId())));
        req.getRequestDispatcher(listJsp()).forward(req, resp);
    }

    @Override
    protected final void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        User u = requireAuthOrRedirect(req, resp, path());
        if (u == null) {
            return;
        }
        final String userId = String.valueOf(u.getUserId());
        final String op = req.getParameter(P_OP);
        final int slot = parseInt(req.getParameter(P_SLOT));
        final String next = nextOrDefault(req);
        if (op == null || slot < 0 || slot >= SLOT_COUNT) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid parameters");
            return;
        }
        switch (op) {
            case OP_GO -> handleGo(req, resp, userId, slot, next);
            case OP_DELETE -> handleDelete(req, resp, userId, slot);
            case OP_CONFIRM -> {
                if (confirmJsp() == null) {
                    resp.sendError(405);
                    return;
                }
                handleConfirm(req, resp, userId, slot, next);
            }
            case OP_CANCEL -> {
                if (confirmJsp() == null) {
                    resp.sendError(405);
                    return;
                }
                handleCancel(req, resp, userId, slot);
            }
            default -> resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown op");
        }
    }

    protected void handleGo(HttpServletRequest req, HttpServletResponse resp,
                            String userId, int slot, String next)
            throws IOException, ServletException {
        resp.sendError(405);
    }

    protected void handleDelete(HttpServletRequest req, HttpServletResponse resp,
                                String userId, int slot)
            throws IOException {
        resp.sendError(405);
    }

    protected void handleConfirm(HttpServletRequest req, HttpServletResponse resp,
                                 String userId, int slot, String next)
            throws IOException {
        resp.sendError(405);
    }

    @SuppressWarnings("unused")
    protected void handleCancel(HttpServletRequest req, HttpServletResponse resp,
                                String userId, int slot)
            throws IOException {
        String back = backToList(req, path());
        resp.sendRedirect(resp.encodeRedirectURL(back));
    }
}
