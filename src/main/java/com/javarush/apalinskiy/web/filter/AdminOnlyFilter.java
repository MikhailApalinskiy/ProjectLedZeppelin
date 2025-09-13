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
 * Servlet filter that restricts access to admin-only resources.
 * <p>
 * This filter checks the authenticated user stored in the HTTP session and
 * ensures they have the {@link Role#ADMIN} role. If the user is missing
 * or not an admin, they are redirected to the home page with an
 * "Access denied" error message.
 * </p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Block anonymous users from accessing admin-only endpoints.</li>
 *   <li>Block authenticated non-admin users from accessing admin-only endpoints.</li>
 *   <li>Allow requests to proceed if the user is an admin.</li>
 * </ul>
 *
 * <h3>Logging</h3>
 * <ul>
 *   <li>Logs warnings when access is denied (anonymous or non-admin).</li>
 *   <li>Logs info messages when admin access is granted.</li>
 * </ul>
 */
public class AdminOnlyFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(AdminOnlyFilter.class);

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
