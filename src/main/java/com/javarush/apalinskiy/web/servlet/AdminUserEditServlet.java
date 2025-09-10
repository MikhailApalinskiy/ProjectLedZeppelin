package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.domain.user.Role;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.exceptions.DuplicateLoginException;
import com.javarush.apalinskiy.domain.notify.NotificationEvent;
import com.javarush.apalinskiy.service.notify.NotificationService;
import com.javarush.apalinskiy.domain.notify.NotificationType;
import com.javarush.apalinskiy.service.user.UserService;
import com.javarush.apalinskiy.web.util.Web;
import com.javarush.apalinskiy.app.WebConst;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.*;

public class AdminUserEditServlet extends HttpServlet {

    private UserService users;
    private NotificationService notify;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        ServletContext ctx = config.getServletContext();
        this.users = Web.ctxBean(ctx, WebConst.Ctx.USER_SERVICE, UserService.class);
        this.notify = Web.ctxBean(ctx, WebConst.Ctx.NOTIFY_SERVICE, NotificationService.class);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Web.pullFlash(req, WebConst.Attr.OK);
        Web.pullFlash(req, WebConst.Attr.ERROR);
        String id = Web.trimOrNull(req.getParameter(WebConst.Param.ID));
        if (id == null) {
            Web.redirectErr(req, resp, WebConst.Path.USERS, "Missing user id");
            return;
        }
        User target = users.findById(id).orElse(null);
        if (target == null) {
            Web.redirectErr(req, resp, WebConst.Path.USERS, "User not found");
            return;
        }
        req.setAttribute("editUser", target);
        req.setAttribute("roles", List.of(Role.USER, Role.ADMIN));
        Web.forward(req, resp, WebConst.Jsp.USER_EDIT);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String id = Web.trimOrNull(req.getParameter(WebConst.Param.ID));
        String name = Web.trimOrNull(req.getParameter(WebConst.Param.USER_NAME));
        String login = Web.trimOrNull(req.getParameter(WebConst.Param.USER_LOGIN));
        String roleStr = Web.trimOrNull(req.getParameter(WebConst.Param.ROLE));
        String newPwd = Web.trimOrNull(req.getParameter(WebConst.Param.PASSWORD));
        if (id == null) {
            Web.redirectErr(req, resp, WebConst.Path.USERS, "Missing user id");
            return;
        }
        Role role = "ADMIN".equalsIgnoreCase(roleStr) ? Role.ADMIN : Role.USER;
        Optional<User> beforeOpt = users.findById(id);
        try {
            User updated = users.adminUpdate(id, role, name, login, newPwd);
            HttpSession s = req.getSession(false);
            User me = (s == null) ? null : (User) s.getAttribute(WebConst.Attr.USER);
            if (s != null && me != null && me.getUserId().equals(updated.getUserId())) {
                boolean sensitiveChanged = Web.sensitiveChanged(beforeOpt.orElse(null), updated, newPwd != null);
                if (sensitiveChanged) {
                    Web.renewSessionAndPut(req, WebConst.Attr.USER, updated);
                } else {
                    s.setAttribute(WebConst.Attr.USER, updated);
                }
            }
            User before = beforeOpt.orElse(null);
            String what = Web.buildAdminChangeSummary(before, updated, newPwd != null);
            Map<String, String> data = Web.buildMachineReadableDiff(before, updated, newPwd != null);
            if (!"No visible changes".equals(what)) {
                notify.notify(NotificationEvent.of(
                        NotificationType.USER_ADMIN_CHANGED,
                        me == null ? null : me.getUserId(),
                        updated.getUserId(),
                        data
                ));
            }
            Web.redirectOk(req, resp,
                    WebConst.Path.USER_EDIT + "?" + WebConst.Param.ID + "=" + id,
                    "The user has been updated");
        } catch (DuplicateLoginException e) {
            Web.redirectErr(req, resp,
                    WebConst.Path.USER_EDIT + "?" + WebConst.Param.ID + "=" + id,
                    "The username is already occupied");
        } catch (IllegalArgumentException | NoSuchElementException e) {
            Web.redirectErr(req, resp,
                    WebConst.Path.USER_EDIT + "?" + WebConst.Param.ID + "=" + id,
                    e.getMessage());
        }
    }
}
