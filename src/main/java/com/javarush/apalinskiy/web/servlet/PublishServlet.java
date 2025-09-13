package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.domain.user.Role;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.domain.quest.custom.CustomQuest;
import com.javarush.apalinskiy.service.quest.QuestAuthoringService;
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
 * Publishes a quest from the editor or submits it for moderation.
 *
 * <p><b>GET</b> validates the current editor draft via
 * {@link QuestAuthoringService#validateCurrentDraft()} and either blocks with a redirect and
 * error flash, or forwards to the publish confirmation page.</p>
 *
 * <p><b>POST</b> has two flows:</p>
 * <ul>
 *   <li><b>Editing existing quest</b> (there is {@code EDITING_QUEST_ID} in session):
 *     <ul>
 *       <li>Calls {@link QuestAuthoringService#updateExisting(String, boolean)}.</li>
 *       <li>If the actor is admin and edits someone else’s quest, notifies the owner via
 *           {@link BaseQuestAdminServlet#notifyQuestAdminChanged(User, String, String)}.</li>
 *       <li>Redirects to Home with either “Changes saved” (admin) or
 *           “Changes submitted for moderation”.</li>
 *     </ul>
 *   </li>
 *   <li><b>Publishing a new quest</b>:
 *     <ul>
 *       <li>Validates {@code questName} (non-blank, ≤ 100 chars).</li>
 *       <li>If admin: {@link QuestAuthoringService#publish(String, String)}, increments stats with
 *           {@link BaseQuestAdminServlet#incCreatedByUserId(String)}, notifies friends with
 *           {@link BaseQuestAdminServlet#notifyFriendsPublishedByUserId(String, String)}, then redirects OK.</li>
 *       <li>If not admin: {@link QuestAuthoringService#submitNewForModeration(String, String)}, then redirects OK.</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h3>Session / Context</h3>
 * <ul>
 *   <li>Reads {@link WebConst.Attr#USER} from session to identify actor and role.</li>
 *   <li>May read {@link WebConst.Attr#EDITING_QUEST_ID} to detect “edit existing” flow.</li>
 * </ul>
 *
 * <h3>View constants</h3>
 * <ul>
 *   <li>GET forwards to {@code WebConst.Jsp.PUBLISH} when the draft is valid.</li>
 *   <li>On validation errors, redirects back with message flashes using {@link Web} helpers.</li>
 * </ul>
 *
 * <h3>Security notes</h3>
 * <ul>
 *   <li>Mutating operations should be CSRF-protected by an upstream filter.</li>
 *   <li>Admin paths send notifications to affected users where applicable.</li>
 * </ul>
 *
 * @see BaseQuestAdminServlet
 * @see QuestAuthoringService
 * @see CustomQuest
 * @see WebConst
 * @see Web
 */
public class PublishServlet extends BaseQuestAdminServlet {

    private static final Logger log = LoggerFactory.getLogger(PublishServlet.class);

    /**
     * Validates the current editor draft and forwards to the publish page if valid.
     * <p>
     * Steps:
     * <ol>
     *   <li>Run {@link QuestAuthoringService#validateCurrentDraft()}.</li>
     *   <li>If there are errors, build a message, log the user (if any), and redirect with an error to
     *       {@code WebConst.Path.GRAPH_SVG}.</li>
     *   <li>Else copy potential flashes, then forward to {@code WebConst.Jsp.PUBLISH}.</li>
     * </ol>
     * </p>
     *
     * @param req  HTTP request
     * @param resp HTTP response
     * @throws ServletException if forwarding fails
     * @throws IOException      on I/O errors
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
     * Submits the current draft for publication or moderation.
     * <p>
     * Behavior:
     * <ul>
     *   <li>If {@code EDITING_QUEST_ID} exists in session, updates the existing quest (admin may apply immediately).</li>
     *   <li>Otherwise, validates {@code questName} and either publishes immediately (admin) or submits for moderation (non-admin).</li>
     *   <li>Uses {@link Web#redirectOk} / {@link Web#redirect} to deliver user-facing feedback.</li>
     * </ul>
     * </p>
     *
     * @param req  HTTP request (expects {@code questName} when creating a new publication)
     * @param resp HTTP response used for redirects
     * @throws IOException on redirect errors
     */
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
