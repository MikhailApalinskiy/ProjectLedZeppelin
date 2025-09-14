package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.repository.quest.CustomQuestRepository;
import com.javarush.apalinskiy.service.quest.QuestAuthoringService;
import com.javarush.apalinskiy.domain.quest.QuestNode;
import com.javarush.apalinskiy.web.util.Web;
import com.javarush.apalinskiy.app.WebConst;
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
import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin-only preview servlet that renders a temporary SVG graph for quests
 * undergoing moderation (either a newly submitted quest or a pending edit).
 * <p>
 * The servlet reads pending items from {@link QuestAuthoringService} and
 * builds a view model for the SVG graph using {@link Web#buildQuestSvgModel}.
 * It does not mutate state and is safe to refresh.
 * </p>
 *
 * <h3>Access</h3>
 * <p>
 * Intended to be protected by admin auth (e.g., a security filter).
 * This class assumes authentication/authorization is enforced upstream.
 * </p>
 *
 * <h3>Query parameters (GET)</h3>
 * <ul>
 *   <li><b>kind</b> — {@code "new"} or {@code "edit"}. Determines which pending list to search.</li>
 *   <li><b>id</b> — identifier of the pending item:
 *     <ul>
 *       <li>for {@code kind=new}: {@code pendingId}</li>
 *       <li>for {@code kind=edit}: {@code questId}</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h3>Response</h3>
 * <ul>
 *   <li><b>200</b> — forwards to {@code WebConst.Jsp.QUESTS_MOD_PREVIEW} with attributes:</li>
 *   <ul>
 *     <li>{@code previewTitle} — human-friendly header for the preview page.</li>
 *     <li>{@code backUrl} — URL back to moderation list ({@code WebConst.Path.QUESTS_MOD}).</li>
 *     <li>SVG graph model attributes populated by {@link Web#buildQuestSvgModel}.</li>
 *   </ul>
 *   <li><b>400</b> — missing/unknown parameters.</li>
 *   <li><b>404</b> — pending item not found.</li>
 * </ul>
 *
 * <h3>Notes</h3>
 * <ul>
 *   <li>Only non-null nodes are passed to the renderer (see {@link #sanitize(List)}).</li>
 *   <li>Start node is taken from the pending item and passed to the SVG builder.</li>
 * </ul>
 *
 * @see QuestAuthoringService
 * @see CustomQuestRepository.PendingNew
 * @see CustomQuestRepository.PendingEdit
 * @see Web#buildQuestSvgModel(HttpServletRequest, List, int, boolean)
 * @see WebConst
 */
public class AdminModerationGraphServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(AdminModerationGraphServlet.class);

    private QuestAuthoringService authoring;

    /**
     * Initializes the servlet by resolving {@link QuestAuthoringService} from the servlet context.
     *
     * @throws UnavailableException if the service is not present in the context
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        ServletContext ctx = config.getServletContext();
        try {
            this.authoring = Web.ctxBean(ctx, WebConst.Ctx.AUTHORING_SERVICE, QuestAuthoringService.class);
            log.debug("AdminModerationGraphServlet initialized");
        } catch (IllegalStateException e) {
            log.error("Initialization failed: QuestAuthoringService not found", e);
            throw new UnavailableException("QuestAuthoringService not found");
        }
    }

    /**
     * Renders a read-only preview of a pending quest graph for moderation.
     * <p>
     * Required params: {@code kind} (new|edit), {@code id}.
     * </p>
     * <ul>
     *   <li>When {@code kind=new}: searches {@link QuestAuthoringService#listPendingNew()} by {@code pendingId}.</li>
     *   <li>When {@code kind=edit}: searches {@link QuestAuthoringService#listPendingEdits()} by {@code questId}.</li>
     * </ul>
     * On success, builds the SVG model and forwards to {@code WebConst.Jsp.QUESTS_MOD_PREVIEW}.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        final String kind = Web.trimOrNull(req.getParameter("kind"));
        final String id = Web.trimOrNull(req.getParameter("id"));
        if (kind == null || id == null) {
            log.warn("Preview request with missing params kind={} id={}", kind, id);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing parameters");
            return;
        }
        final int startId;
        final List<QuestNode> nodes;
        switch (kind) {
            case "new" -> {
                CustomQuestRepository.PendingNew item = authoring.listPendingNew().stream()
                        .filter(x -> id.equals(x.getPendingId()))
                        .findFirst().orElse(null);
                if (item == null) {
                    log.warn("Preview 'new' not found pendingId={}", id);
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                startId = item.getStartId();
                nodes = sanitize(item.getNodes());
                req.setAttribute("previewTitle", "New quest: " + item.getName());
                log.info("Preview pending NEW pendingId={} name='{}' nodes={} startId={}", id, item.getName(), nodes.size(), startId);
            }
            case "edit" -> {
                CustomQuestRepository.PendingEdit item = authoring.listPendingEdits().stream()
                        .filter(x -> id.equals(x.getQuestId()))
                        .findFirst().orElse(null);
                if (item == null) {
                    log.warn("Preview 'edit' not found questId={}", id);
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                startId = item.getStartId();
                nodes = sanitize(item.getNodes());
                req.setAttribute("previewTitle", "Quest edits: " + item.getName());
                log.info("Preview pending EDIT questId={} name='{}' nodes={} startId={}", id, item.getName(), nodes.size(), startId);
            }
            default -> {
                log.warn("Preview request with unknown kind kind={} id={}", kind, id);
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown kind");
                return;
            }
        }
        Web.buildQuestSvgModel(req, nodes, startId, true);
        req.setAttribute("backUrl", req.getContextPath() + WebConst.Path.QUESTS_MOD);
        Web.forward(req, resp, WebConst.Jsp.QUESTS_MOD_PREVIEW);
    }

    /**
     * Ensures the node list has no {@code null} entries and replaces {@code null} with an empty list.
     *
     * @param src original list (may be {@code null})
     * @return non-null list containing only non-null nodes
     */
    private static List<QuestNode> sanitize(List<QuestNode> src) {
        return (src == null) ? List.of() : src.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }
}
