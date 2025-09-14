package com.javarush.apalinskiy.web.filter;

import com.javarush.apalinskiy.app.WebConst;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.service.user.UserService;
import com.javarush.apalinskiy.web.util.Web;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Filter that refreshes the current user in request/session scope on each request.
 * <p>
 * If the session contains {@code WebConst.Attr.USER} as a {@link User} or as a user id {@link String},
 * the filter loads a fresh {@link User} via {@link UserService} and:
 * <ul>
 *   <li>puts it into the request attributes under {@code WebConst.Attr.USER};</li>
 *   <li>and, when the original value was a {@link User}, also updates the session attribute.</li>
 * </ul>
 * Then the request is passed down the chain.
 */
public class CurrentUserFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(CurrentUserFilter.class);

    private UserService userService;

    /**
     * Resolves {@link UserService} from the {@link jakarta.servlet.ServletContext} using {@link Web#ctxBean}.
     *
     * @param cfg filter config provided by the container
     */
    @Override
    public void init(FilterConfig cfg) throws UnavailableException {
        try {
            this.userService = Web.ctxBean(cfg.getServletContext(),
                    WebConst.Ctx.USER_SERVICE, UserService.class);
            log.debug("CurrentUserFilter init: userService={}",
                    userService == null ? "null" : userService.getClass().getSimpleName());
        } catch (IllegalStateException e) {
            log.error("CurrentUserFilter init failed: UserService missing", e);
            throw new UnavailableException("UserService not found in ServletContext");
        }
    }

    /**
     * Refreshes the current user (if present in session) and continues the chain.
     * <p>
     * Supports two session formats for {@code WebConst.Attr.USER}: a {@link User} object or a {@link String} user id.
     * The fresh user is attached to the request; when the session held a {@link User}, it is updated as well.
     * </p>
     *
     * @param request  incoming request
     * @param response response
     * @param chain    next filter in the chain
     * @throws IOException      if an I/O error occurs during filtering
     * @throws ServletException if the next filter/servlet throws it
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpSession session = req.getSession(false);
        if (session != null) {
            Object val = session.getAttribute(WebConst.Attr.USER);
            if (val instanceof User su) {
                userService.findById(su.getUserId()).ifPresent(fresh -> {
                    req.setAttribute(WebConst.Attr.USER, fresh);
                    session.setAttribute(WebConst.Attr.USER, fresh);
                });
            } else if (val instanceof String uid) {
                userService.findById(uid).ifPresent(fresh ->
                        req.setAttribute(WebConst.Attr.USER, fresh));
            }
        }
        chain.doFilter(request, response);
    }
}
