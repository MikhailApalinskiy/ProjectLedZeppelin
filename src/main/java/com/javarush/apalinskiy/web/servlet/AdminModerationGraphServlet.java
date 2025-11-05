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
 * Servlet for displaying quest graphs during the moderation process.
 *
 * <p>This servlet allows administrators to preview quests submitted for moderation
 * — either new quest submissions or edits to existing quests. It visualizes
 * the quest structure (nodes and options) as an SVG graph for manual review.</p>
 *
 * <p>Supported request parameters:</p>
 * <ul>
 *     <li>{@code kind} — defines the type of moderation item:
 *         <ul>
 *             <li>{@code new} — for newly submitted quests</li>
 *             <li>{@code edit} — for quest edit submissions</li>
 *         </ul>
 *     </li>
 *     <li>{@code id} — the identifier of the pending quest item</li>
 * </ul>
 *
 * <p>The servlet delegates SVG graph rendering to {@link Web#buildQuestSvgModel(HttpServletRequest, List, int, boolean)}
 * and forwards the result to the JSP defined by {@link WebConst.Jsp#QUESTS_MOD_PREVIEW}.</p>
 */
public class AdminModerationGraphServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(AdminModerationGraphServlet.class);

    private QuestAuthoringService authoring;

    /**
     * Initializes the servlet by obtaining the {@link QuestAuthoringService} bean from the application context.
     *
     * @param config servlet configuration provided by the container
     * @throws ServletException if the service cannot be initialized
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
     * Handles GET requests for quest moderation previews.
     *
     * <p>Based on the {@code kind} parameter, this method fetches either a pending new quest
     * or a pending quest edit from the moderation queue and builds its graphical representation.</p>
     *
     * @param req  current HTTP request
     * @param resp current HTTP response
     * @throws ServletException if request forwarding fails
     * @throws IOException      if an I/O error occurs
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
     * Removes null values from the provided quest node list for safe rendering.
     *
     * @param src list of quest nodes
     * @return sanitized list without null elements
     */
    private static List<QuestNode> sanitize(List<QuestNode> src) {
        return (src == null) ? List.of() : src.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }
}
