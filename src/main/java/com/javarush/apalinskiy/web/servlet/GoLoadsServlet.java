package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.service.save.SaveStateService;
import com.javarush.apalinskiy.web.util.Web;
import com.javarush.apalinskiy.app.WebConst;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

/**
 * Servlet that implements the "Load Game" action for global save slots.
 * <p>
 * Inherits the common slots UI and routing from {@link AbstractSlotsServlet}. This subclass
 * defines the page endpoints and, on {@code GO}, resolves the selected slot and redirects
 * the user to the appropriate quest node URL.
 * </p>
 *
 * <h3>Behavior</h3>
 * <ul>
 *   <li>{@link #path()} — base path for the loads page (used for redirects).</li>
 *   <li>{@link #listJsp()} — JSP used to render the list of slots.</li>
 *   <li>{@link #handleGo(HttpServletRequest, HttpServletResponse, String, String, int, String)} —
 *       loads the slot, posts a flash message, and redirects to the node URL.</li>
 * </ul>
 *
 * @see AbstractSlotsServlet
 * @see SaveStateService.GlobalSlot
 * @see Web#questUrl(HttpServletRequest, int, String)
 * @see WebConst
 */
public class GoLoadsServlet extends AbstractSlotsServlet {

    private static final Logger log = LoggerFactory.getLogger(GoLoadsServlet.class);

    /**
     * Returns the servlet-relative path for the loads page.
     *
     * @return {@code WebConst.Path.LOADS}
     */
    @Override
    protected String path() {
        return WebConst.Path.LOADS;
    }

    /**
     * Returns the JSP used to render the list of slots for the "load game" page.
     *
     * @return {@code WebConst.Jsp.LOADS}
     */
    @Override
    protected String listJsp() {
        return WebConst.Jsp.LOADS;
    }

    /**
     * Handles the {@code GO} operation for a selected slot:
     * <ol>
     *   <li>If the slot is empty, redirects back to {@link #path()} preserving slot
     *       navigation parameters.</li>
     *   <li>If present, composes a user-facing flash message with quest name and node title,
     *       computes the target quest URL via {@link Web#questUrl}, and redirects there.</li>
     * </ol>
     * <p>
     * The custom quest id passed to {@code questUrl} is {@code null} for the main quest and
     * the normalized non-blank {@code questId} for custom quests.
     * </p>
     *
     * @param req            HTTP request
     * @param resp           HTTP response
     * @param userId         current user's id (stringified)
     * @param questIdIgnored ignored for the load operation (slot already contains quest id)
     * @param slot           zero-based slot index
     * @param next           ignored for the load operation (we redirect to the quest URL)
     * @throws IOException if sending a redirect fails
     */
    @Override
    protected void handleGo(HttpServletRequest req, HttpServletResponse resp,
                            String userId, String questIdIgnored, int slot, String next)
            throws IOException {
        Optional<SaveStateService.GlobalSlot> opt = saveState.getGlobalSlot(userId, slot);
        if (opt.isEmpty()) {
            log.debug("Load slot: empty slot userId={} slot={}", userId, slot);
            Web.redirectKeep(req, resp, path(), WebConst.ParamGroup.SLOT_NAV);
            return;
        }
        SaveStateService.GlobalSlot g = opt.get();
        String qname = (g.questName() != null && !g.questName().isBlank())
                ? g.questName()
                : ("main".equals(g.questId()) ? "Main quest" : "Custom quest");
        String title = (g.title() != null && !g.title().isBlank())
                ? g.title()
                : ("Node #" + g.nodeId());
        req.getSession().setAttribute(WebConst.Attr.FLASH,
                "Slot № " + (slot + 1) + " is loaded" + " — " + qname + " • " + title + ".");
        String qid = (g.questId() == null || g.questId().isBlank() || "main".equals(g.questId())) ? null : g.questId();
        String target = Web.questUrl(req, g.nodeId(), qid);
        log.info("Load slot success userId={} slot={} nodeId={} questId={} target={}",
                userId, slot, g.nodeId(), (qid == null ? "main" : qid), target);
        resp.sendRedirect(resp.encodeRedirectURL(target));
    }
}
