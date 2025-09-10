package com.javarush.apalinskiy.web.servlet;

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

import java.io.IOException;

public class FriendsServlet extends HttpServlet {

    private FriendService service;
    private NotificationService notify;

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
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Web.pullFlash(req, WebConst.Attr.OK);
        Web.pullFlash(req, WebConst.Attr.ERROR);
        User me = requireAuth(req, resp);
        if (me == null) {
            return;
        }
        req.setAttribute("friends", service.listFriends(me.getUserId()));
        req.setAttribute("incoming", service.incoming(me.getUserId()));
        req.setAttribute("outgoing", service.outgoing(me.getUserId()));
        Web.forward(req, resp, WebConst.Jsp.FRIENDS);
    }

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
                        Web.redirectErr(req, resp, WebConst.Path.FRIENDS, "The recipient is not specified");
                        return;
                    }
                    service.sendRequest(me.getUserId(), toId);
                    Web.redirectOk(req, resp, WebConst.Path.FRIENDS, "The application has been sent");
                }
                case "accept" -> {
                    String fromId = Web.trimOrNull(req.getParameter(WebConst.Param.FROM_ID));
                    if (fromId == null) {
                        Web.redirectErr(req, resp, WebConst.Path.FRIENDS, "The sender is not specified");
                        return;
                    }
                    service.accept(me.getUserId(), fromId);
                    Web.redirectOk(req, resp, WebConst.Path.FRIENDS, "The application has been accepted");
                }
                case "decline" -> {
                    String fromId = Web.trimOrNull(req.getParameter(WebConst.Param.FROM_ID));
                    if (fromId == null) {
                        Web.redirectErr(req, resp, WebConst.Path.FRIENDS, "The sender is not specified");
                        return;
                    }
                    service.decline(me.getUserId(), fromId);
                    Web.redirectOk(req, resp, WebConst.Path.FRIENDS, "The application was rejected");
                }
                case "cancel" -> {
                    String toId = Web.trimOrNull(req.getParameter(WebConst.Param.ID));
                    if (toId == null) {
                        Web.redirectErr(req, resp, WebConst.Path.FRIENDS, "The recipient is not specified");
                        return;
                    }
                    service.cancel(me.getUserId(), toId);
                    Web.redirectOk(req, resp, WebConst.Path.FRIENDS, "The application has been cancelled");
                }
                case "remove" -> {
                    String friendId = Web.trimOrNull(req.getParameter(WebConst.Param.ID));
                    if (friendId == null) {
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
                                    java.util.Map.of()
                            ));
                        }
                    } catch (Exception ignore) {
                    }
                    Web.redirectOk(req, resp, WebConst.Path.FRIENDS, "The user has been removed from friends");
                }
                default -> Web.redirectErr(req, resp, WebConst.Path.FRIENDS, "Unknown action");
            }
        } catch (Exception e) {
            getServletContext().log("Friends POST error", e);
            Web.redirectErr(req, resp, WebConst.Path.FRIENDS, e.getMessage());
        }
    }

    private User requireAuth(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User u = (User) req.getSession(true).getAttribute(WebConst.Attr.USER);
        if (u == null) {
            Web.redirectErr(req, resp, WebConst.Path.LOGIN, "Please login");
        }
        return u;
    }
}
