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
 * Handles user sign-out by invalidating the current HTTP session.
 * <p>
 * This endpoint only accepts <b>POST</b> requests. A successful logout clears the session
 * (including the {@code WebConst.Attr.USER} attribute, if present) and redirects to the home path.
 * Attempts to call it via <b>GET</b> are rejected with HTTP 405.
 * </p>
 *
 * <h3>Security notes</h3>
 * <ul>
 *   <li>Logout should be protected against CSRF by an upstream filter or framework mechanism.</li>
 *   <li>Session invalidation prevents reuse of the same session id after logout.</li>
 * </ul>
 *
 * @see WebConst
 */
public class LogoutServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(LogoutServlet.class);

    /**
     * Rejects logout attempts via GET with HTTP 405 (Method Not Allowed).
     *
     * @param req  incoming request
     * @param resp response used to send the error
     * @throws IOException if sending the error fails
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        log.warn("Logout attempted via GET uri={}", req.getRequestURI());
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    /**
     * Performs logout by invalidating the current session (if present) and redirecting to {@code WebConst.Path.HOME}.
     * <p>
     * Behavior:
     * <ul>
     *   <li>If a session exists, logs the user (if available), then calls {@link HttpSession#invalidate()}.</li>
     *   <li>Regardless of prior session state, redirects to the application home path.</li>
     * </ul>
     * </p>
     *
     * @param req  incoming request
     * @param resp response used for the redirect
     * @throws IOException if the redirect fails
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
