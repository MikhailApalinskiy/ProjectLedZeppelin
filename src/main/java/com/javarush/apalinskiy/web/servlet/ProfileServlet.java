package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.service.user.UserService;
import com.javarush.apalinskiy.domain.user.UserStats;
import com.javarush.apalinskiy.service.user.UserStatsService;
import com.javarush.apalinskiy.web.util.Web;
import com.javarush.apalinskiy.app.WebConst;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ProfileServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(ProfileServlet.class);

    private UserService userService;
    private UserStatsService userStats;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        ServletContext ctx = config.getServletContext();
        this.userService = Web.ctxBean(ctx, WebConst.Ctx.USER_SERVICE, UserService.class);
        try {
            this.userStats = Web.ctxBean(ctx, WebConst.Ctx.USER_STATS_SERVICE, UserStatsService.class);
        } catch (IllegalStateException ignore) {
            this.userStats = null;
        }
        log.debug("ProfileServlet initialized with userService={} userStats={}",
                userService.getClass().getSimpleName(),
                (userStats != null ? userStats.getClass().getSimpleName() : "none"));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Web.copyParamsToAttrs(req, WebConst.Attr.OK, WebConst.Attr.ERROR);
        Web.pullFlash(req, WebConst.Attr.OK);
        Web.pullFlash(req, WebConst.Attr.ERROR);
        User user = (User) req.getSession().getAttribute(WebConst.Attr.USER);
        if (user != null) {
            req.setAttribute(WebConst.Attr.USER, user);
            if (userStats != null) {
                UserStats s = userStats.statsOf(user.getUserId());
                req.setAttribute("stats", s);
            }
            log.debug("Profile page requested userId={}", user.getUserId());
        } else {
            log.warn("Unauthenticated profile page access, redirecting to login");
        }
        Web.forward(req, resp, WebConst.Jsp.PROFILE);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        User user = (User) req.getSession().getAttribute(WebConst.Attr.USER);
        if (user == null) {
            log.warn("Profile update attempt without authentication");
            Web.redirect(req, resp, WebConst.Path.LOGIN, null);
            return;
        }
        String action = req.getParameter(WebConst.Param.ACTION);
        try {
            switch (action == null ? "" : action) {
                case "updateName" -> {
                    handleUpdateName(req, user);
                    log.info("User updated display name userId={}", user.getUserId());
                }
                case "changePassword" -> {
                    handleChangePassword(req, user);
                    log.info("User changed password userId={}", user.getUserId());
                }
                default -> {
                    log.warn("Unknown profile action userId={} action={}", user.getUserId(), action);
                    Web.redirectErr(req, resp, WebConst.Path.PROFILE, "Unknown action");
                    return;
                }
            }
            Web.redirectOk(req, resp, WebConst.Path.PROFILE, "Password changed successfully");
        } catch (SecurityException se) {
            log.warn("Password change failed (wrong current password) userId={}", user.getUserId());
            Web.redirectErr(req, resp, WebConst.Path.PROFILE, "The current password is incorrect");
        } catch (IllegalArgumentException iae) {
            log.warn("Profile update validation error userId={} msg={}", user.getUserId(), iae.getMessage());
            Web.redirectErr(req, resp, WebConst.Path.PROFILE, iae.getMessage());
        } catch (Exception e) {
            log.error("Unexpected profile update error userId={}", user.getUserId(), e);
            Web.redirectErr(req, resp, WebConst.Path.PROFILE, WebConst.Msg.INTERNAL_ERROR);
        }
    }

    private void handleUpdateName(HttpServletRequest req, User user) {
        String newName = Web.trimOrNull(req.getParameter("displayName"));
        if (newName == null) {
            newName = Web.trimOrNull(req.getParameter(WebConst.Param.USER_NAME));
        }
        if (newName == null) {
            throw new IllegalArgumentException("The name cannot be empty.");
        }
        User updated = userService.updateProfile(user.getUserId(), newName);
        req.getSession().setAttribute(WebConst.Attr.USER, updated);
    }

    private void handleChangePassword(HttpServletRequest req, User user) {
        String current = Web.trimOrNull(req.getParameter(WebConst.Param.CURRENT_PASSWORD));
        String newPwd = Web.trimOrNull(req.getParameter(WebConst.Param.NEW_PASSWORD));
        String confirm = Web.trimOrNull(req.getParameter(WebConst.Param.CONFIRM_PASSWORD));
        if (current == null || newPwd == null || confirm == null) {
            throw new IllegalArgumentException("Fill in all the fields to change the password.");
        }
        if (!newPwd.equals(confirm)) {
            throw new IllegalArgumentException("The new password and the confirmation don't match");
        }
        if (newPwd.length() < 6) {
            throw new IllegalArgumentException("The new password must be at least 6 characters long");
        }
        if (newPwd.equals(current)) {
            throw new IllegalArgumentException("The new password matches the current one.");
        }
        userService.changePassword(user.getUserId(), current, newPwd);
        User fresh = userService.findById(user.getUserId()).orElse(user);
        req.getSession().setAttribute(WebConst.Attr.USER, fresh);
    }
}
