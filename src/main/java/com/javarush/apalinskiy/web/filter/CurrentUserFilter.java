package com.javarush.apalinskiy.web.filter;

import com.javarush.apalinskiy.utils.CurrentUserContext;
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
 * Servlet filter responsible for binding the currently authenticated user to the request context.
 *
 * <p>This filter retrieves the {@link User} object (or its ID) from the HTTP session,
 * refreshes it using {@link UserService}, and attaches the latest version to both the
 * request and session scopes. It also updates the {@link CurrentUserContext} thread-local
 * for consistent access to the user ID during request processing.</p>
 *
 * <p>At the end of the request, the filter clears the user context to avoid leaks across threads.</p>
 */
public class CurrentUserFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(CurrentUserFilter.class);

    private UserService userService;

    /**
     * Initializes the filter and retrieves the {@link UserService} bean from the servlet context.
     *
     * @param cfg filter configuration provided by the servlet container
     * @throws UnavailableException if the {@link UserService} is not found in the context
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
     * Resolves the current user for each request and populates both the servlet attributes
     * and the thread-local {@link CurrentUserContext}.
     *
     * <p>If the session contains a {@link User} or a string user ID, it attempts to fetch the
     * latest user object from the database. This ensures that the request always operates
     * with an up-to-date user representation.</p>
     *
     * @param request incoming servlet request
     * @param response outgoing servlet response
     * @param chain filter chain for forwarding the request
     * @throws IOException if an I/O error occurs
     * @throws ServletException if request processing fails
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpSession session = req.getSession(false);
        String userIdForContext = null;
        log.trace("CurrentUserFilter: path={} sessionPresent={}",
                req.getRequestURI(), (session != null));
        if (session != null) {
            Object val = session.getAttribute(WebConst.Attr.USER);
            if (val instanceof User su) {
                log.debug("CurrentUserFilter: session has User object id={}", su.getUserId());
                userService.findById(su.getUserId()).ifPresent(fresh -> {
                    req.setAttribute(WebConst.Attr.USER, fresh);
                    session.setAttribute(WebConst.Attr.USER, fresh);
                    log.trace("CurrentUserFilter: refreshed User in request & session id={}", fresh.getUserId());
                });
                userIdForContext = su.getUserId();
            } else if (val instanceof String uid) {
                log.debug("CurrentUserFilter: session has userId string id={}", uid);
                userService.findById(uid).ifPresent(fresh -> {
                    req.setAttribute(WebConst.Attr.USER, fresh);
                    log.trace("CurrentUserFilter: attached fresh User to request id={}", fresh.getUserId());
                });
                userIdForContext = uid;
            }
        }
        if (userIdForContext != null && !userIdForContext.isBlank()) {
            CurrentUserContext.set(userIdForContext);
            log.trace("CurrentUserFilter: CurrentUserContext set id={}", userIdForContext);
        }
        try {
            chain.doFilter(request, response);
        } finally {
            CurrentUserContext.clear();
            log.trace("CurrentUserFilter: CurrentUserContext cleared");
        }
    }
}
