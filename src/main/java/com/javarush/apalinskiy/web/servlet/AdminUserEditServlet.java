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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * Administrative servlet for editing user accounts.
 *
 * <p>This servlet allows administrators to view and update user details such as
 * name, login, role, and password. It also automatically updates the active
 * session if the admin edits their own account.</p>
 *
 * <p>When a user is modified, a {@link NotificationEvent} of type
 * {@link NotificationType#USER_ADMIN_CHANGED} is sent to the affected user
 * for audit and transparency.</p>
 */
public class AdminUserEditServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(AdminUserEditServlet.class);

    private UserService users;
    private NotificationService notify;

    /**
     * Initializes the servlet and retrieves the necessary beans from the servlet context.
     *
     * @param config servlet configuration provided by the container
     * @throws ServletException if one or more required beans are missing
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        ServletContext ctx = config.getServletContext();
        this.users = Web.ctxBean(ctx, WebConst.Ctx.USER_SERVICE, UserService.class);
        this.notify = Web.ctxBean(ctx, WebConst.Ctx.NOTIFY_SERVICE, NotificationService.class);
        log.debug("AdminUserEditServlet initialized");
    }

    /**
     * Displays the user edit form for a given user ID.
     *
     * <p>If the user is not found, redirects back to the user list page with an error message.</p>
     *
     * @param req  HTTP request containing the user ID parameter
     * @param resp HTTP response for forwarding or redirection
     * @throws ServletException if forwarding to the JSP fails
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Web.pullFlash(req, WebConst.Attr.OK);
        Web.pullFlash(req, WebConst.Attr.ERROR);
        String id = Web.trimOrNull(req.getParameter(WebConst.Param.ID));
        if (id == null) {
            log.warn("GET user edit: missing id");
            Web.redirectErr(req, resp, WebConst.Path.USERS, "Missing user id");
            return;
        }
        User target = users.findById(id).orElse(null);
        if (target == null) {
            log.warn("GET user edit: target not found id={}", id);
            Web.redirectErr(req, resp, WebConst.Path.USERS, "User not found");
            return;
        }
        req.setAttribute("editUser", target);
        req.setAttribute("roles", List.of(Role.USER, Role.ADMIN));
        log.info("GET user edit opened targetId={} login='{}'", target.getUserId(), target.getUserLogin());
        Web.forward(req, resp, WebConst.Jsp.USER_EDIT);
    }

    /**
     * Handles user updates submitted via POST requests.
     *
     * <p>Allows administrators to modify user roles, names, logins, and passwords.
     * If the administrator edits their own account, the active session is refreshed
     * to maintain consistency.</p>
     *
     * <p>Additionally, any significant changes trigger a user notification for transparency.</p>
     *
     * @param req  HTTP request containing updated user data
     * @param resp HTTP response for redirection
     * @throws IOException if an I/O or redirection error occurs
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String id = Web.trimOrNull(req.getParameter(WebConst.Param.ID));
        String name = Web.trimOrNull(req.getParameter(WebConst.Param.USER_NAME));
        String login = Web.trimOrNull(req.getParameter(WebConst.Param.USER_LOGIN));
        String roleStr = Web.trimOrNull(req.getParameter(WebConst.Param.ROLE));
        String newPwd = Web.trimOrNull(req.getParameter(WebConst.Param.PASSWORD));
        if (id == null) {
            log.warn("POST user edit: missing id");
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
                    log.debug("Session renewed after self-update userId={}", updated.getUserId());
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
            log.info("POST user edit: updated by={} targetId={} role={} login='{}' name='{}' pwdChanged={}",
                    (beforeOpt.isPresent() ? req.getSession(false) != null &&
                            (req.getSession(false).getAttribute(WebConst.Attr.USER)) != null
                            ? ((User) req.getSession(false).getAttribute(WebConst.Attr.USER)).getUserId() : null : null),
                    updated.getUserId(), updated.getRole(), updated.getUserLogin(), updated.getUserName(),
                    (newPwd != null && !newPwd.isBlank()));
            Web.redirectOk(req, resp,
                    WebConst.Path.USER_EDIT + "?" + WebConst.Param.ID + "=" + id,
                    "The user has been updated");
        } catch (DuplicateLoginException e) {
            log.warn("POST user edit: duplicate login id={} login='{}'", id, login);
            Web.redirectErr(req, resp,
                    WebConst.Path.USER_EDIT + "?" + WebConst.Param.ID + "=" + id,
                    "The login is already occupied");
        } catch (IllegalArgumentException | NoSuchElementException e) {
            log.warn("POST user edit: invalid input or user not found id={} msg={}", id, e.getMessage());
            Web.redirectErr(req, resp,
                    WebConst.Path.USER_EDIT + "?" + WebConst.Param.ID + "=" + id,
                    e.getMessage());
        }
    }
}
