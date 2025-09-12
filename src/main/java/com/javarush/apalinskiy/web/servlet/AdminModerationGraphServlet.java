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

public class AdminModerationGraphServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(AdminModerationGraphServlet.class);

    private QuestAuthoringService authoring;

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

    private static List<QuestNode> sanitize(List<QuestNode> src) {
        return (src == null) ? List.of() : src.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }
}
