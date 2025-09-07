package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.web.util.Web;
import com.javarush.apalinskiy.web.util.WebConst;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import java.util.Optional;

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
        if (u == null) {
            return;
        }
        Web.pullFlash(req, WebConst.Attr.FLASH);
        req.setAttribute("slots", buildSlotsAll(String.valueOf(u.getUserId())));
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
        final String op = req.getParameter(WebConst.Param.OP);
        final int slot = Web.parseIntOrDefault(req.getParameter(WebConst.Param.SLOT), -1);
        final String next = Optional.ofNullable(req.getParameter(WebConst.Param.NEXT)).orElse(WebConst.Path.QUEST);
        final String questId = Web.trimOrNull(req.getParameter(WebConst.Param.CUSTOM));
        if (op == null || slot < 0 || slot >= WebConst.SLOT_COUNT) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid parameters");
            return;
        }
        switch (op) {
            case WebConst.Op.GO -> handleGo(req, resp, userId, questId, slot, next);
            case WebConst.Op.DELETE -> handleDelete(req, resp, userId, questId, slot);
            case WebConst.Op.CONFIRM -> {
                if (confirmJsp() == null) {
                    resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                    return;
                }
                handleConfirm(req, resp, userId, questId, slot, next);
            }
            case WebConst.Op.CANCEL -> {
                if (confirmJsp() == null) {
                    resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                    return;
                }
                handleCancel(req, resp, userId, questId, slot);
            }
            default -> resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown op");
        }
    }

    protected void handleGo(HttpServletRequest req, HttpServletResponse resp,
                            String userId, String questId, int slot, String next)
            throws IOException, ServletException {
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    @SuppressWarnings("unused")
    protected void handleDelete(HttpServletRequest req, HttpServletResponse resp, String userId, String questId, int slot) throws IOException {
        saveState.clearGlobalSlot(userId, slot);
        req.getSession().setAttribute(WebConst.Attr.FLASH, "Slot №" + (slot + 1) + " delete.");
        Web.redirectKeep(req, resp, path(), WebConst.ParamGroup.SLOT_NAV);
    }

    protected void handleConfirm(HttpServletRequest req, HttpServletResponse resp,
                                 String userId, String questId, int slot, String next)
            throws IOException {
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    @SuppressWarnings("unused")
    protected void handleCancel(HttpServletRequest req, HttpServletResponse resp,
                                String userId, String questId, int slot)
            throws IOException {
        String back = Web.addParamsFromReqEncoded(
                req, req.getContextPath() + path(),
                WebConst.Param.NEXT, WebConst.Param.PURPOSE, WebConst.Param.NODE, WebConst.Param.CUSTOM
        );
        resp.sendRedirect(resp.encodeRedirectURL(back));
    }
}
