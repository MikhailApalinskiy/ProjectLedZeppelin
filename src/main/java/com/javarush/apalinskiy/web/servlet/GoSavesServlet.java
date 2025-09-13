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
 * Implements the "Save Game" flow for global slots.
 * <p>
 * Extends {@link AbstractSlotsServlet} to reuse listing/routing. This subclass:
 * <ul>
 *   <li>Defines paths/JSPs for the save UI;</li>
 *   <li>Supports a confirmation step when overwriting a non-empty slot;</li>
 *   <li>Writes/overwrites a global slot and redirects back to the quest node.</li>
 * </ul>
 *
 * <h3>Flow</h3>
 * <ol>
 *   <li>User clicks "Save" on a node → {@link #handleGo(HttpServletRequest, HttpServletResponse, String, String, int, String)}.</li>
 *   <li>If the target slot is empty → save immediately.</li>
 *   <li>If the target slot is occupied → forward to confirm JSP ({@link #confirmJsp()}).</li>
 *   <li>Confirm POST → {@link #handleConfirm(HttpServletRequest, HttpServletResponse, String, String, int, String)} overwrites the slot.</li>
 * </ol>
 *
 * @see AbstractSlotsServlet
 * @see SaveStateService
 * @see SaveStateService.GlobalSlot
 * @see WebConst
 * @see Web
 */
public class GoSavesServlet extends AbstractSlotsServlet {

    private static final Logger log = LoggerFactory.getLogger(GoSavesServlet.class);

    /**
     * @return base path for the saves page (used for redirects).
     */
    @Override
    protected String path() {
        return WebConst.Path.SAVES;
    }

    /**
     * @return JSP used to render the list of save slots.
     */
    @Override
    protected String listJsp() {
        return WebConst.Jsp.SAVES;
    }

    /**
     * @return JSP used for the overwrite confirmation dialog.
     */
    @Override
    protected String confirmJsp() {
        return WebConst.Jsp.CONFIRM;
    }

    /**
     * Handles initial "save" intent for a given slot.
     * <p>
     * Behavior:
     * <ul>
     *   <li>Reads the target node id from {@code node} parameter and normalizes {@code questId} (defaults to {@code main}).</li>
     *   <li>If the slot already has a value, forwards to the confirm JSP with both "old" and "new" titles set as attributes.</li>
     *   <li>If the slot is empty, writes it immediately via {@link SaveStateService#setGlobalSlot} and redirects to the
     *       quest URL derived from {@code next} and the node id.</li>
     * </ul>
     *
     * <p>Request attributes set for confirm view:</p>
     * <ul>
     *   <li>{@code slotIndex}, {@code newNodeId}, {@code newNodeTitle}, {@code oldNodeId}, {@code oldNodeTitle}, {@code next}, {@code purpose}</li>
     * </ul>
     *
     * @param req     HTTP request (expects {@code node}, optional {@code purpose})
     * @param resp    HTTP response
     * @param userId  current user id (stringified)
     * @param questId quest id (nullable → treated as {@code main})
     * @param slot    zero-based slot index
     * @param next    next URL hint used to compute redirect after saving
     * @throws IOException      if redirect fails
     * @throws ServletException if forwarding to confirm JSP fails
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
     * Handles the overwrite confirmation step.
     * <p>
     * Normalizes {@code questId}, computes display name and node title, writes the slot via
     * {@link SaveStateService#setGlobalSlot}, sets a flash message, and redirects to the computed quest URL.
     * </p>
     *
     * @param req     HTTP request (expects {@code node})
     * @param resp    HTTP response
     * @param userId  current user id (stringified)
     * @param questId quest id (nullable → treated as {@code main})
     * @param slot    zero-based slot index to overwrite
     * @param next    next URL hint used to compute redirect after saving
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
