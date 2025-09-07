package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.domain.quest.Option;
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
import lombok.Getter;

import java.io.IOException;
import java.util.*;

public class GraphSvgServlet extends HttpServlet {

    private InMemoryQuestStore repo;

    @Getter
    public static class NodePos {
        private final int id, x, y;
        private final boolean fin, start;
        private final List<String> labelLines;

        public NodePos(int id, int x, int y, boolean fin, boolean start, List<String> labelLines) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.fin = fin;
            this.start = start;
            this.labelLines = (labelLines == null) ? List.of() : labelLines;
        }
    }

    @Getter
    public static class EdgeSeg {
        private final int from, to;
        private final String label;
        private final double sx, sy, tx, ty;

        public EdgeSeg(int from, int to, String label, double sx, double sy, double tx, double ty) {
            this.from = from;
            this.to = to;
            this.label = label;
            this.sx = sx;
            this.sy = sy;
            this.tx = tx;
            this.ty = ty;
        }
    }

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
        if (loadQuestId != null) {
            if (svc instanceof QuestAuthoringService a) {
                try {
                    a.loadToEditor(loadQuestId);
                    session.setAttribute(WebConst.Attr.EDITING_QUEST_ID, loadQuestId);
                    a.getFromCatalog(loadQuestId).ifPresent(cq -> session.setAttribute(WebConst.Attr.EDITING_QUEST_NAME, cq.getName()));
                    req.setAttribute(WebConst.Attr.OK, "The quest is uploaded to the editor");
                } catch (Exception e) {
                    req.setAttribute(WebConst.Attr.ERROR, "Couldn't upload the quest: " + e.getMessage());
                }
            }
        } else {
            if (svc instanceof QuestAuthoringService a) {
                String editingId = (String) session.getAttribute(WebConst.Attr.EDITING_QUEST_ID);
                Object editingName = session.getAttribute(WebConst.Attr.EDITING_QUEST_NAME);
                if (editingId != null && (editingName == null || String.valueOf(editingName).isBlank())) {
                    a.getFromCatalog(editingId).ifPresent(cq -> session.setAttribute(WebConst.Attr.EDITING_QUEST_NAME, cq.getName()));
                }
            }
        }
        List<QuestNode> nodes = safeNodes();
        int startId = repo.startId();
        if (Web.isEffectivelyEmpty(nodes)) {
            renderEmpty(req, resp);
            return;
        }
        Map<Integer, QuestNode> byId = new HashMap<>();
        for (QuestNode n : nodes) {
            byId.put(n.getId(), n);
        }
        Map<Integer, Integer> dist = new HashMap<>();
        Deque<Integer> dq = new ArrayDeque<>();
        if (byId.containsKey(startId)) {
            dist.put(startId, 0);
            dq.add(startId);
        }
        while (!dq.isEmpty()) {
            int v = dq.pollFirst();
            QuestNode qn = byId.get(v);
            if (qn == null || qn.isFin()) {
                continue;
            }
            for (Option o : qn.getOptions()) {
                Integer to = o.next();
                if (to == null || !byId.containsKey(to)) {
                    continue;
                }
                if (!dist.containsKey(to)) {
                    dist.put(to, dist.get(v) + 1);
                    dq.addLast(to);
                }
            }
        }
        Map<Integer, List<Integer>> layers = new TreeMap<>();
        for (Map.Entry<Integer, Integer> e : dist.entrySet()) {
            layers.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
        }
        List<Integer> unreachable = new ArrayList<>();
        for (QuestNode n : nodes) {
            if (!dist.containsKey(n.getId())) {
                unreachable.add(n.getId());
            }
        }
        int lastLayerIndex = layers.isEmpty() ? 0 : Collections.max(layers.keySet()) + 1;
        if (!unreachable.isEmpty()) {
            layers.put(lastLayerIndex, unreachable);
        }
        for (List<Integer> l : layers.values()) {
            l.sort(Integer::compare);
        }
        int nodeW = 160, nodeH = 60, hGap = 80, vGap = 120, padding = 40;
        int colsMax = 1;
        for (List<Integer> l : layers.values()) colsMax = Math.max(colsMax, l.size());
        int width = padding * 2 + colsMax * nodeW + Math.max(0, colsMax - 1) * hGap;
        int height = padding * 2 + layers.size() * nodeH + Math.max(0, layers.size() - 1) * vGap;
        Map<Integer, NodePos> pos = new HashMap<>();
        int row = 0;
        for (List<Integer> l : layers.values()) {
            int count = l.size();
            if (count == 0) {
                row++;
                continue;
            }
            int rowWidth = count * nodeW + (count - 1) * hGap;
            int x0 = (width - rowWidth) / 2;
            int y = padding + row * (nodeH + vGap);
            for (int i = 0; i < count; i++) {
                int id = l.get(i);
                QuestNode qn = byId.get(id);
                String snippet = Web.makeSnippet(qn == null ? null : qn.getText());
                List<String> labelLines = Arrays.asList(snippet.split("\n", -1));
                pos.put(id, new NodePos(id, x0 + i * (nodeW + hGap), y, qn != null && qn.isFin(), id == startId, labelLines));
            }
            row++;
        }
        if (pos.isEmpty()) {
            renderEmpty(req, resp);
            return;
        }
        List<EdgeSeg> edges = new ArrayList<>();
        double pad = 12.0;
        for (QuestNode from : nodes) {
            if (from.isFin()) {
                continue;
            }
            NodePos npFrom = pos.get(from.getId());
            if (npFrom == null) {
                continue;
            }
            double cx1 = npFrom.getX() + nodeW / 2.0;
            double cy1 = npFrom.getY() + nodeH / 2.0;
            for (Option o : from.getOptions()) {
                Integer to = o.next();
                if (to == null) {
                    continue;
                }
                NodePos npTo = pos.get(to);
                if (npTo == null) {
                    continue;
                }
                double cx2 = npTo.getX() + nodeW / 2.0;
                double cy2 = npTo.getY() + nodeH / 2.0;
                double dx = cx2 - cx1, dy = cy2 - cy1, len = Math.hypot(dx, dy);
                if (len == 0) {
                    len = 1;
                }
                double nx = dx / len, ny = dy / len;
                double sx = cx1 + nx * (nodeW / 2.0 - pad);
                double sy = cy1 + ny * (nodeH / 2.0 - pad);
                double tx = cx2 - nx * (nodeW / 2.0 - pad);
                double ty = cy2 - ny * (nodeH / 2.0 - pad);
                edges.add(new EdgeSeg(from.getId(), to, o.choice(), sx, sy, tx, ty));
            }
        }
        req.setAttribute("width", width);
        req.setAttribute("height", height);
        req.setAttribute("nodeW", nodeW);
        req.setAttribute("nodeH", nodeH);
        req.setAttribute("positions", pos.values());
        req.setAttribute("edges", edges);
        req.setAttribute("isEmpty", Boolean.FALSE);
        Web.forward(req, resp, WebConst.Jsp.GRAPH_SVG);
    }

    private List<QuestNode> safeNodes() {
        try {
            return repo.nodes();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private void renderEmpty(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("width", 800);
        req.setAttribute("height", 300);
        req.setAttribute("nodeW", 160);
        req.setAttribute("nodeH", 60);
        req.setAttribute("positions", List.<NodePos>of());
        req.setAttribute("edges", List.<EdgeSeg>of());
        req.setAttribute("isEmpty", Boolean.TRUE);
        Web.forward(req, resp, WebConst.Jsp.GRAPH_SVG);
    }
}
