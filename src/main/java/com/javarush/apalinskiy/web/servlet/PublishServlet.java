package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.domain.user.Role;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.domain.quest.custom.CustomQuest;
import com.javarush.apalinskiy.web.util.Web;
import com.javarush.apalinskiy.app.WebConst;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PublishServlet extends BaseQuestAdminServlet {

    private static final Logger log = LoggerFactory.getLogger(PublishServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        List<String> errs = authoring.validateCurrentDraft();
        if (!errs.isEmpty()) {
            String msg = "You can't go to the publication: " + String.join(" ", errs);
            String uid = "anon";
            HttpSession sess = req.getSession(false);
            if (sess != null) {
                Object uObj = sess.getAttribute(WebConst.Attr.USER);
                if (uObj instanceof User u) {
                    uid = u.getUserId();
                }
            }
            log.warn("Publish GET blocked: {} (userId={})", msg, uid);
            Web.redirectErr(req, resp, WebConst.Path.GRAPH_SVG, msg);
            return;
        }
        log.debug("Publish GET allowed (draft valid)");
        Web.copyParamsToAttrs(req, WebConst.Attr.ERROR, WebConst.Attr.OK);
        Web.forward(req, resp, WebConst.Jsp.PUBLISH);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = (User) req.getSession().getAttribute(WebConst.Attr.USER);
        String owner = (user != null) ? user.getUserLogin() : "anonymous";
        boolean isAdmin = (user != null && user.getRole() == Role.ADMIN);
        HttpSession s = req.getSession(false);
        String editingId = (s != null) ? (String) s.getAttribute(WebConst.Attr.EDITING_QUEST_ID) : null;
        try {
            if (editingId != null && !editingId.isBlank()) {
                Optional<CustomQuest> cqOpt = authoring.getFromCatalog(editingId);
                String questName = cqOpt.map(CustomQuest::getName).orElse("Quest");
                String ownerLogin = cqOpt.map(CustomQuest::getOwnerLogin).orElse(null);
                authoring.updateExisting(editingId, isAdmin);
                log.info("Quest update submitted questId={} by={} isAdmin={}", editingId, owner, isAdmin);
                if (isAdmin && ownerLogin != null && !ownerLogin.equals(owner)) {
                    notifyQuestAdminChanged(user, ownerLogin, questName);
                    log.info("Owner notified about admin update questId={} ownerLogin={}", editingId, ownerLogin);
                }
                Web.redirectOk(req, resp, WebConst.Path.HOME,
                        isAdmin ? "Changes saved" : "Changes submitted for moderation");
                return;
            }
            String questNameRaw = req.getParameter("questName");
            String questName = questNameRaw == null ? "" : questNameRaw.trim();
            if (questName.isBlank()) {
                log.warn("Publish POST: empty quest name by={}", owner);
                Web.redirect(req, resp, WebConst.Path.PUBLISH,
                        Map.of(WebConst.Attr.ERROR, "Specify the name of the quest",
                                "questName", String.valueOf(questNameRaw)));
                return;
            }
            if (questName.length() > 100) {
                log.warn("Publish POST: too long quest name (len={}) by={}", questName.length(), owner);
                Web.redirect(req, resp, WebConst.Path.PUBLISH,
                        Map.of(WebConst.Attr.ERROR, "The name is too long (maximum 100 characters)",
                                "questName", questName));
                return;
            }
            if (isAdmin) {
                authoring.publish(owner, questName);
                incCreatedByUserId(user.getUserId());
                notifyFriendsPublishedByUserId(user.getUserId(), questName);
                log.info("Quest published by admin ownerLogin={} userId={} name='{}'",
                        owner, user.getUserId(), questName);
                Web.redirectOk(req, resp, WebConst.Path.HOME, "The quest has been published");
            } else {
                authoring.submitNewForModeration(owner, questName);
                log.info("Quest submitted for moderation by ownerLogin={} name='{}'", owner, questName);
                Web.redirectOk(req, resp, WebConst.Path.HOME, "The quest has been submitted for moderation");
            }
        } catch (IllegalArgumentException | IllegalStateException ex) {
            String msg = "Publication failed: " + ex.getMessage();
            log.warn("Publish POST failed by={} reason={}", owner, ex.getMessage());
            Web.redirect(req, resp, WebConst.Path.PUBLISH,
                    Map.of(WebConst.Attr.ERROR, msg,
                            "questName", String.valueOf(req.getParameter("questName"))));
        }
    }
}
