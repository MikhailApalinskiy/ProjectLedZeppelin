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

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class BaseSaveServlet extends HttpServlet {

    protected transient SaveStateService saveState;
    protected transient QuestService questService;
    protected transient QuestAuthoringService authoring;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        ServletContext ctx = config.getServletContext();
        try {
            this.questService = Web.ctxBean(ctx, WebConst.Ctx.QUEST_SERVICE, QuestService.class);
            this.saveState = Web.ctxBean(ctx, WebConst.Ctx.SAVE_STATE_SERVICE, SaveStateService.class);
        } catch (IllegalStateException e) {
            throw new UnavailableException(e.getMessage());
        }
        Object as = ctx.getAttribute(WebConst.Ctx.AUTHORING_SERVICE);
        if (as instanceof QuestAuthoringService a) {
            this.authoring = a;
        }
    }

    protected User requireAuthOrRedirect(HttpServletRequest req, HttpServletResponse resp, String returnPath)
            throws IOException {
        User u = (User) req.getSession().getAttribute(WebConst.Attr.USER);
        if (u == null) {
            String next = req.getContextPath() + (returnPath.startsWith("/") ? returnPath : ("/" + returnPath));
            String loginUrl = req.getContextPath() + WebConst.Path.LOGIN + "?next=" + Web.urlEncode(next);
            resp.sendRedirect(resp.encodeRedirectURL(loginUrl));
            return null;
        }
        return u;
    }


    protected String resolveQuestName(String questIdOrNull) {
        return Web.displayName(questIdOrNull, authoring);
    }

    protected String titleFor(int nodeId, String questIdOrNull) {
        String qid = (questIdOrNull == null || questIdOrNull.isBlank()) ? "main" : questIdOrNull;
        if ("main".equals(qid)) {
            QuestNode n = questService.getById(nodeId);
            return (n != null) ? Web.shortTitle(n.getText()) : ("Node #" + nodeId);
        }
        if (authoring != null) {
            return authoring.getFromCatalog(qid)
                    .map(q -> {
                        for (QuestNode n : q.getNodes()) {
                            if (n.getId() == nodeId) {
                                return Web.shortTitle(n.getText());
                            }
                        }
                        return "Node #" + nodeId;
                    })
                    .orElse("Node #" + nodeId);
        }
        return "Node #" + nodeId;
    }

    protected String formatUpdated(Instant ts) {
        if (ts == null) return null;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                .withZone(ZoneId.systemDefault());
        return fmt.format(ts);
    }

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
        return list;
    }
}
