package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.service.impl.user.DefaultUserService;
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
 * Servlet responsible for displaying and searching users.
 *
 * <p>Supports:
 * <ul>
 *   <li>Paginated listing of all users (default mode)</li>
 *   <li>Search by user ID or login (case-insensitive)</li>
 * </ul>
 * </p>
 *
 * <p>Expected query parameters:
 * <ul>
 *   <li>{@code q} — search term (optional)</li>
 *   <li>{@code page} — page number (optional, defaults to 1)</li>
 * </ul>
 * </p>
 */
public class UsersServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(UsersServlet.class);

    private UserService userService;

    /**
     * Initializes the servlet and resolves required services.
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        this.userService = Web.ctxBean(config.getServletContext(), WebConst.Ctx.USER_SERVICE, UserService.class);
        log.debug("UsersServlet initialized userService={}", userService.getClass().getSimpleName());
    }

    /**
     * Handles user listing or search requests.
     *
     * @param req  HTTP request (expects optional {@code q} and {@code page})
     * @param resp HTTP response
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Web.copyParamsToAttrs(req, WebConst.Attr.OK, WebConst.Attr.ERROR);
        Web.pullFlash(req, WebConst.Attr.OK);
        Web.pullFlash(req, WebConst.Attr.ERROR);
        String q = Web.trimOrNull(req.getParameter("q"));
        int page = 1;
        try {
            String p = req.getParameter("page");
            if (p != null) page = Math.max(1, Integer.parseInt(p));
        } catch (NumberFormatException ignored) {
        }
        final int size = 10;
        if (q == null) {
            DefaultUserService.PagedResult<User> pg = userService.findPage(page, size);
            req.setAttribute("users", pg.items());
            req.setAttribute("page", pg.page());
            req.setAttribute("size", pg.size());
            req.setAttribute("total", pg.total());
            req.setAttribute("pages", pg.totalPages());
            req.setAttribute("offset", pg.offset());
            log.info("Users page requested: page={} size={} total={} pages={}",
                    pg.page(), pg.size(), pg.total(), pg.totalPages());
        } else {
            List<User> result;
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
            req.setAttribute("users", result);
            req.setAttribute("page", 1);
            req.setAttribute("size", result.size());
            req.setAttribute("total", result.size());
            req.setAttribute("pages", 1);
            req.setAttribute("offset", 0);
            req.setAttribute("q", q);
        }
        Web.forward(req, resp, WebConst.Jsp.USERS);
    }
}
