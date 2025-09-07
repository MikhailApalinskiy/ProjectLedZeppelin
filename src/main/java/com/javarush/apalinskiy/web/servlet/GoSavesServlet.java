package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.application.save.SaveStateService;
import com.javarush.apalinskiy.web.util.Web;
import com.javarush.apalinskiy.web.util.WebConst;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Optional;

public class GoSavesServlet extends AbstractSlotsServlet {

    @Override
    protected String path() {
        return WebConst.Path.SAVES;
    }

    @Override
    protected String listJsp() {
        return WebConst.Jsp.SAVES;
    }

    @Override
    protected String confirmJsp() {
        return WebConst.Jsp.CONFIRM;
    }

    @Override
    protected void handleGo(HttpServletRequest req, HttpServletResponse resp,
                            String userId, String questId, int slot, String next)
            throws IOException, ServletException {
        int nodeId = Web.parseIntOrDefault(req.getParameter(WebConst.Param.NODE), 0);
        Optional<SaveStateService.GlobalSlot> existing = saveState.getGlobalSlot(userId, slot);
        if (existing.isPresent()) {
            SaveStateService.GlobalSlot g = existing.get();
            req.setAttribute("slotIndex", slot);
            req.setAttribute("newNodeId", nodeId);
            req.setAttribute("newNodeTitle", titleFor(nodeId, questId));
            req.setAttribute("oldNodeId", g.nodeId());
            req.setAttribute("oldNodeTitle", g.title());
            req.setAttribute(WebConst.Param.NEXT, next);
            req.setAttribute(WebConst.Param.PURPOSE,
                    Optional.ofNullable(req.getParameter(WebConst.Param.PURPOSE)).orElse("save"));
            req.getRequestDispatcher(confirmJsp()).forward(req, resp);
            return;
        }
        String qid = (questId == null || questId.isBlank()) ? "main" : questId;
        String qname = resolveQuestName(qid);
        String title = titleFor(nodeId, qid);
        saveState.setGlobalSlot(userId, slot, qid, qname, nodeId, title);
        req.getSession().setAttribute(WebConst.Attr.FLASH,
                "The save is recorded in the slot №" + (slot + 1) + ".");
        resp.sendRedirect(resp.encodeRedirectURL(Web.buildQuestUrlFromNext(req, next, nodeId)));
    }

    @Override
    protected void handleConfirm(HttpServletRequest req, HttpServletResponse resp,
                                 String userId, String questId, int slot, String next)
            throws IOException {
        int nodeId = Web.parseIntOrDefault(req.getParameter(WebConst.Param.NODE), 0);
        String qid = (questId == null || questId.isBlank()) ? "main" : questId;
        String qname = resolveQuestName(qid);
        String title = titleFor(nodeId, qid);
        saveState.setGlobalSlot(userId, slot, qid, qname, nodeId, title);
        req.getSession().setAttribute(WebConst.Attr.FLASH,
                "Slot №" + (slot + 1) + " is overwritten. Saved.");
        resp.sendRedirect(resp.encodeRedirectURL(Web.buildQuestUrlFromNext(req, next, nodeId)));
    }
}
