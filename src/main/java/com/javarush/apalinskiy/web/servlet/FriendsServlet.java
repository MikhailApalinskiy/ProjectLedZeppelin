package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.service.impl.user.DefaultUserService;
import com.javarush.apalinskiy.service.social.FriendService;
import com.javarush.apalinskiy.domain.notify.NotificationEvent;
import com.javarush.apalinskiy.service.notify.NotificationService;
import com.javarush.apalinskiy.domain.notify.NotificationType;
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
import java.util.Map;

/**
 * Servlet that manages friend relationships and requests.
 *
 * <p><b>GET</b> — renders the Friends page with:</p>
 * <ul>
 *   <li>Paginated list of current friends (with optional search query)</li>
 *   <li>Incoming friend requests</li>
 *   <li>Outgoing friend requests</li>
 * </ul>
 *
 * <p><b>POST</b> — performs actions specified by {@code action} parameter:</p>
 * <ul>
 *   <li>{@code request} — send a friend request to a user</li>
 *   <li>{@code accept} — accept a pending request</li>
 *   <li>{@code decline} — decline a pending request</li>
 *   <li>{@code cancel} — cancel an outgoing request</li>
 *   <li>{@code remove} — remove an existing friend</li>
 * </ul>
 *
 * <p>All actions require authentication. Results are communicated via
 * flash messages and redirects to keep endpoints idempotent from the UI perspective.</p>
 */
