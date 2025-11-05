package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.service.quest.QuestAuthoringService;
import com.javarush.apalinskiy.service.quest.QuestService;
import com.javarush.apalinskiy.service.save.SaveStateService;
import com.javarush.apalinskiy.domain.quest.QuestNode;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.web.util.Web;
import com.javarush.apalinskiy.app.WebConst;
import com.javarush.apalinskiy.web.view.SlotView;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Abstract base servlet providing shared logic for save-slot related operations.
 *
 * <p>This class offers utility methods for managing save states, displaying quest
 * information in slots, formatting timestamps, and verifying user authentication.</p>
 *
 * <p>It initializes core services used across save/load servlet hierarchy:</p>
 * <ul>
 *     <li>{@link SaveStateService} — access to player save slots and global states</li>
 *     <li>{@link QuestService} — retrieval of main quest nodes and text</li>
 *     <li>{@link QuestAuthoringService} — optional access to custom quest metadata</li>
 * </ul>
 *
 * <p>Child servlets such as save and load controllers extend this class to reuse its
 * helper utilities and enforce consistent authentication and slot rendering logic.</p>
 */
public class BaseSaveServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(BaseSaveServlet.class);

    protected transient SaveStateService saveState;
    protected transient QuestService questService;
    protected transient QuestAuthoringService authoring;

    /**
     * Initializes all core services required for save-slot operations.
     *
     * @param config servlet configuration provided by the container
     * @throws ServletException if mandatory services are not found
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        ServletContext ctx = config.getServletContext();
        try {
            this.questService = Web.ctxBean(ctx, WebConst.Ctx.QUEST_SERVICE, QuestService.class);
            this.saveState = Web.ctxBean(ctx, WebConst.Ctx.SAVE_STATE_SERVICE, SaveStateService.class);
            Object as = ctx.getAttribute(WebConst.Ctx.AUTHORING_SERVICE);
            if (as instanceof QuestAuthoringService a) {
                this.authoring = a;
            }
            log.debug("BaseSaveServlet init: questService={}, saveState={}, authoring={}",
                    (questService == null ? "null" : questService.getClass().getSimpleName()),
                    (saveState == null ? "null" : saveState.getClass().getSimpleName()),
                    (authoring == null ? "null" : authoring.getClass().getSimpleName()));
        } catch (IllegalStateException e) {
            log.error("BaseSaveServlet init failed: {}", e.getMessage(), e);
            throw new UnavailableException(e.getMessage());
        }
    }

    /**
     * Ensures that a user is authenticated. If not, redirects them to the login page.
     *
     * <p>Includes a “next” parameter to return to the originally requested page after login.</p>
     *
     * @param req        HTTP request
     * @param resp       HTTP response
     * @param returnPath path to return to after successful login
     * @return the authenticated {@link User}, or {@code null} if redirected
     * @throws IOException if redirect fails
     */
    protected User requireAuthOrRedirect(HttpServletRequest req, HttpServletResponse resp, String returnPath)
            throws IOException {
        User u = (User) req.getSession().getAttribute(WebConst.Attr.USER);
        if (u == null) {
            String next = req.getContextPath() + (returnPath.startsWith("/") ? returnPath : ("/" + returnPath));
            String loginUrl = req.getContextPath() + WebConst.Path.LOGIN + "?next=" + Web.urlEncode(next);
            log.info("Auth required -> redirect to login next={}", next);
            resp.sendRedirect(resp.encodeRedirectURL(loginUrl));
            return null;
        }
        return u;
    }

    /**
     * Resolves the display name of a quest based on its ID.
     *
     * <p>Delegates to {@link Web#displayName(String, QuestAuthoringService)} for proper formatting.</p>
     *
     * @param questIdOrNull quest identifier or {@code null}
     * @return resolved display name
     */
    protected String resolveQuestName(String questIdOrNull) {
        String name = Web.displayName(questIdOrNull, authoring);
        log.debug("resolveQuestName questId={} -> '{}'", questIdOrNull, name);
        return name;
    }

    /**
     * Produces a short, user-friendly title for a given quest node.
     *
     * @param nodeId        node identifier
     * @param questIdOrNull quest ID or {@code null} for the main quest
     * @return concise title (e.g., truncated node text or “Node #X”)
     */
    protected String titleFor(int nodeId, String questIdOrNull) {
        String qid = (questIdOrNull == null || questIdOrNull.isBlank()) ? "main" : questIdOrNull;
        if ("main".equals(qid)) {
            QuestNode n = questService.getById(nodeId);
            String title = (n != null) ? Web.shortTitle(n.getText()) : ("Node #" + nodeId);
            log.debug("titleFor(main) nodeId={} -> '{}'", nodeId, title);
            return title;
        }
        if (authoring != null) {
            String title = authoring.getFromCatalog(qid)
                    .map(q -> {
                        for (QuestNode n : q.getNodes()) {
                            if (n.getId() == nodeId) {
                                return Web.shortTitle(n.getText());
                            }
                        }
                        return "Node #" + nodeId;
                    })
                    .orElse("Node #" + nodeId);
            log.debug("titleFor(custom) questId={} nodeId={} -> '{}'", qid, nodeId, title);
            return title;
        }
        String fallback = "Node #" + nodeId;
        log.debug("titleFor(custom) authoring=null nodeId={} -> '{}'", nodeId, fallback);
        return fallback;
    }

    /**
     * Formats a timestamp for human-readable display (e.g., “25.04.2025 13:45”).
     *
     * @param ts timestamp to format
     * @return formatted string, or {@code null} if timestamp is {@code null}
     */
    protected String formatUpdated(Instant ts) {
        if (ts == null) {
            return null;
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                .withZone(ZoneId.systemDefault());
        return fmt.format(ts);
    }

    /**
     * Builds a complete list of all save slots for a given user.
     *
     * <p>Combines filled and empty slots using {@link SlotView} wrappers for rendering
     * in the save/load JSP pages. Each filled slot contains quest metadata, node title,
     * and last update timestamp.</p>
     *
     * @param userId identifier of the user whose slots should be built
     * @return list of {@link SlotView} objects representing all slots
     */
    protected List<SlotView> buildSlotsAll(String userId) {
        List<SlotView> list = new ArrayList<>(WebConst.SLOT_COUNT);
        for (int i = 0; i < WebConst.SLOT_COUNT; i++) {
            Optional<SaveStateService.GlobalSlot> opt = saveState.getGlobalSlot(userId, i);
            if (opt.isPresent()) {
                SaveStateService.GlobalSlot g = opt.get();
                String qid = (g.questId() == null || g.questId().isBlank()) ? "main" : g.questId();
                String qname = (g.questName() != null && !g.questName().isBlank())
                        ? g.questName()
                        : ("main".equals(qid) ? "Main quest" : "Custom quest");
                String title = (g.title() != null && !g.title().isBlank())
                        ? g.title()
                        : ("Node #" + g.nodeId());
                String updated = formatUpdated(g.updatedAt());
                list.add(SlotView.filled(i, g.nodeId(), title, updated, qid, qname));
            } else {
                list.add(SlotView.empty(i, "main", "Main quest"));
            }
        }
        log.debug("buildSlotsAll userId={} total={}", userId, list.size());
        return list;
    }
}
