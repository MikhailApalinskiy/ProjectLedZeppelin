package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.service.quest.QuestAuthoringService;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.service.notify.NotificationService;
import com.javarush.apalinskiy.domain.notify.NotificationType;
import com.javarush.apalinskiy.domain.quest.custom.CustomQuest;
import com.javarush.apalinskiy.service.user.UserService;
import com.javarush.apalinskiy.web.util.Web;
import com.javarush.apalinskiy.app.WebConst;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;


public class DeleteQuestServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(DeleteQuestServlet.class);

    private transient QuestAuthoringService authoring;
    private transient NotificationService notifications;
    private transient UserService users;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            this.authoring = Web.ctxBean(
                    config.getServletContext(),
                    WebConst.Ctx.AUTHORING_SERVICE,
                    QuestAuthoringService.class
            );
        } catch (IllegalStateException e) {
            log.error("Init failed: QuestAuthoringService missing", e);
            throw new UnavailableException("QuestAuthoringService not found in ServletContext");
        }
        try {
            this.notifications = Web.ctxBean(
                    config.getServletContext(),
                    WebConst.Ctx.NOTIFY_SERVICE,
                    NotificationService.class
            );
        } catch (IllegalStateException ignore) {
        }
        try {
            this.users = Web.ctxBean(
                    config.getServletContext(),
                    WebConst.Ctx.USER_SERVICE,
                    UserService.class
            );
        } catch (IllegalStateException ignore) {
        }
        log.debug("DeleteQuestServlet initialized: authoring={}, notifications={}, users={}",
                (authoring == null ? "null" : authoring.getClass().getSimpleName()),
                (notifications == null ? "null" : notifications.getClass().getSimpleName()),
                (users == null ? "null" : users.getClass().getSimpleName()));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User actor = (User) req.getSession().getAttribute(WebConst.Attr.USER);
        if (actor == null) {
            log.info("Delete quest: anonymous user -> redirect to login");
            Web.redirect(req, resp, WebConst.Path.LOGIN, java.util.Map.of());
            return;
        }
        String questId = Web.trimOrNull(req.getParameter(WebConst.Param.ID));
        String next = Web.safeNextOrHome(req, req.getParameter(WebConst.Param.NEXT));
        if (next == null) {
            next = req.getContextPath() + WebConst.Path.MY_QUESTS;
        }
        if (questId == null) {
            log.warn("Delete quest: missing questId by actorId={}", actor.getUserId());
            req.getSession().setAttribute(WebConst.Attr.ERROR, "The quest ID is not specified.");
            resp.sendRedirect(resp.encodeRedirectURL(next));
            return;
        }
        Optional<CustomQuest> questOpt = authoring.getFromCatalog(questId);
        String questName = questOpt.map(CustomQuest::getName).orElse("Quest");
        String ownerLogin = questOpt.map(CustomQuest::getOwnerLogin).orElse(null);
        try {
            boolean isAdmin = "ADMIN".equals(String.valueOf(actor.getRole()));
            log.info("Delete quest attempt questId={} by actorId={} isAdmin={}", questId, actor.getUserId(), isAdmin);
            boolean removed = isAdmin
                    ? authoring.deleteFromCatalogAsAdmin(questId)
                    : authoring.deleteFromCatalogIfOwner(questId, actor.getUserLogin());
            if (removed) {
                req.getSession().setAttribute(WebConst.Attr.FLASH, "The quest has been deleted.");
                log.info("Delete quest success questId={} by actorId={} questName='{}'", questId, actor.getUserId(), questName);
                if (isAdmin && ownerLogin != null && notifications != null && users != null) {
                    Optional<User> ownerOpt = users.findByLogin(ownerLogin);
                    if (ownerOpt.isPresent()) {
                        String ownerUserId = ownerOpt.get().getUserId();
                        if (!ownerUserId.equals(actor.getUserId())) {
                            String shortTitle = Web.shortTitle(questName);
                            notifications.add(
                                    ownerUserId,
                                    NotificationType.QUEST_ADMIN_CHANGED,
                                    "The administrator deleted your quest.\n",
                                    "Quest <b>" + shortTitle + "</b> was deleted by the administrator."
                            );
                            log.info("Owner notified about deletion ownerId={} questId={}", ownerUserId, questId);
                        }
                    } else {
                        log.warn("Owner login not found for questId={} ownerLogin='{}'", questId, ownerLogin);
                    }
                }
            } else {
                String msg = isAdmin
                        ? "Cannot be deleted: not found."
                        : "Cannot be deleted: not found or you are not the owner.";
                req.getSession().setAttribute(WebConst.Attr.ERROR, msg);
                log.warn("Delete quest failed questId={} by actorId={} reason='{}'", questId, actor.getUserId(), msg);
            }
        } catch (IllegalArgumentException | IllegalStateException ex) {
            req.getSession().setAttribute(WebConst.Attr.ERROR, "Deletion error: " + ex.getMessage());
            log.warn("Delete quest exception questId={} by actorId={} msg={}", questId, actor.getUserId(), ex.getMessage());
        }
        resp.sendRedirect(resp.encodeRedirectURL(next));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        log.warn("Delete quest GET not allowed");
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }
}
