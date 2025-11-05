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

/**
 * Servlet responsible for handling “save slot” operations in quests.
 *
 * <p>Extends {@link AbstractSlotsServlet} and provides logic for saving
 * the current quest progress to a selected slot. Supports both
 * creating new saves and overwriting existing ones with confirmation.</p>
 *
 * <p>When the user selects a slot:
 * <ul>
 *   <li>If it’s empty — a new save is immediately recorded.</li>
 *   <li>If it already contains data — a confirmation prompt is displayed
 *       before overwriting.</li>
 * </ul>
 */
public class GoSavesServlet extends AbstractSlotsServlet {

    private static final Logger log = LoggerFactory.getLogger(GoSavesServlet.class);

    /**
     * @return servlet path used for redirection and page context.
     */
    @Override
    protected String path() {
        return WebConst.Path.SAVES;
    }

    /**
     * @return JSP path for the saves list page.
     */
    @Override
    protected String listJsp() {
        return WebConst.Jsp.SAVES;
    }

    /**
     * @return JSP path for the confirmation dialog when overwriting a save.
     */
    @Override
    protected String confirmJsp() {
        return WebConst.Jsp.CONFIRM;
    }

    /**
     * Handles a “Go” action: saves the current quest progress into the chosen slot.
     *
     * <p>If the target slot already contains a save, forwards to the confirmation
     * JSP with old and new node info for user approval.</p>
     * <p>If the slot is empty, immediately creates a new save and redirects back
     * to the quest node.</p>
     *
     * @param req     HTTP request
     * @param resp    HTTP response
     * @param userId  current user ID
     * @param questId quest ID (or null for main quest)
     * @param slot    slot index
     * @param next    optional redirect parameter from the quest screen
     * @throws IOException      if redirect fails
     * @throws ServletException if forwarding to confirmation JSP fails
     */
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

    /**
     * Handles confirmation submission when overwriting an existing save slot.
     *
     * <p>Replaces previous save data with new quest position and redirects
     * the user back to the quest node.</p>
     *
     * @param req     HTTP request
     * @param resp    HTTP response
     * @param userId  current user ID
     * @param questId quest ID (or null for main quest)
     * @param slot    slot index to overwrite
     * @param next    optional redirect target from quest screen
     * @throws IOException if redirect fails
     */
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
