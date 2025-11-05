package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.domain.quest.QuestNode;
import com.javarush.apalinskiy.repository.hibernate.quest.InMemoryQuestStore;
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
 * Servlet responsible for rendering the SVG graph of the quest currently open in the editor.
 *
 * <p>It retrieves quest nodes from the in-memory repository (usually the active editor draft),
 * constructs an SVG model using {@link Web#buildQuestSvgModel}, and forwards the result
 * to the corresponding JSP.</p>
 *
 * <p>Additionally, it supports an optional {@code load} parameter that triggers
 * loading of a quest from the catalog into the editor context before rendering.</p>
 */
public class GraphSvgServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(GraphSvgServlet.class);

    private InMemoryQuestStore repo;

    /**
     * Initializes servlet dependencies from the application context.
     *
     * @param config servlet configuration
     * @throws ServletException if repository is not found in context
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
     * Renders the quest graph as SVG.
     *
     * <p>Optional query parameters:</p>
     * <ul>
     *   <li>{@code load} — quest ID to load into the editor before rendering</li>
     * </ul>
     *
     * <p>If {@code load} is specified, the servlet attempts to load the quest
     * into the {@link QuestAuthoringService} and update the session attributes
     * {@code EDITING_QUEST_ID} and {@code EDITING_QUEST_NAME}.</p>
     *
     * <p>Then it reads nodes from the editor repository and builds
     * the SVG model using {@link Web#buildQuestSvgModel(HttpServletRequest, List, int, boolean)}.</p>
     *
     * @param req  HTTP request
     * @param resp HTTP response
     * @throws ServletException on internal forward errors
     * @throws IOException      on I/O or redirect issues
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
     * Safely retrieves quest nodes from the in-memory repository.
     *
     * @return list of quest nodes or empty list if repository read fails
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
