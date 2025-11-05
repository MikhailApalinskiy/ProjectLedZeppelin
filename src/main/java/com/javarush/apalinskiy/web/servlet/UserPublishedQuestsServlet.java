package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.domain.quest.custom.CustomQuest;
import com.javarush.apalinskiy.service.quest.QuestAuthoringService;
import com.javarush.apalinskiy.domain.user.Role;
import com.javarush.apalinskiy.domain.user.User;
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

/**
 * Servlet responsible for displaying all published (and possibly moderated)
 * quests of a specific user.
 *
 * <p>Supports pagination, search, and role-based access control:
 * <ul>
 *   <li>Regular visitors can view only published quests.</li>
 *   <li>The owner and administrators can see drafts or hidden quests as well.</li>
 * </ul>
 *
 * <p>Expected request parameters:
 * <ul>
 *   <li>{@code id} — the target user's ID (required)</li>
 *   <li>{@code page}, {@code size}, {@code q} — pagination and search parameters</li>
 * </ul>
 * </p>
 */
public class UserPublishedQuestsServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(UserPublishedQuestsServlet.class);

    private UserService userService;
    private QuestAuthoringService authoring;

    /**
     * Initializes servlet dependencies from the {@link ServletContext}.
     *
     * @throws UnavailableException if required services are missing
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
     * Handles displaying a list of quests published by a specific user.
     *
     * <p>Redirects to the home page with an error message if the user is not found
     * or if the {@code id} parameter is missing.</p>
     *
     * @param req  HTTP request (expects {@code id} parameter)
     * @param resp HTTP response
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        log.trace("UserQuestsServlet.doGet: start uri={} query={}", req.getRequestURI(), req.getQueryString());
        Web.pullFlash(req, WebConst.Attr.FLASH);
        Web.pullFlash(req, WebConst.Attr.ERROR);
        final String userId = Web.trimOrNull(req.getParameter("id"));
        if (userId == null) {
            log.warn("UserQuestsServlet.doGet: missing userId parameter, redirecting to HOME");
            Web.redirectErr(req, resp, WebConst.Path.HOME, "User id is required");
            return;
        }
        log.debug("UserQuestsServlet.doGet: userId param='{}'", userId);
        Optional<User> viewUserOpt = userService.findById(userId);
        if (viewUserOpt.isEmpty()) {
            log.warn("UserQuestsServlet.doGet: user not found id='{}'", userId);
            Web.redirectErr(req, resp, WebConst.Path.HOME, "User not found");
            return;
        }
        User viewUser = viewUserOpt.get();
        User me = (User) (req.getAttribute(WebConst.Attr.USER) != null
                ? req.getAttribute(WebConst.Attr.USER)
                : req.getSession().getAttribute(WebConst.Attr.USER));
        boolean isOwner = (me != null && Objects.equals(me.getUserId(), viewUser.getUserId()));
        boolean isAdmin = (me != null && me.getRole() == Role.ADMIN);
        boolean viewerIsOwnerOrAdmin = isOwner || isAdmin;
        log.debug("UserQuestsServlet.doGet: viewerId={} viewUserId={} isOwner={} isAdmin={}",
                (me != null ? me.getUserId() : "anon"), viewUser.getUserId(), isOwner, isAdmin);
        Web.Params params = Web.extract(req);
        log.trace("UserQuestsServlet.doGet: extracted params page={} size={} q='{}'",
                params.page, params.size, params.q);
        QuestAuthoringService.Paged<CustomQuest> paged = authoring.listOwnerFromCatalogPaged(viewUser.getUserId(), params.q, params.page, params.size, viewerIsOwnerOrAdmin);
        log.debug("UserQuestsServlet.doGet: fetched {} quests (total={}) for viewUserId={}",
                paged.getItems().size(), paged.getTotal(), viewUser.getUserId());
        Map<String, String> ownerNameById = Map.of(viewUser.getUserId(), viewUser.getUserName());
        req.setAttribute("ownerNameById", ownerNameById);
        req.setAttribute("viewUser", viewUser);
        req.setAttribute("items", paged.getItems());
        req.setAttribute("total", paged.getTotal());
        req.setAttribute("pages", paged.getPages());
        req.setAttribute("page", paged.getPage());
        req.setAttribute("q", params.q);
        String selfPathOnly = req.getContextPath() + req.getServletPath();
        req.setAttribute("selfPathOnly", selfPathOnly);
        req.setAttribute("viewUserId", viewUser.getUserId());
        log.debug("UserQuestsServlet.doGet: forwarding to JSP={} for viewUserId={}", WebConst.Jsp.USER_QUESTS, viewUser.getUserId());
        Web.forward(req, resp, WebConst.Jsp.USER_QUESTS);
    }
}
