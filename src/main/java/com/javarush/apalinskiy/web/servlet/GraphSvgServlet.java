package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.domain.quest.QuestNode;
import com.javarush.apalinskiy.repository.inmemory.quest.InMemoryQuestStore;
import com.javarush.apalinskiy.service.quest.QuestAuthoringService;
import com.javarush.apalinskiy.web.util.Web;
import com.javarush.apalinskiy.app.WebConst;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * Renders an SVG preview of the quest graph currently held in the in-memory editor store.
 * <p>
 * The servlet reads nodes and start id from {@link InMemoryQuestStore} and builds a view model
 * for an SVG diagram via {@link Web#buildQuestSvgModel(HttpServletRequest, List, int, boolean)}.
 * It can optionally load a quest from the catalog into the editor (when {@code load} parameter is provided)
 * using {@link QuestAuthoringService}, and keep editor metadata in the HTTP session.
 * </p>
 *
 * <h3>Editor session metadata</h3>
 * <ul>
 *   <li>{@link WebConst.Attr#EDITING_QUEST_ID}</li>
 *   <li>{@link WebConst.Attr#EDITING_QUEST_NAME}</li>
 * </ul>
 *
 * <h3>GET parameters</h3>
 * <ul>
 *   <li><b>load</b> — optional quest id to load from the catalog into the editor before rendering.</li>
 * </ul>
 *
 * <h3>View</h3>
 * Forwards to {@code WebConst.Jsp.GRAPH_SVG} after populating the SVG model attributes.
 *
 * @see InMemoryQuestStore
 * @see QuestAuthoringService
 * @see WebConst
 * @see Web
 */
public class GraphSvgServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(GraphSvgServlet.class);

    private InMemoryQuestStore repo;

    /**
     * Resolves {@link InMemoryQuestStore} from the {@link ServletContext}.
     * <p>
     * The store is expected to be under {@code WebConst.Ctx.EDITOR_REPOSITORY}. If it's missing
     * or of an unexpected type, initialization fails.
     * </p>
     *
     * @param config servlet config provided by the container
     * @throws ServletException if the editor repository is absent or invalid
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        ServletContext ctx = config.getServletContext();
        Object obj = ctx.getAttribute(WebConst.Ctx.EDITOR_REPOSITORY);
        if (!(obj instanceof InMemoryQuestStore r)) {
            log.error("Init failed: editor repository not found (attr={})", WebConst.Ctx.EDITOR_REPOSITORY);
            throw new ServletException("Editor repository not found in ServletContext (attr: " + WebConst.Ctx.EDITOR_REPOSITORY + ")");
        }
        this.repo = r;
        log.debug("GraphSvgServlet initialized with InMemoryQuestStore");
    }

    /**
     * Builds and renders the SVG graph for the current editor state.
     * <p>
     * Flow:
     * <ol>
     *   <li>Copies optional flash params ({@code ERROR}, {@code OK}) to request attributes.</li>
     *   <li>If {@code load} parameter is present and an authoring service is available in context,
     *       loads the quest into the editor, stores {@code EDITING_QUEST_ID/NAME} in session, and sets an OK/ERROR message.</li>
     *   <li>If no {@code load}, but {@code EDITING_QUEST_ID} exists and {@code EDITING_QUEST_NAME} is blank,
     *       tries to backfill the name from the catalog.</li>
     *   <li>Reads nodes/start id from the in-memory store and calls
     *       {@link Web#buildQuestSvgModel(HttpServletRequest, List, int, boolean)} with {@code readOnly=true}.</li>
     *   <li>Forwards to {@code WebConst.Jsp.GRAPH_SVG}.</li>
     * </ol>
     * </p>
     *
     * @param req  HTTP request (may contain {@code load})
     * @param resp HTTP response
     * @throws ServletException if forwarding fails
     * @throws IOException      if I/O errors occur
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Web.copyParamsToAttrs(req, WebConst.Attr.ERROR, WebConst.Attr.OK);
        HttpSession session = req.getSession(true);
        String loadQuestId = Web.trimOrNull(req.getParameter(WebConst.Param.LOAD));
        Object svc = getServletContext().getAttribute(WebConst.Ctx.AUTHORING_SERVICE);
        if (loadQuestId != null && svc instanceof QuestAuthoringService a) {
            try {
                a.loadToEditor(loadQuestId);
                session.setAttribute(WebConst.Attr.EDITING_QUEST_ID, loadQuestId);
                a.getFromCatalog(loadQuestId)
                        .ifPresent(cq -> session.setAttribute(WebConst.Attr.EDITING_QUEST_NAME, cq.getName()));
                req.setAttribute(WebConst.Attr.OK, "The quest is uploaded to the editor");
                log.info("Graph load into editor questId={} (set EDITING_QUEST_ID/NAME if available)", loadQuestId);
            } catch (Exception e) {
                req.setAttribute(WebConst.Attr.ERROR, "Couldn't upload the quest: " + e.getMessage());
                log.warn("Graph load failed questId={} msg={}", loadQuestId, e.getMessage());
            }
        } else if (svc instanceof QuestAuthoringService a) {
            String editingId = (String) session.getAttribute(WebConst.Attr.EDITING_QUEST_ID);
            Object editingName = session.getAttribute(WebConst.Attr.EDITING_QUEST_NAME);
            if (editingId != null && (editingName == null || String.valueOf(editingName).isBlank())) {
                a.getFromCatalog(editingId)
                        .ifPresent(cq -> session.setAttribute(WebConst.Attr.EDITING_QUEST_NAME, cq.getName()));
                log.debug("Filled missing EDITING_QUEST_NAME from catalog for questId={}", editingId);
            }
        }
        List<QuestNode> nodes = safeNodes();
        int startId = repo.startId();
        Web.buildQuestSvgModel(req, nodes, startId, true);
        log.debug("Graph model built nodes={} startId={}", nodes.size(), startId);
        Web.forward(req, resp, WebConst.Jsp.GRAPH_SVG);
    }

    /**
     * Safely retrieves the current node list from the editor repository.
     * <p>Returns an empty list if the repository throws.</p>
     *
     * @return non-null list of nodes (possibly empty)
     */
    private List<QuestNode> safeNodes() {
        try {
            return repo.nodes();
        } catch (Exception e) {
            log.warn("Failed to read nodes from repo, returning empty list", e);
            return Collections.emptyList();
        }
    }
}
