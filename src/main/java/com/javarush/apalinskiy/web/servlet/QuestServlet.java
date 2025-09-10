package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.domain.quest.index.QuestNavigator;
import com.javarush.apalinskiy.domain.quest.QuestNode;
import com.javarush.apalinskiy.service.quest.QuestAuthoringService;
import com.javarush.apalinskiy.service.quest.QuestService;
import com.javarush.apalinskiy.domain.quest.choice.ChoiceError;
import com.javarush.apalinskiy.domain.quest.choice.ChooseResult;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.domain.quest.custom.CustomQuest;
import com.javarush.apalinskiy.service.user.UserStatsService;
import com.javarush.apalinskiy.web.util.Web;
import com.javarush.apalinskiy.app.WebConst;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Optional;

public class QuestServlet extends HttpServlet {

    private transient QuestService prodService;
    private transient QuestAuthoringService authoring;
    private transient UserStatsService userStats;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        ServletContext ctx = config.getServletContext();
        try {
            this.prodService = Web.ctxBean(ctx, WebConst.Ctx.QUEST_SERVICE, QuestService.class);
        } catch (IllegalStateException e) {
            throw new UnavailableException("QuestService is not initialized");
        }
        Object a = ctx.getAttribute(WebConst.Ctx.AUTHORING_SERVICE);
        if (a instanceof QuestAuthoringService as) {
            this.authoring = as;
        }
        try {
            this.userStats = Web.ctxBean(ctx, WebConst.Ctx.USER_STATS_SERVICE, UserStatsService.class);
        } catch (IllegalStateException ignore) {
            this.userStats = null;
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String customId = Web.normalizedCustomParam(req);
        if (Web.isMissingCustomId(customId, authoring)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Custom quest not found or was deleted");
            return;
        }
        TempService svc = (customId == null) ? TempService.from(prodService) : resolveCustomService(customId);
        Integer id = Web.firstIntParam(req, WebConst.Param.ID, WebConst.Param.NODE);
        QuestNode node = (id == null) ? svc.getStart() : svc.getById(id);
        if (node == null) {
            req.setAttribute(WebConst.Attr.ERROR, WebConst.Msg.NODE_NOT_FOUND_PREFIX + id);
            node = svc.getStart();
        }
        Web.pullFlash(req, WebConst.Attr.FLASH);
        req.setAttribute(WebConst.Attr.CUSTOM, customId);
        req.setAttribute("questTitle", Web.displayName(customId, authoring));
        forwardQuest(req, resp, node, svc.version(), null);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String customId = Web.normalizedCustomParam(req);
        if (Web.isMissingCustomId(customId, authoring)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Custom quest not found or was deleted");
            return;
        }
        TempService svc = (customId == null) ? TempService.from(prodService) : resolveCustomService(customId);
        Integer fromId = Web.parseIntOrNull(req.getParameter(WebConst.Param.FROM_ID));
        if (fromId == null) {
            forwardQuest(req, resp, svc.getStart(), svc.version(), WebConst.Msg.BAD_FROM_ID);
            return;
        }
        String answer = req.getParameter(WebConst.Param.ANSWER);
        ChooseResult result = svc.choose(fromId, answer);
        if (result.isOk()) {
            QuestNode next = result.getNext();
            if (next != null && next.isFin() && userStats != null) {
                User u = (User) req.getSession().getAttribute(WebConst.Attr.USER);
                if (u != null) {
                    String questKey = (customId == null) ? "main" : customId;
                    Integer finalId = (customId == null) ? next.getId() : null;
                    userStats.onQuestCompleted(u.getUserId(), questKey, finalId);
                }
            }
            if (next == null) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Next node is null");
                return;
            }
            resp.sendRedirect(resp.encodeRedirectURL(Web.questUrl(req, next.getId(), customId)));
        } else {
            QuestNode node = svc.getById(fromId);
            if (node == null) {
                node = svc.getStart();
            }
            req.setAttribute(WebConst.Attr.CUSTOM, customId);
            forwardQuest(req, resp, node, svc.version(), result.getMessage());
        }
    }

    private TempService resolveCustomService(String customId) throws ServletException {
        if (authoring == null) {
            throw new UnavailableException("Authoring service is not available");
        }
        CustomQuest q = authoring.getFromCatalog(customId).orElseThrow(
                () -> new UnavailableException("Custom quest is missing"));
        QuestNavigator nav = QuestNavigator.from(q.getNodes(), q.getStartId());
        return TempService.from(nav, "custom:" + q.getId());
    }

    private void forwardQuest(HttpServletRequest req, HttpServletResponse resp,
                              QuestNode node, String version, String error)
            throws ServletException, IOException {
        req.setAttribute(WebConst.Attr.NODE, node);
        req.setAttribute(WebConst.Attr.VERSION, version);
        if (error != null && !error.isBlank()) {
            req.setAttribute(WebConst.Attr.ERROR, error);
        }
        Web.forward(req, resp, WebConst.Jsp.QUEST);
    }

    private interface TempService {
        QuestNode getStart();

        QuestNode getById(int id);

        ChooseResult choose(int fromId, String answer);

        String version();

        static TempService from(QuestService prod) {
            return new TempService() {
                @Override
                public QuestNode getStart() {
                    return prod.getStart();
                }

                @Override
                public QuestNode getById(int id) {
                    return prod.getById(id);
                }

                @Override
                public ChooseResult choose(int fromId, String answer) {
                    return prod.choose(fromId, answer);
                }

                @Override
                public String version() {
                    return prod.version();
                }
            };
        }

        static TempService from(QuestNavigator nav, String version) {
            return new TempService() {
                @Override
                public QuestNode getStart() {
                    return nav.start();
                }

                @Override
                public QuestNode getById(int id) {
                    return nav.get(id);
                }

                @Override
                public ChooseResult choose(int fromId, String answer) {
                    Optional<QuestNode> opt = nav.choose(fromId, answer);
                    return opt.map(ChooseResult::ok)
                            .orElseGet(() -> ChooseResult.error(ChoiceError.NO_SUCH_OPTION, "No such option: "));
                }

                @Override
                public String version() {
                    return version;
                }
            };
        }
    }
}