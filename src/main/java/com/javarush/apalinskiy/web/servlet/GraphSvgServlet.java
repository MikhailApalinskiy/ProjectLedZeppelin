package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.domain.quest.QuestNode;
import com.javarush.apalinskiy.infra.quest.InMemoryQuestStore;
import com.javarush.apalinskiy.application.quests.QuestAuthoringService;
import com.javarush.apalinskiy.web.util.Web;
import com.javarush.apalinskiy.web.util.WebConst;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.*;

public class GraphSvgServlet extends HttpServlet {

    private InMemoryQuestStore repo;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        ServletContext ctx = config.getServletContext();
        Object obj = ctx.getAttribute(WebConst.Ctx.EDITOR_REPOSITORY);
        if (!(obj instanceof InMemoryQuestStore r)) {
            throw new ServletException("Editor repository not found in ServletContext (attr: " + WebConst.Ctx.EDITOR_REPOSITORY + ")");
        }
        this.repo = r;
    }

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
            } catch (Exception e) {
                req.setAttribute(WebConst.Attr.ERROR, "Couldn't upload the quest: " + e.getMessage());
            }
        } else if (svc instanceof QuestAuthoringService a) {
            String editingId = (String) session.getAttribute(WebConst.Attr.EDITING_QUEST_ID);
            Object editingName = session.getAttribute(WebConst.Attr.EDITING_QUEST_NAME);
            if (editingId != null && (editingName == null || String.valueOf(editingName).isBlank())) {
                a.getFromCatalog(editingId)
                        .ifPresent(cq -> session.setAttribute(WebConst.Attr.EDITING_QUEST_NAME, cq.getName()));
            }
        }
        List<QuestNode> nodes = safeNodes();
        int startId = repo.startId();
        Web.buildQuestSvgModel(req, nodes, startId, true);
        Web.forward(req, resp, WebConst.Jsp.GRAPH_SVG);
    }

    private List<QuestNode> safeNodes() {
        try {
            return repo.nodes();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
