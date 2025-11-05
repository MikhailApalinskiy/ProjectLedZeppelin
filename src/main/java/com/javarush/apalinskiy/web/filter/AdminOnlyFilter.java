package com.javarush.apalinskiy.web.filter;

import com.javarush.apalinskiy.domain.user.Role;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.web.util.Web;
import com.javarush.apalinskiy.app.WebConst;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Servlet filter that restricts access to administrative resources.
 *
 * <p>This filter ensures that only authenticated users with the {@link Role#ADMIN}
 * role can access the protected endpoints. All unauthorized or anonymous requests
 * are redirected to the home page with an error message.</p>
 *
 * <p>Typical usage: register this filter in {@code web.xml} or via annotation to
 * protect administrative sections of the application such as moderation panels
 * or system management pages.</p>
 */
public class AdminOnlyFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(AdminOnlyFilter.class);

    /**
     * Intercepts incoming requests and validates that the current user has administrative privileges.
     *
     * <p>If the session does not contain a user or the user’s role is not ADMIN,
     * the request is denied and the client is redirected to the home page with an
     * appropriate error message.</p>
     *
     * @param req incoming servlet request
     * @param resp outgoing servlet response
     * @param chain filter chain for passing control to the next filter or servlet
     * @throws IOException if an I/O error occurs during processing
     * @throws ServletException if the request cannot be processed further
     */
    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest r = (HttpServletRequest) req;
        HttpServletResponse w = (HttpServletResponse) resp;
        User me = (User) r.getSession().getAttribute(WebConst.Attr.USER);
        String path = r.getRequestURI();
        if (me == null) {
            log.warn("Access denied: anonymous user tried to access {}", path);
            Web.redirectErr(r, w, WebConst.Path.HOME, "Access denied");
            return;
        }
        if (me.getRole() != Role.ADMIN) {
            log.warn("Access denied: user id={} login='{}' role={} tried to access {}",
                    me.getUserId(), me.getUserLogin(), me.getRole(), path);
            Web.redirectErr(r, w, WebConst.Path.HOME, "Access denied");
            return;
        }
        log.info("Admin access granted id={} login='{}' path={}", me.getUserId(), me.getUserLogin(), path);
        chain.doFilter(req, resp);
    }
}
