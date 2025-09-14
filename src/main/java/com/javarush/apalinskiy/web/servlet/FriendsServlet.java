package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.domain.social.FriendRequest;
import com.javarush.apalinskiy.domain.user.User;
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
import java.util.List;
import java.util.Map;

/**
 * Servlet that renders and manages the user's friend graph:
 * current friends, incoming/outgoing requests, and mutation actions
 * (send/cancel/accept/decline requests; remove a friend).
 * <p>
 * Authentication is required for all endpoints and enforced via {@link #requireAuth(HttpServletRequest, HttpServletResponse)}.
 * The servlet relies on {@link FriendService} for mutations and (optionally) {@link NotificationService}
 * for fire-and-forget notifications.
 * </p>
 *
 * <h3>GET</h3>
 * <ul>
 *   <li>Pulls flash messages ({@code OK}, {@code ERROR}).</li>
 *   <li>Loads <i>friends</i>, <i>incoming</i>, and <i>outgoing</i> lists and exposes them to the JSP.</li>
 *   <li>Forwards to {@code WebConst.Jsp.FRIENDS}.</li>
 * </ul>
 *
 * <h3>POST actions</h3>
 * <ul>
 *   <li><b>request</b> &rarr; {@link FriendService#sendRequest(String, String)}</li>
 *   <li><b>accept</b>  &rarr; {@link FriendService#accept(String, String)}</li>
 *   <li><b>decline</b> &rarr; {@link FriendService#decline(String, String)}</li>
 *   <li><b>cancel</b>  &rarr; {@link FriendService#cancel(String, String)}</li>
 *   <li><b>remove</b>  &rarr; {@link FriendService#remove(String, String)} (+ optional {@link NotificationType#FRIEND_REMOVED})</li>
 * </ul>
 *
 * <p>Flash helpers: {@link Web#redirectOk}, {@link Web#redirectErr}. View helpers: {@link Web#forward}.</p>
 *
 * @see FriendService
 * @see NotificationService
 * @see NotificationType
 * @see WebConst
 * @see Web
 */
public class FriendsServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(FriendsServlet.class);

    private FriendService service;
    private NotificationService notify;

    /**
     * Resolves required {@link FriendService} and optional {@link NotificationService}
     * from the {@link ServletContext}.
     *
     * @param config servlet config supplied by the container
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
     * Renders the friends page for the authenticated user.
     * <p>
     * Behavior:
     * <ul>
     *   <li>Pulls flash messages ({@code OK}, {@code ERROR}).</li>
     *   <li>Requires auth; if unauthenticated, {@link #requireAuth(HttpServletRequest, HttpServletResponse)} handles redirect.</li>
     *   <li>Loads {@code friends}, {@code incoming}, and {@code outgoing} lists and exposes them as request attributes.</li>
     *   <li>Forwards to {@code WebConst.Jsp.FRIENDS}.</li>
     * </ul>
     * </p>
     *
     * @param req  HTTP request
     * @param resp HTTP response
     * @throws ServletException forwarding errors
     * @throws IOException      I/O errors during forward
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Web.pullFlash(req, WebConst.Attr.OK);
        Web.pullFlash(req, WebConst.Attr.ERROR);
        User me = requireAuth(req, resp);
        if (me == null) {
            return;
        }
        List<User> friends = service.listFriends(me.getUserId());
        List<FriendRequest> incoming = service.incoming(me.getUserId());
        List<FriendRequest> outgoing = service.outgoing(me.getUserId());
        log.info("Friends page opened userId={} friends={} incoming={} outgoing={}",
                me.getUserId(), friends.size(), incoming.size(), outgoing.size());
        req.setAttribute("friends", service.listFriends(me.getUserId()));
        req.setAttribute("incoming", service.incoming(me.getUserId()));
        req.setAttribute("outgoing", service.outgoing(me.getUserId()));
        Web.forward(req, resp, WebConst.Jsp.FRIENDS);
    }

    /**
     * Executes friend-related mutations for the authenticated user.
     * <p>
     * Consumes {@code action} and auxiliary parameters:
     * <ul>
     *   <li><b>request</b>: {@code id} (recipient user id)</li>
     *   <li><b>accept</b>: {@code fromId} (original sender id)</li>
     *   <li><b>decline</b>: {@code fromId} (original sender id)</li>
     *   <li><b>cancel</b>: {@code id} (recipient user id)</li>
     *   <li><b>remove</b>: {@code id} (friend id)</li>
     * </ul>
     * On success, redirects with an OK flash; on validation/unknown action, redirects with an error flash.
     * </p>
     *
     * @param req  HTTP request containing {@code action} and related parameters
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
     * Ensures the user is authenticated before proceeding.
     * <p>
     * Looks up {@code WebConst.Attr.USER} in the session (creating one if missing)
     * and, if absent, redirects to {@code WebConst.Path.LOGIN} with an error flash.
     * Returns {@code null} in that case so the caller can stop the flow.
     * </p>
     *
     * @param req  HTTP request
     * @param resp HTTP response used for redirects
     * @return the authenticated {@link User} or {@code null} if redirected to login
     * @throws IOException if the redirect fails
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
