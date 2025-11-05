package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.app.WebConst;
import com.javarush.apalinskiy.domain.user.User;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Servlet responsible for user logout and session termination.
 *
 * <p>Accepts only POST requests to ensure CSRF protection.
 * Invalidates the current session and redirects to the home page.</p>
 *
 * <p>Logs logout events including user ID and login name if available.</p>
 */
public class LogoutServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(LogoutServlet.class);

    /**
     * Rejects GET requests to avoid accidental or malicious logout triggers.
     *
     * <p>Returns {@code 405 Method Not Allowed}.</p>
     *
     * @param req  HTTP request
     * @param resp HTTP response
     * @throws IOException if sending error fails
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        log.warn("Logout attempted via GET uri={}", req.getRequestURI());
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    /**
     * Handles POST logout requests: invalidates session and redirects to home.
     *
     * <p>If the session contains a {@link User} attribute,
     * logs its ID and login name before invalidation.</p>
     *
     * @param req  HTTP request
     * @param resp HTTP response
     * @throws IOException if redirect fails
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession s = req.getSession(false);
        if (s != null) {
            Object userObj = s.getAttribute(WebConst.Attr.USER);
            s.invalidate();
            if (userObj instanceof User u) {
                log.info("User logged out userId={} login={}", u.getUserId(), u.getUserLogin());
            } else {
                log.info("Session invalidated without user attribute");
            }
        } else {
            log.debug("Logout called but no session present");
        }
        resp.sendRedirect(resp.encodeRedirectURL(req.getContextPath() + WebConst.Path.HOME));
    }
}
