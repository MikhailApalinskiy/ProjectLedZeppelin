package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.service.user.UserService;
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
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Admin/user-directory controller that lists users or searches by id/login.
 * <p>
 * <b>GET</b> without a query parameter returns the full list from {@link UserService#findAll()}.
 * When the optional query parameter {@code q} is provided, the servlet attempts to resolve it
 * first as a user <i>id</i>, then (on miss) as a user <i>login</i> (case-insensitive),
 * and forwards a single-element list on hit or an empty list on miss.
 * </p>
 *
 * <h3>Request parameter</h3>
 * <ul>
 *   <li><b>q</b> — optional; user id or login to search for.</li>
 * </ul>
 *
 * <h3>Model / view</h3>
 * <ul>
 *   <li>Sets request attribute {@code users} with the resulting list.</li>
 *   <li>Copies/pulls flashes for {@code OK} and {@code ERROR} into attributes.</li>
 *   <li>Forwards to {@code WebConst.Jsp.USERS}.</li>
 * </ul>
 *
 * @see UserService
 * @see Web
 * @see WebConst
 */
public class UsersServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(UsersServlet.class);

    private UserService userService;

    /**
     * Resolves {@link UserService} from the servlet context.
     *
     * @param config servlet config provided by the container
     * @throws ServletException if initialization fails
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        this.userService = Web.ctxBean(config.getServletContext(), WebConst.Ctx.USER_SERVICE, UserService.class);
        log.debug("UsersServlet initialized userService={}", userService.getClass().getSimpleName());
    }

    /**
     * Lists users or searches by id/login based on the {@code q} parameter.
     * <p>
     * Flow:
     * <ol>
     *   <li>Copy/pull flash messages ({@code OK}, {@code ERROR}).</li>
     *   <li>If {@code q} is absent → load all users via {@link UserService#findAll()}.</li>
     *   <li>If {@code q} is present → try {@link UserService#findById(String)}; on miss, try
     *       {@link UserService#findByLogin(String)} with {@code q.toLowerCase(Locale.ROOT)}.</li>
     *   <li>Attach the resulting list to {@code users} and forward to {@code WebConst.Jsp.USERS}.</li>
     * </ol>
     * </p>
     *
     * @param req  HTTP request (optional {@code q})
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
        String q = Web.trimOrNull(req.getParameter("q"));
        List<User> result;
        if (q == null) {
            result = userService.findAll();
            log.info("Users list requested: all users count={}", result.size());
        } else {
            Optional<User> byId = userService.findById(q);
            if (byId.isPresent()) {
                result = List.of(byId.get());
                log.info("Users search by id hit q={} userLogin={}", q, byId.get().getUserLogin());
            } else {
                Optional<User> byLogin = userService.findByLogin(q.toLowerCase(Locale.ROOT));
                result = byLogin.map(List::of).orElseGet(List::of);
                if (byLogin.isPresent()) {
                    log.info("Users search by login hit q={} userId={}", q, byLogin.get().getUserId());
                } else {
                    log.warn("Users search miss q={}", q);
                }
            }
        }
        req.setAttribute("users", result);
        Web.forward(req, resp, WebConst.Jsp.USERS);
    }
}
