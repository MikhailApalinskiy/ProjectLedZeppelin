package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.service.save.SaveStateService;
import com.javarush.apalinskiy.web.util.Web;
import com.javarush.apalinskiy.app.WebConst;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

public class GoSavesServlet extends AbstractSlotsServlet {

    private static final Logger log = LoggerFactory.getLogger(GoSavesServlet.class);

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
        String qid = (questId == null || questId.isBlank()) ? "main" : questId;
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
            log.info("Save confirm prompt userId={} slot={} oldNodeId={} newNodeId={} questId={}",
                    userId, slot, g.nodeId(), nodeId, qid);
            req.getRequestDispatcher(confirmJsp()).forward(req, resp);
            return;
        }
        String qname = resolveQuestName(qid);
        String title = titleFor(nodeId, qid);
        saveState.setGlobalSlot(userId, slot, qid, qname, nodeId, title);
        log.info("Save created userId={} slot={} nodeId={} questId={} title='{}'",
                userId, slot, nodeId, qid, title);
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
        log.info("Save overwritten userId={} slot={} nodeId={} questId={} title='{}'",
                userId, slot, nodeId, qid, title);
        req.getSession().setAttribute(WebConst.Attr.FLASH,
                "Slot №" + (slot + 1) + " is overwritten. Saved.");
        resp.sendRedirect(resp.encodeRedirectURL(Web.buildQuestUrlFromNext(req, next, nodeId)));
    }
}
