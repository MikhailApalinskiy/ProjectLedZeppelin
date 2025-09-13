package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.domain.user.UserStats;
import com.javarush.apalinskiy.service.user.UserService;
import com.javarush.apalinskiy.service.user.UserStatsService;
import com.javarush.apalinskiy.web.util.Web;
import com.javarush.apalinskiy.app.WebConst;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

/**
 * Serves public, read-only user profile pages.
 * <p>
 * The servlet looks up a user by the {@code id} query parameter and renders a public profile
 * view. If available, aggregate statistics are also attached to the model.
 * </p>
 *
 * <h3>Dependencies</h3>
 * <ul>
 *   <li><b>Required</b>: {@link UserService} (resolved from {@code WebConst.Ctx.USER_SERVICE}).</li>
 *   <li><b>Optional</b>: {@link UserStatsService} (resolved from {@code WebConst.Ctx.USER_STATS_SERVICE}).</li>
 * </ul>
 *
 * <h3>View</h3>
 * <ul>
 *   <li>Forwards to {@code WebConst.Jsp.USER_PUBLIC}.</li>
 *   <li>Request attributes:
 *     <ul>
 *       <li>{@code profileUser} — the user being viewed (may be {@code null} if not found).</li>
 *       <li>{@code stats} — optional {@link UserStats} for the viewed user.</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h3>Notes</h3>
 * <ul>
 *   <li>Authentication is not required; this is a public endpoint.</li>
 *   <li>Flash and query parameters ({@code OK}, {@code ERROR}) are copied to attributes for the view.</li>
 * </ul>
 */
public class UserPublicProfileServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(UserPublicProfileServlet.class);

    private transient UserService userService;
    private transient UserStatsService userStatsService;

    /**
     * Resolves required and optional services from the servlet context.
     *
     * @param config servlet config provided by the container
     * @throws ServletException if required services are missing
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        this.userService = Web.ctxBean(config.getServletContext(), WebConst.Ctx.USER_SERVICE, UserService.class);
        try {
            this.userStatsService = Web.ctxBean(config.getServletContext(), WebConst.Ctx.USER_STATS_SERVICE, UserStatsService.class);
        } catch (IllegalStateException ignore) {
            this.userStatsService = null;
        }
    }

    /**
     * Renders a public profile for the user identified by the {@code id} query parameter.
     * <p>
     * Flow:
     * <ol>
     *   <li>Copy optional flash/query params ({@code OK}, {@code ERROR}) to request attributes.</li>
     *   <li>Read {@code id}; if present, try {@link UserService#findById(String)}.</li>
     *   <li>If found, set {@code profileUser} and (optionally) {@code stats} via {@link UserStatsService#statsOf(String)}.</li>
     *   <li>Forward to {@code WebConst.Jsp.USER_PUBLIC} regardless of existence (view decides what to show).</li>
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
        Web.copyParamsToAttrs(req, WebConst.Attr.OK, WebConst.Attr.ERROR);
        Web.pullFlash(req, WebConst.Attr.OK);
        Web.pullFlash(req, WebConst.Attr.ERROR);
        String userId = Web.trimOrNull(req.getParameter("id"));
        User viewUser = null;
        if (userId != null) {
            Optional<User> opt = userService.findById(userId);
            viewUser = opt.orElse(null);
            if (viewUser != null) {
                log.info("Public profile view userId={} login={}", viewUser.getUserId(), viewUser.getUserLogin());
            } else {
                log.warn("Public profile requested but not found id={}", userId);
            }
        } else {
            log.warn("Public profile requested without id param");
        }
        req.setAttribute("profileUser", viewUser);
        if (viewUser != null && userStatsService != null) {
            req.setAttribute("stats", userStatsService.statsOf(viewUser.getUserId()));
        }
        Web.forward(req, resp, WebConst.Jsp.USER_PUBLIC);
    }
}
