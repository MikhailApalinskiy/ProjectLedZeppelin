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
 * Servlet responsible for publishing quests.
 * <p>
 * Provides both the publication form (via <b>GET</b>) and handles submission (via <b>POST</b>).
 * <p>
 * Workflow:
 * <ul>
 *   <li><b>GET</b>:
 *     <ul>
 *       <li>Validates the current draft via {@link QuestAuthoringService#validateCurrentDraft()}.</li>
 *       <li>If invalid, blocks navigation and redirects back to the graph view with an error message.</li>
 *       <li>If valid, forwards to the publish JSP.</li>
 *     </ul>
 *   </li>
 *   <li><b>POST</b>:
 *     <ul>
 *       <li>If editing an existing quest (session contains {@code EDITING_QUEST_ID}), updates it directly.</li>
 *       <li>If publishing a new quest:
 *         <ul>
 *           <li>Validates quest name (non-blank, max length 100).</li>
 *           <li>If admin:
 *             <ul>
 *               <li>Publishes immediately.</li>
 *               <li>Increments user stats and notifies friends.</li>
 *             </ul>
 *           </li>
 *           <li>If not admin:
 *             <ul>
 *               <li>Submits the quest for moderation.</li>
 *             </ul>
 *           </li>
 *         </ul>
 *       </li>
 *       <li>On error, redirects back to the publish form with a descriptive error message.</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <b>Security:</b> Requires an authenticated user in the session. Role {@link Role#ADMIN}
 * grants immediate publish rights, otherwise moderation is required.
 *
 * @author Your Name
 * @since 1.0
 */
public class PublishServlet extends BaseQuestAdminServlet {

    private static final Logger log = LoggerFactory.getLogger(PublishServlet.class);

    /**
     * Handles GET requests for the publish page.
     * <p>
     * Behavior:
     * <ul>
     *   <li>Validates the current draft.</li>
     *   <li>If invalid, prevents navigation to publish page and redirects to graph view with error.</li>
     *   <li>If valid, copies error/ok params and forwards to {@link WebConst.Jsp#PUBLISH}.</li>
     * </ul>
     *
     * @param req  current HTTP request
     * @param resp current HTTP response
     * @throws ServletException if forwarding to JSP fails
     * @throws IOException      if redirect/forward fails
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
     * Handles POST requests to publish or update a quest.
     * <p>
     * Behavior:
     * <ul>
     *   <li>Resolves current user and determines if they are admin.</li>
     *   <li>If session contains {@code EDITING_QUEST_ID}, updates the existing quest.
     *       Admin updates trigger notifications to the quest owner.</li>
     *   <li>If no editing id is present:
     *     <ul>
     *       <li>Validates quest name (not blank, ≤100 chars).</li>
     *       <li>Admin flow:
     *         <ul>
     *           <li>Publishes quest immediately.</li>
     *           <li>Increments created-quests counter.</li>
     *           <li>Notifies friends about the publication.</li>
     *         </ul>
     *       </li>
     *       <li>User flow:
     *         <ul>
     *           <li>Submits quest for moderation.</li>
     *         </ul>
     *       </li>
     *     </ul>
     *   </li>
     *   <li>On validation or state errors, redirects back to publish page with an error.</li>
     * </ul>
     *
     * @param req  current HTTP request
     * @param resp current HTTP response
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
                        Map.of(WebConst.Attr.ERROR, "The name is too long (maximum 100 characters)",
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
