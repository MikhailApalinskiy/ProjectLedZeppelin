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

public class BaseSaveServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(BaseSaveServlet.class);

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


    protected String resolveQuestName(String questIdOrNull) {
        String name = Web.displayName(questIdOrNull, authoring);
        log.debug("resolveQuestName questId={} -> '{}'", questIdOrNull, name);
        return name;
    }

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

    protected String formatUpdated(Instant ts) {
        if (ts == null) {
            return null;
        }
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
        log.debug("buildSlotsAll userId={} total={}", userId, list.size());
        return list;
    }
}