public class FriendsServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(FriendsServlet.class);

    private FriendService service;
    private NotificationService notify;

    /**
     * Resolves required services from the application context.
     *
     * @param config servlet configuration
     * @throws ServletException if initialization fails
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        ServletContext ctx = config.getServletContext();
        this.service = Web.ctxBean(ctx, WebConst.Ctx.FRIEND_SERVICE, FriendService.class);
        try {
            this.notify = Web.ctxBean(ctx, WebConst.Ctx.NOTIFY_SERVICE, NotificationService.class);
        } catch (IllegalStateException ignore) {
            this.notify = null;
        }
        log.debug("FriendsServlet initialized notifyPresent={}", notify != null);
    }

    /**
     * Renders the Friends page with friends list and pending requests.
     *
     * <p>Query parameters:</p>
     * <ul>
     *   <li>{@code q} — optional search query by name/login</li>
     *   <li>{@code page} — 1-based page index (defaults to 1)</li>
     *   <li>{@code size} — page size (defaults to 10, clamped to [1..100])</li>
     * </ul>
     *
     * @param req  HTTP request
     * @param resp HTTP response
     * @throws ServletException on forwarding errors
     * @throws IOException      on I/O errors
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.trace("FriendsServlet.doGet: start uri={} query={}", req.getRequestURI(), req.getQueryString());
        Web.pullFlash(req, WebConst.Attr.OK);
        Web.pullFlash(req, WebConst.Attr.ERROR);
        User me = requireAuth(req, resp);
        if (me == null) {
            log.warn("FriendsServlet.doGet: unauthorized access, redirected to login");
            return;
        }
        String q = Web.trimOrNull(req.getParameter("q"));
        int page = Web.parseIntOrDefault(req.getParameter("page"), 1);
        int size = Web.parseIntOrDefault(req.getParameter("size"), 10);
        if (page < 1) page = 1;
        if (size < 1) size = 12;
        if (size > 100) size = 100;
        log.debug("FriendsServlet.doGet: userId={} page={} size={} q='{}'", me.getUserId(), page, size, q);
        DefaultUserService.PagedResult<User> paged = service.listFriendsPaged(me.getUserId(), page, size, q);
        log.trace("FriendsServlet.doGet: found {} friends (total={})", paged.items().size(), paged.total());
        req.setAttribute("friends", paged.items());
        req.setAttribute("total", paged.total());
        req.setAttribute("page", paged.page());
        req.setAttribute("size", paged.size());
        req.setAttribute("pages", paged.totalPages());
        req.setAttribute("q", q);
        req.setAttribute("incoming", service.incoming(me.getUserId()));
        req.setAttribute("outgoing", service.outgoing(me.getUserId()));
        log.trace("FriendsServlet.doGet: loaded incoming/outgoing requests for userId={}", me.getUserId());
        req.setAttribute("selfUrl", req.getContextPath() + WebConst.Path.FRIENDS);
        log.debug("FriendsServlet.doGet: forwarding to JSP={} userId={}", WebConst.Jsp.FRIENDS, me.getUserId());
        Web.forward(req, resp, WebConst.Jsp.FRIENDS);
    }

    /**
     * Performs friend-related actions based on the {@code action} parameter.
     *
     * <p>Expected parameters per action:</p>
     * <ul>
     *   <li><b>request</b>: {@code id} (target user id)</li>
     *   <li><b>accept</b>: {@code fromId} (request sender id)</li>
     *   <li><b>decline</b>: {@code fromId} (request sender id)</li>
     *   <li><b>cancel</b>: {@code id} (target user id)</li>
     *   <li><b>remove</b>: {@code id} (friend id)</li>
     * </ul>
     *
     * <p>On success or failure, sets a flash message and redirects to {@code /friends}.</p>
     *
     * @param req  HTTP request
     * @param resp HTTP response
     * @throws IOException on redirect errors
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
                case "request" -> {
                    String toId = Web.trimOrNull(req.getParameter(WebConst.Param.ID));
                    if (toId == null) {
                        log.warn("friend request: missing recipient userId={}", me.getUserId());
                        Web.redirectErr(req, resp, WebConst.Path.FRIENDS, "The recipient is not specified");
                        return;
                    }
                    service.sendRequest(me.getUserId(), toId);
                    log.info("friend request sent from={} to={}", me.getUserId(), toId);
                    Web.redirectOk(req, resp, WebConst.Path.FRIENDS, "The application has been sent");
                }
                case "accept" -> {
                    String fromId = Web.trimOrNull(req.getParameter(WebConst.Param.FROM_ID));
                    if (fromId == null) {
                        log.warn("friend accept: missing sender userId={}", me.getUserId());
                        Web.redirectErr(req, resp, WebConst.Path.FRIENDS, "The sender is not specified");
                        return;
                    }
                    service.accept(me.getUserId(), fromId);
                    log.info("friend request accepted to={} from={}", me.getUserId(), fromId);
                    Web.redirectOk(req, resp, WebConst.Path.FRIENDS, "The application has been accepted");
                }
                case "decline" -> {
                    String fromId = Web.trimOrNull(req.getParameter(WebConst.Param.FROM_ID));
                    if (fromId == null) {
                        log.warn("friend decline: missing sender userId={}", me.getUserId());
                        Web.redirectErr(req, resp, WebConst.Path.FRIENDS, "The sender is not specified");
                        return;
                    }
                    service.decline(me.getUserId(), fromId);
                    log.info("friend request declined to={} from={}", me.getUserId(), fromId);
                    Web.redirectOk(req, resp, WebConst.Path.FRIENDS, "The application was rejected");
                }
                case "cancel" -> {
                    String toId = Web.trimOrNull(req.getParameter(WebConst.Param.ID));
                    if (toId == null) {
                        log.warn("friend cancel: missing recipient userId={}", me.getUserId());
                        Web.redirectErr(req, resp, WebConst.Path.FRIENDS, "The recipient is not specified");
                        return;
                    }
                    service.cancel(me.getUserId(), toId);
                    log.info("friend request cancelled from={} to={}", me.getUserId(), toId);
                    Web.redirectOk(req, resp, WebConst.Path.FRIENDS, "The application has been cancelled");
                }
                case "remove" -> {
                    String friendId = Web.trimOrNull(req.getParameter(WebConst.Param.ID));
                    if (friendId == null) {
                        log.warn("friend remove: missing friendId userId={}", me.getUserId());
                        Web.redirectErr(req, resp, WebConst.Path.FRIENDS, "Friend not specified");
                        return;
                    }
                    service.remove(me.getUserId(), friendId);
                    try {
                        if (notify != null) {
                            notify.notify(NotificationEvent.of(
                                    NotificationType.FRIEND_REMOVED,
                                    me.getUserId(),
                                    friendId,
                                    Map.of()
                            ));
                        }
                    } catch (Exception ignore) {
                        log.debug("notify FRIEND_REMOVED failed silently userId={} friendId={}", me.getUserId(), friendId);
                    }
                    log.info("friend removed userId={} friendId={}", me.getUserId(), friendId);
                    Web.redirectOk(req, resp, WebConst.Path.FRIENDS, "The user has been removed from friends");
                }
                default -> {
                    log.warn("friends POST unknown action userId={} action='{}'", me.getUserId(), action);
                    Web.redirectErr(req, resp, WebConst.Path.FRIENDS, "Unknown action");
                }
            }
        } catch (Exception e) {
            log.error("friends POST failed userId={} action='{}': {}", me.getUserId(), action, e.getMessage(), e);
            Web.redirectErr(req, resp, WebConst.Path.FRIENDS, e.getMessage());
        }
    }

    /**
     * Ensures that the current request is authenticated; otherwise redirects to login.
     *
     * @param req  HTTP request
     * @param resp HTTP response
     * @return authenticated {@link User} or {@code null} if redirected to login
     * @throws IOException if redirect fails
     */
    private User requireAuth(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User u = (User) req.getSession(true).getAttribute(WebConst.Attr.USER);
        if (u == null) {
            log.info("friends access requires auth -> redirect to login");
            Web.redirectErr(req, resp, WebConst.Path.LOGIN, "Please login");
        }
        return u;
    }
}
