package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.service.UserService;
import com.javarush.apalinskiy.web.util.Web;
import com.javarush.apalinskiy.web.util.WebConst;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class ProfileServlet extends HttpServlet {

    private UserService userService;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        this.userService = Web.ctxBean(config.getServletContext(), WebConst.Ctx.USER_SERVICE, UserService.class);
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
        }
        Web.forward(req, resp, WebConst.Jsp.PROFILE);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        User user = (User) req.getSession().getAttribute(WebConst.Attr.USER);
        if (user == null) {
            Web.redirect(req, resp, WebConst.Path.LOGIN, null);
            return;
        }
        String action = req.getParameter(WebConst.Param.ACTION);
        try {
            switch (action == null ? "" : action) {
                case "updateName" -> handleUpdateName(req, user);
                case "changePassword" -> handleChangePassword(req, user);
                default -> {
                    Web.redirectErr(req, resp, WebConst.Path.PROFILE, "Unknown action");
                    return;
                }
            }
            Web.redirectOk(req, resp, WebConst.Path.PROFILE, "Password changed successfully");
        } catch (SecurityException se) {
            Web.redirectErr(req, resp, WebConst.Path.PROFILE, "The current password is incorrect");
        } catch (IllegalArgumentException iae) {
            Web.redirectErr(req, resp, WebConst.Path.PROFILE, iae.getMessage());
        } catch (Exception e) {
            Web.redirectErr(req, resp, WebConst.Path.PROFILE, WebConst.Msg.INTERNAL_ERROR);
        }
    }

    private void handleUpdateName(HttpServletRequest req, User user) {
        String newName = Web.trimOrNull(req.getParameter("displayName"));
        if (newName == null) {
            newName = Web.trimOrNull(req.getParameter(WebConst.Param.USER_NAME)); // "userName"
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
