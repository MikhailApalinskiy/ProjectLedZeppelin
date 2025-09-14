package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.service.quest.QuestAuthoringService;
import com.javarush.apalinskiy.domain.user.Role;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.domain.quest.custom.CustomQuest;
import com.javarush.apalinskiy.service.user.UserService;
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

/**
 * Servlet responsible for displaying quests published by a specific user.
 * <p>
 * Provides a read-only view of a user's authored quests.
 * The result set depends on the role of the viewer:
 * <ul>
 *   <li>When the viewer is the same user (owner), all their quests are visible.</li>
 *   <li>When the viewer is an admin, all the user's quests are visible.</li>
 *   <li>When the viewer is another regular user or anonymous, only published quests are visible.</li>
 * </ul>
 * <p>
 * <b>Initialization:</b><br>
 * Requires {@link UserService} and {@link QuestAuthoringService} to be present
 * in the servlet context. If either is missing, the servlet is marked unavailable.
 * <p>
 * <b>GET:</b><br>
 * Accepts a query parameter {@code id} representing the target user ID.
 * Retrieves the list of quests authored by that user, applies visibility rules,
 * and forwards the request to {@link WebConst.Jsp#USER_QUESTS}.
 * <p>
 * Sets the following request attributes for the JSP:
 * <ul>
 *   <li>{@code ownerNameById} — mapping of userId → userName for display</li>
 *   <li>{@code viewUser} — the {@link User} whose quests are being viewed</li>
 *   <li>{@code selfUrl} — current servlet URL including userId parameter</li>
 *   <li>Quest lists attached via {@link Web#attachQuestLists(HttpServletRequest, List)}</li>
 * </ul>
 *
 * <b>Security:</b> Does not block anonymous access. Visibility rules enforce
 * that only published quests are shown to non-owners/non-admins.
 *
 * @author Your Name
 * @since 1.0
 */
public class UserPublishedQuestsServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(UserPublishedQuestsServlet.class);

    /**
     * Service for user lookups and profile resolution.
     */
    private UserService userService;

    /**
     * Service for quest catalog access and queries.
     */
    private QuestAuthoringService authoring;

    /**
     * Initializes servlet dependencies by retrieving required services from the servlet context.
     * <p>
     * Required:
     * <ul>
     *   <li>{@link UserService}</li>
     *   <li>{@link QuestAuthoringService}</li>
     * </ul>
     * If either is missing, the servlet is marked unavailable.
     *
     * @param config servlet configuration
     * @throws ServletException     if superclass init fails
     * @throws UnavailableException if required services cannot be found
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        ServletContext ctx = config.getServletContext();
        try {
            this.userService = Web.ctxBean(ctx, WebConst.Ctx.USER_SERVICE, UserService.class);
            this.authoring = Web.ctxBean(ctx, WebConst.Ctx.AUTHORING_SERVICE, QuestAuthoringService.class);
        } catch (IllegalStateException e) {
            log.error("Init failed: {}", e.getMessage());
            throw new UnavailableException("Required services not found: " + e.getMessage());
        }
        log.debug("UserPublishedQuestsServlet initialized (userService={}, authoring={})",
                userService.getClass().getSimpleName(),
                authoring.getClass().getSimpleName());
    }

    /**
     * Handles GET requests by showing quests authored by a given user.
     * <p>
     * Behavior:
     * <ul>
     *   <li>Reads {@code id} parameter for the target user ID.</li>
     *   <li>If user not found, logs a warning and returns an empty list.</li>
     *   <li>If user is found, retrieves all quests authored by them.</li>
     *   <li>Determines viewer (session user) and applies visibility rules:
     *     <ul>
     *       <li>Owner or admin: see all quests.</li>
     *       <li>Others: see only published quests.</li>
     *     </ul>
     *   </li>
     *   <li>Logs the request with details of visibility filtering.</li>
     *   <li>Attaches attributes: {@code ownerNameById}, {@code viewUser}, {@code selfUrl}.</li>
     *   <li>Delegates to {@link Web#attachQuestLists} to prepare quest lists for the JSP.</li>
     *   <li>Forwards to {@link WebConst.Jsp#USER_QUESTS}.</li>
     * </ul>
     *
     * @param req  current HTTP request
     * @param resp current HTTP response
     * @throws ServletException if forwarding fails
     * @throws IOException      if forwarding fails
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Web.pullFlash(req, WebConst.Attr.FLASH);
        Web.pullFlash(req, WebConst.Attr.ERROR);
        String userId = Web.trimOrNull(req.getParameter("id"));
        User viewUser = null;
        if (userId != null) {
            viewUser = userService.findById(userId).orElse(null);
            if (viewUser == null) {
                log.warn("Published quests requested but user not found id={}", userId);
            }
        } else {
            log.warn("Published quests requested without id param");
        }
        List<CustomQuest> items;
        if (viewUser == null) {
            items = Collections.emptyList();
        } else {
            items = authoring.listOwnerFromCatalog(viewUser.getUserId());
            User me = (User) (req.getAttribute(WebConst.Attr.USER) != null
                    ? req.getAttribute(WebConst.Attr.USER)
                    : req.getSession().getAttribute(WebConst.Attr.USER));
            boolean isOwner = (me != null && Objects.equals(me.getUserId(), viewUser.getUserId()));
            boolean isAdmin = (me != null && me.getRole() == Role.ADMIN);
            int total = items.size();
            if (!isOwner && !isAdmin) {
                items = items.stream().filter(CustomQuest::isPublished).collect(Collectors.toList());
            }
            log.info("Published quests view for userId={} login={} requestedBy={} isOwner={} isAdmin={} total={} visible={}",
                    viewUser.getUserId(),
                    viewUser.getUserLogin(),
                    (me == null ? "anon" : me.getUserId()),
                    isOwner, isAdmin, total, items.size());
        }
        Map<String, String> ownerNameById = new HashMap<>();
        if (viewUser != null) {
            ownerNameById.put(viewUser.getUserId(), viewUser.getUserName());
        }
        req.setAttribute("ownerNameById", ownerNameById);
        req.setAttribute("viewUser", viewUser);
        Web.attachQuestLists(req, items);
        String selfUrl = req.getContextPath() + req.getServletPath()
                + (userId == null ? "" : "?id=" + Web.urlEncode(userId));
        req.setAttribute("selfUrl", selfUrl);
        Web.forward(req, resp, WebConst.Jsp.USER_QUESTS);
    }
}
