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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Displays a user's published (and, for owners/admins, also unpublished) custom quests.
 * <p>
 * This controller renders the public list of quests for a given user (identified by the
 * {@code id} query parameter). If the request comes from the same user (owner) or an
 * administrator, the list includes all quests; otherwise only quests where
 * {@link CustomQuest#isPublished()} is {@code true} are shown.
 * </p>
 *
 * <h3>Dependencies</h3>
 * <ul>
 *   <li><b>Required:</b> {@link UserService} (user lookup)</li>
 *   <li><b>Required:</b> {@link QuestAuthoringService} (quest catalog access)</li>
 * </ul>
 *
 * <h3>Request parameters</h3>
 * <ul>
 *   <li><b>id</b> — user id whose quests should be shown.</li>
 * </ul>
 *
 * <h3>Model / view</h3>
 * <ul>
 *   <li>Sets {@code viewUser} — the user whose quests are listed (may be {@code null} if not found).</li>
 *   <li>Calls {@link Web#attachQuestLists(HttpServletRequest, java.util.List)} to attach
 *       filtered lists into the model (e.g., for pagination/sorting blocks in the JSP).</li>
 *   <li>Sets {@code selfUrl} — canonical URL of this listing (stable for pagination/links).</li>
 *   <li>Forwards to {@code WebConst.Jsp.USER_QUESTS}.</li>
 * </ul>
 *
 * <h3>Flash</h3>
 * <p>
 * Reads and exposes {@code FLASH} and {@code ERROR} messages using {@link Web#pullFlash}.
 * </p>
 *
 * @see UserService
 * @see QuestAuthoringService
 * @see CustomQuest
 * @see Role
 * @see WebConst
 * @see Web
 */
public class UserPublishedQuestsServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(UserPublishedQuestsServlet.class);

    private UserService userService;
    private QuestAuthoringService authoring;

    /**
     * Resolves {@link UserService} and {@link QuestAuthoringService} from the servlet context.
     *
     * @param config servlet config provided by the container
     * @throws ServletException if required services are missing
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
     * Renders the list of quests for the user specified by {@code id}.
     * <p>
     * Flow:
     * <ol>
     *   <li>Pull flash messages ({@code FLASH}, {@code ERROR}).</li>
     *   <li>Resolve {@code viewUser} via {@link UserService#findById(String)} (warn if missing or id absent).</li>
     *   <li>Load quests via {@link QuestAuthoringService#listOwnerFromCatalog(String)} when {@code viewUser} exists;
     *       otherwise, use an empty list.</li>
     *   <li>Determine viewer identity from session:
     *     <ul>
     *       <li><i>Owner</i>: session user id equals {@code viewUser.userId}</li>
     *       <li><i>Admin</i>: session user role equals {@link Role#ADMIN}</li>
     *     </ul>
     *   </li>
     *   <li>If viewer is neither owner nor admin, filter to published items only
     *       ({@code items = items.stream().filter(CustomQuest::isPublished)...}).</li>
     *   <li>Attach {@code viewUser}, quest lists ({@link Web#attachQuestLists}), and {@code selfUrl} to the request.</li>
     *   <li>Forward to {@code WebConst.Jsp.USER_QUESTS}.</li>
     * </ol>
     * </p>
     *
     * @param req  HTTP request (expects {@code id} query parameter)
     * @param resp HTTP response
     * @throws ServletException if forwarding fails
     * @throws IOException      on I/O errors
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Web.pullFlash(req, WebConst.Attr.FLASH);
        Web.pullFlash(req, WebConst.Attr.ERROR);
        String userId = Web.trimOrNull(req.getParameter("id"));
        User viewUser = null;
        if (userId != null) {
            Optional<User> opt = userService.findById(userId);
            viewUser = opt.orElse(null);
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
            items = authoring.listOwnerFromCatalog(viewUser.getUserLogin());
            int total = items.size();
            User me = (User) req.getSession().getAttribute(WebConst.Attr.USER);
            boolean isOwner = (me != null && me.getUserId() != null && me.getUserId().equals(viewUser.getUserId()));
            boolean isAdmin = (me != null && me.getRole() == Role.ADMIN);
            if (!isOwner && !isAdmin) {
                items = items.stream().filter(CustomQuest::isPublished).collect(Collectors.toList());
            }
            log.info("Published quests view for userId={} login={} requestedBy={} isOwner={} isAdmin={} total={} visible={}",
                    viewUser.getUserId(),
                    viewUser.getUserLogin(),
                    (me == null ? "anon" : me.getUserId()),
                    isOwner, isAdmin, total, items.size());
        }
        req.setAttribute("viewUser", viewUser);
        Web.attachQuestLists(req, items);
        String selfUrl = req.getContextPath() + req.getServletPath()
                + (userId == null ? "" : "?id=" + Web.urlEncode(userId));
        req.setAttribute("selfUrl", selfUrl);
        Web.forward(req, resp, WebConst.Jsp.USER_QUESTS);
    }
}
