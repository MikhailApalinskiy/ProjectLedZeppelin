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
 * Servlet responsible for handling load-slot operations.
 *
 * <p>Extends {@link AbstractSlotsServlet} and implements the logic for
 * loading a saved slot (game state) and redirecting the player to the
 * appropriate quest node.</p>
 *
 * <p>When a user selects a slot, the servlet verifies that the slot exists
 * and contains valid quest data, then redirects to the quest URL derived
 * from the saved node ID and quest ID.</p>
 *
 * <p>Empty slots are ignored and cause a simple return to the slots list.</p>
 */
public class GoLoadsServlet extends AbstractSlotsServlet {

    private static final Logger log = LoggerFactory.getLogger(GoLoadsServlet.class);

    /**
     * @return the relative servlet path used for redirects and navigation.
     */
    @Override
    protected String path() {
        return WebConst.Path.LOADS;
    }

    /**
     * @return the JSP path that renders the list of available load slots.
     */
    @Override
    protected String listJsp() {
        return WebConst.Jsp.LOADS;
    }

    /**
     * Handles the “Go” action — attempts to load a game slot and start the quest
     * from the corresponding node.
     *
     * <p>If the slot is empty, the user is redirected back to the loads page.
     * Otherwise, the servlet constructs a quest URL and redirects the player to it.</p>
     *
     * @param req            HTTP request
     * @param resp           HTTP response
     * @param userId         current user ID
     * @param questIdIgnored unused parameter in this subclass
     * @param slot           slot index being loaded
     * @param next           optional next parameter from the UI (ignored here)
     * @throws IOException if redirect fails
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
