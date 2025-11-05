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

/**
 * Servlet responsible for handling the publication process of a quest.
 *
 * <p>Validates the current draft before allowing publication. Depending on user role,
 * it either publishes immediately (admin) or submits the quest for moderation (regular user).</p>
 */
public class PublishServlet extends BaseQuestAdminServlet {

    private static final Logger log = LoggerFactory.getLogger(PublishServlet.class);

    /**
     * Validates the current draft and forwards to the publication confirmation page.
     *
     * <p>If validation fails, the user is redirected back to the editor graph view with an error message.</p>
     */
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

    /**
     * Handles quest publication or submission for moderation.
     *
     * <p>If the quest is being edited and has an existing ID, the changes are updated.
     * Otherwise, a new quest is published or sent for moderation based on user role.</p>
     *
     * @param req  HTTP request
     * @param resp HTTP response
     * @throws IOException if redirect fails
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = (User) req.getSession().getAttribute(WebConst.Attr.USER);
        String ownerId = (user != null) ? user.getUserId() : null;
        boolean isAdmin = (user != null && user.getRole() == Role.ADMIN);
        HttpSession s = req.getSession(false);
        String editingId = (s != null) ? (String) s.getAttribute(WebConst.Attr.EDITING_QUEST_ID) : null;
        try {
            if (editingId != null && !editingId.isBlank()) {
                Optional<CustomQuest> cqOpt = authoring.getFromCatalog(editingId);
                String questName = cqOpt.map(CustomQuest::getName).orElse("Quest");
                String questOwnerId = cqOpt.map(CustomQuest::getOwnerId).orElse(null);
                authoring.updateExisting(editingId, isAdmin);
                log.info("Quest update submitted questId={} byUserId={} isAdmin={}", editingId, ownerId, isAdmin);
                if (isAdmin && questOwnerId != null && !questOwnerId.equals(ownerId)) {
                    notifyQuestAdminChangedById(user, questOwnerId, questName);
                    log.info("Owner notified about admin update questId={} ownerId={}", editingId, questOwnerId);
                }
                Web.redirectOk(req, resp, WebConst.Path.HOME,
                        isAdmin ? "Changes saved" : "Changes submitted for moderation");
                return;
            }
            String questNameRaw = req.getParameter("questName");
            String questName = questNameRaw == null ? "" : questNameRaw.trim();
            if (questName.isBlank()) {
                log.warn("Publish POST: empty quest name byUserId={}", ownerId);
                Web.redirect(req, resp, WebConst.Path.PUBLISH,
                        Map.of(WebConst.Attr.ERROR, "Specify the name of the quest",
                                "questName", String.valueOf(questNameRaw)));
                return;
            }
            if (questName.length() > 100) {
                log.warn("Publish POST: too long quest name (len={}) byUserId={}", questName.length(), ownerId);
                Web.redirect(req, resp, WebConst.Path.PUBLISH,
                        Map.of(WebConst.Attr.ERROR, "The name is too long, maximum length is 50 characters",
                                "questName", questName));
                return;
            }
            if (isAdmin) {
                authoring.publish(ownerId, questName);
                incCreatedByUserId(ownerId);
                notifyFriendsPublishedByUserId(ownerId, questName);
                log.info("Quest published by admin ownerId={} name='{}'", ownerId, questName);
                Web.redirectOk(req, resp, WebConst.Path.HOME, "The quest has been published");
            } else {
                authoring.submitNewForModeration(ownerId, questName);
                log.info("Quest submitted for moderation by ownerId={} name='{}'", ownerId, questName);
                Web.redirectOk(req, resp, WebConst.Path.HOME, "The quest has been submitted for moderation");
            }
        } catch (IllegalArgumentException | IllegalStateException ex) {
            String msg = "Publication failed: " + ex.getMessage();
            log.warn("Publish POST failed byUserId={} reason={}", ownerId, ex.getMessage());
            Web.redirect(req, resp, WebConst.Path.PUBLISH,
                    Map.of(WebConst.Attr.ERROR, msg,
                            "questName", String.valueOf(req.getParameter("questName"))));
        }
    }
}
