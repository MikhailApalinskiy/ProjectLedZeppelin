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
 * Admin-only servlet for viewing and editing user accounts.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Resolve {@link UserService} and {@link NotificationService} from the servlet context.</li>
 *   <li>Render edit form for a target user (GET).</li>
 *   <li>Apply admin updates to a user (POST), optionally renewing the admin's own session
 *       if they edited their own sensitive data (login/name/password/role).</li>
 *   <li>Send a {@link NotificationType#USER_ADMIN_CHANGED} notification to the edited user
 *       when visible changes occur.</li>
 * </ul>
 * Authentication/authorization should be enforced upstream (e.g., an admin filter).
 * </p>
 *
 * <h3>GET</h3>
 * <p>Parameters:</p>
 * <ul>
 *   <li><b>id</b> — required, target user id.</li>
 * </ul>
 * <p>Behavior:</p>
 * <ul>
 *   <li>Pulls flash messages ({@code OK}, {@code ERROR}).</li>
 *   <li>Fetches target user; redirects to {@code /users} with error flash if missing.</li>
 *   <li>Sets request attributes:
 *     <ul>
 *       <li>{@code editUser} — target {@link User};</li>
 *       <li>{@code roles} — fixed list of roles ({@link Role#USER}, {@link Role#ADMIN}).</li>
 *     </ul>
 *   </li>
 *   <li>Forwards to {@code WebConst.Jsp.USER_EDIT}.</li>
 * </ul>
 *
 * <h3>POST</h3>
 * <p>Parameters:</p>
 * <ul>
 *   <li><b>id</b> — required, target user id;</li>
 *   <li><b>userName</b>, <b>userLogin</b> — optional new name/login;</li>
 *   <li><b>role</b> — {@code USER} or {@code ADMIN} (case-insensitive; defaults to {@code USER});</li>
 *   <li><b>password</b> — optional new password (non-blank indicates change).</li>
 * </ul>
 * <p>Behavior:</p>
 * <ul>
 *   <li>Validates presence of {@code id}; redirects with error if missing.</li>
 *   <li>Reads the "before" snapshot (if present), performs {@link UserService#adminUpdate}.</li>
 *   <li>If the editor edits themselves and sensitive data changed, renews the session via
 *       {@link Web#renewSessionAndPut} to refresh authentication state; otherwise updates session user attribute.</li>
 *   <li>Builds human-readable summary and machine-readable diff via {@link Web#buildAdminChangeSummary}
 *       and {@link Web#buildMachineReadableDiff}. Sends {@code USER_ADMIN_CHANGED} notification if there are visible changes.</li>
 *   <li>On success, redirects back to the edit page with OK flash. On conflicts or invalid input,
 *       redirects with ERROR flash.</li>
 * </ul>
 *
 * <h3>Errors</h3>
 * <ul>
 *   <li>{@link DuplicateLoginException} → "The username is already occupied".</li>
 *   <li>{@link IllegalArgumentException}/{@link NoSuchElementException} → message propagated to error flash.</li>
 * </ul>
 *
 * <h3>Notes</h3>
 * <ul>
 *   <li>Logging includes actor id (if available) and target properties.</li>
 *   <li>Flash helpers: {@link Web#redirectOk}, {@link Web#redirectErr}.</li>
 * </ul>
 */
public class AdminUserEditServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(AdminUserEditServlet.class);

    private UserService users;
    private NotificationService notify;

    /**
     * Resolves required services from the servlet context.
     * <p>Expected context attributes: {@code USER_SERVICE}, {@code NOTIFY_SERVICE}.</p>
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
     * Renders the admin user edit page.
     * <p>
     * Required param: {@code id}. Redirects to {@code /users} with an error flash if missing or not found.
     * Exposes {@code editUser} and {@code roles} to the JSP, then forwards to {@code WebConst.Jsp.USER_EDIT}.
     * </p>
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
     * Applies admin-side updates to the target user and handles session/notifications.
     * <p>
     * Parameters: {@code id} (required), {@code userName}, {@code userLogin}, {@code role}, {@code password}.
     * On success, redirects back to the edit page with an OK flash; otherwise redirects with error.
     * </p>
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
                    "The username is already occupied");
        } catch (IllegalArgumentException | NoSuchElementException e) {
            log.warn("POST user edit: invalid input or user not found id={} msg={}", id, e.getMessage());
            Web.redirectErr(req, resp,
                    WebConst.Path.USER_EDIT + "?" + WebConst.Param.ID + "=" + id,
                    e.getMessage());
        }
    }
}
