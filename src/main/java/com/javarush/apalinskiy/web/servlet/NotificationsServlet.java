package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.repository.notify.NotificationRepository;
import com.javarush.apalinskiy.web.util.Web;
import com.javarush.apalinskiy.app.WebConst;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Renders and manages user notifications (list, mark-read, clear).
 * <p>
 * This servlet requires an authenticated {@link User} in the HTTP session
 * (checked via {@link #requireAuth(HttpServletRequest, HttpServletResponse)}).
 * Data access is delegated to {@link NotificationRepository}, resolved from the
 * servlet context in {@link #init()}.
 * </p>
 *
 * <h3>GET</h3>
 * <ul>
 *   <li>Reads flash messages ({@code OK}, {@code ERROR}).</li>
 *   <li>Query params:
 *     <ul>
 *       <li><b>limit</b> — optional page size (default: 50)</li>
 *       <li><b>offset</b> — optional page offset (default: 0)</li>
 *     </ul>
 *   </li>
 *   <li>Sets request attrs:
 *     <ul>
 *       <li>{@code items} — notification list for the current user</li>
 *       <li>{@code unread} — unread counter</li>
 *     </ul>
 *   </li>
 *   <li>Forwards to {@code WebConst.Jsp.NOTIFICATIONS}.</li>
 * </ul>
 *
 * <h3>POST actions</h3>
 * <ul>
 *   <li><b>markRead</b> — marks a single notification as read (param {@code id}).</li>
 *   <li><b>markAll</b> — marks all notifications as read.</li>
 *   <li><b>clearAll</b> — deletes all notifications.</li>
 *   <li>Unknown action → error flash.</li>
 * </ul>
 *
 * <p>Flash helpers: {@link Web#redirectOk}, {@link Web#redirectErr}. View helpers: {@link Web#forward}.</p>
 *
 * @see NotificationRepository
 * @see WebConst
 * @see Web
 */
public class NotificationsServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(NotificationsServlet.class);

    private NotificationRepository repo;

    /**
     * Resolves {@link NotificationRepository} from the servlet context.
     * <p>
     * Looks up the repository via {@code WebConst.Ctx.NOTIFY_REPO}. Assumes it is present
     * and throws if the bean cannot be found.
     * </p>
     */
    @Override
    public void init() {
        this.repo = Web.ctxBean(getServletContext(), WebConst.Ctx.NOTIFY_REPO, NotificationRepository.class);
        log.debug("NotificationsServlet initialized with NotificationRepository={}", repo.getClass().getSimpleName());
    }

    /**
     * Displays the notifications list for the authenticated user.
     * <p>
     * Flow:
     * <ol>
     *   <li>Pull flash messages ({@code OK}, {@code ERROR}).</li>
     *   <li>Require auth; if missing, {@link #requireAuth} redirects to login and this method returns.</li>
     *   <li>Parse pagination params {@code limit} (default 50) and {@code offset} (default 0).</li>
     *   <li>Query the repository and set {@code items} + {@code unread} attributes.</li>
     *   <li>Forward to {@code WebConst.Jsp.NOTIFICATIONS}.</li>
     * </ol>
     * </p>
     *
     * @param req  HTTP request (optional {@code limit}, {@code offset})
     * @param resp HTTP response
     * @throws ServletException if forwarding fails
     * @throws IOException      on I/O errors
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Web.pullFlash(req, WebConst.Attr.OK);
        Web.pullFlash(req, WebConst.Attr.ERROR);
        User me = requireAuth(req, resp);
        if (me == null) {
            return;
        }
        int limit = Web.parseIntOrDefault(req.getParameter("limit"), 50);
        int offset = Web.parseIntOrDefault(req.getParameter("offset"), 0);
        req.setAttribute("items", repo.list(me.getUserId(), limit, offset));
        req.setAttribute("unread", repo.unreadCount(me.getUserId()));
        log.debug("Notifications page userId={} limit={} offset={}", me.getUserId(), limit, offset);
        Web.forward(req, resp, WebConst.Jsp.NOTIFICATIONS);
    }

    /**
     * Executes mutations on the user's notifications.
     * <p>
     * Consumes {@code action} and optional auxiliary parameters:
     * <ul>
     *   <li><b>markRead</b>: {@code id} — notification id to mark as read;</li>
     *   <li><b>markAll</b>: no params;</li>
     *   <li><b>clearAll</b>: no params.</li>
     * </ul>
     * Redirects back to {@code /notifications} with an OK or ERROR flash.
     * </p>
     *
     * @param req  HTTP request containing {@code action} and optional parameters
     * @param resp HTTP response used for redirects
     * @throws IOException if a redirect fails
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User me = requireAuth(req, resp);
        if (me == null) {
            return;
        }
        String action = Web.trimOrNull(req.getParameter(WebConst.Param.ACTION));
        try {
            switch (action == null ? "" : action) {
                case "markRead" -> {
                    String id = Web.trimOrNull(req.getParameter(WebConst.Param.ID));
                    if (id != null) repo.markRead(me.getUserId(), id);
                    log.info("Notification marked as read userId={} notificationId={}", me.getUserId(), id);
                    Web.redirectOk(req, resp, WebConst.Path.NOTIFICATIONS, "Marked as read");
                }
                case "markAll" -> {
                    repo.markAllRead(me.getUserId());
                    log.info("All notifications marked as read userId={}", me.getUserId());
                    Web.redirectOk(req, resp, WebConst.Path.NOTIFICATIONS, "All notifications are marked as read");
                }
                case "clearAll" -> {
                    repo.clearAll(me.getUserId());
                    log.info("All notifications cleared userId={}", me.getUserId());
                    Web.redirectOk(req, resp, WebConst.Path.NOTIFICATIONS, "The list has been cleared");
                }
                default -> {
                    log.warn("Unknown action in notifications userId={} action={}", me.getUserId(), action);
                    Web.redirectErr(req, resp, WebConst.Path.NOTIFICATIONS, "Unknown action");
                }
            }
        } catch (Exception e) {
            log.error("Notifications POST error userId={} action={}", me.getUserId(), action, e);
            Web.redirectErr(req, resp, WebConst.Path.NOTIFICATIONS, "Error: " + e.getMessage());
        }
    }

    /**
     * Ensures there is an authenticated user in the session; otherwise redirects to login.
     * <p>
     * Looks for {@code WebConst.Attr.USER}. If absent, sends a redirect to
     * {@code WebConst.Path.LOGIN} with an error flash and returns {@code null}.
     * </p>
     *
     * @param req  HTTP request
     * @param resp HTTP response used for redirect
     * @return the authenticated {@link User} or {@code null} if unauthenticated
     * @throws IOException if redirect fails
     */
    private User requireAuth(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User u = (User) req.getSession(true).getAttribute(WebConst.Attr.USER);
        if (u == null) {
            log.warn("Unauthorized access to notifications, redirecting to login");
            Web.redirectErr(req, resp, WebConst.Path.LOGIN, "Please login");
        }
        return u;
    }
}
