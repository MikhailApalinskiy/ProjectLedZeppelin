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

public class LogoutServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(LogoutServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        log.warn("Logout attempted via GET uri={}", req.getRequestURI());
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

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
