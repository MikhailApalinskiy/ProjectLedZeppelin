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
 * Base servlet for features related to quest playing and save slots.
 * <p>
 * Resolves core services from the {@link ServletContext} and provides helpers for:
 * authentication guard with redirect, human-readable titles, date formatting,
 * and building a list of {@link SlotView} for the UI.
 * </p>
 *
 * <h3>Injected services (from context)</h3>
 * <ul>
 *   <li>{@code QUEST_SERVICE} → {@link QuestService} (required)</li>
 *   <li>{@code SAVE_STATE_SERVICE} → {@link SaveStateService} (required)</li>
 *   <li>{@code AUTHORING_SERVICE} → {@link QuestAuthoringService} (optional; detected if present)</li>
 * </ul>
 *
 * <p>All service fields are marked {@code transient} to avoid accidental session serialization.</p>
 *
 * @see SlotView
 * @see SaveStateService
 * @see QuestService
 * @see QuestAuthoringService
 * @see WebConst
 * @see Web
 */
public class BaseSaveServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(BaseSaveServlet.class);

    /**
     * Global save-state persistence.
     */
    protected transient SaveStateService saveState;
    /**
     * Service to retrieve main-quest nodes.
     */
    protected transient QuestService questService;
    /**
     * Optional authoring/catalog service for custom quests.
     */
    protected transient QuestAuthoringService authoring;

    /**
     * Resolves required services from the application context.
     * <p>
     * Required: {@link QuestService}, {@link SaveStateService}. Optional: {@link QuestAuthoringService}.
     * Fails fast with {@link UnavailableException} if required beans are missing.
     * </p>
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
     * Requires an authenticated {@link User} in the session; otherwise redirects to the login page.
     * <p>
     * Looks up the user under {@code WebConst.Attr.USER}. If absent, constructs a login URL with a
     * {@code next} parameter pointing back to {@code returnPath} (normalized to start with {@code /}),
     * redirects, and returns {@code null}. If present, returns the {@link User}.
     * </p>
     *
     * @param returnPath relative path to return to after login (e.g., {@code "/slots"})
     * @return the authenticated user or {@code null} if a redirect happened
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
     * Resolves a human-friendly quest name for display purposes.
     * <p>
     * Delegates to {@link Web#displayName(String, QuestAuthoringService)} which uses the authoring
     * service when available and falls back to sensible defaults.
     * </p>
     *
     * @param questIdOrNull {@code null}, {@code "main"}, or custom quest id
     * @return non-null display name (e.g., {@code "Main quest"} or a catalog name)
     */
    protected String resolveQuestName(String questIdOrNull) {
        String name = Web.displayName(questIdOrNull, authoring);
        log.debug("resolveQuestName questId={} -> '{}'", questIdOrNull, name);
        return name;
    }

    /**
     * Computes a short title for a node, depending on whether it belongs to the main quest or a custom quest.
     * <ul>
     *   <li>Main quest: fetches node via {@link QuestService#getById(int)} and shortens text with {@link Web#shortTitle(String)}.</li>
     *   <li>Custom quest: finds the node inside the catalog entry and shortens its text; falls back to {@code "Node #<id>"}.</li>
     * </ul>
     *
     * @param nodeId        node identifier
     * @param questIdOrNull {@code null}, {@code "main"}, or custom quest id
     * @return a concise, human-readable title
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
     * Formats an {@link Instant} as {@code dd.MM.yyyy HH:mm} in the system default zone.
     *
     * @param ts timestamp (may be {@code null})
     * @return formatted string or {@code null} if {@code ts} is {@code null}
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
     * Builds a fixed-size list of {@link SlotView} representing all global save slots for a user.
     * <p>
     * For each index in {@code [0, WebConst.SLOT_COUNT)}:
     * <ul>
     *   <li>If a {@link SaveStateService.GlobalSlot} exists, normalizes its data (quest id/name, title, updatedAt)
     *       and creates a {@link SlotView#filled(int, int, String, String, String, String)}.</li>
     *   <li>Otherwise, creates a default {@link SlotView#empty(int, String, String)} with {@code "main"}.</li>
     * </ul>
     * Titles default to {@code "Node #<id>"}; quest name defaults to {@code "Main quest"} or {@code "Custom quest"}.
     * </p>
     *
     * @param userId owner of the global slots
     * @return immutable-size list (but not unmodifiable) of slot view models
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
