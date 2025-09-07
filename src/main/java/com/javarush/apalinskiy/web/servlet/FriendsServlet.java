package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.friends.FriendService;
import com.javarush.apalinskiy.web.util.Web;
import com.javarush.apalinskiy.web.util.WebConst;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.Serial;

public class FriendsServlet extends HttpServlet {

    @Serial
    private static final long serialVersionUID = 1L;

    private FriendService service;

    @Override
    public void init() {
        this.service = Web.ctxBean(getServletContext(), WebConst.Ctx.FRIEND_SERVICE, FriendService.class);
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
