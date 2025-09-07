package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.mail.NotificationRepository;
import com.javarush.apalinskiy.web.util.Web;
import com.javarush.apalinskiy.web.util.WebConst;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.Serial;

public class NotificationsServlet extends HttpServlet {

    @Serial
    private static final long serialVersionUID = 1L;

    private NotificationRepository repo;

    @Override
    public void init() {
        this.repo = Web.ctxBean(getServletContext(), WebConst.Ctx.NOTIFY_REPO, NotificationRepository.class);
    }

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
        Web.forward(req, resp, WebConst.Jsp.NOTIFICATIONS);
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
                case "markRead" -> {
                    String id = Web.trimOrNull(req.getParameter(WebConst.Param.ID));
                    if (id != null) repo.markRead(me.getUserId(), id);
                    Web.redirectOk(req, resp, WebConst.Path.NOTIFICATIONS, "Marked as read");
                }
                case "markAll" -> {
                    repo.markAllRead(me.getUserId());
                    Web.redirectOk(req, resp, WebConst.Path.NOTIFICATIONS, "All notifications are marked as read");
                }
                case "clearAll" -> {
                    repo.clearAll(me.getUserId());
                    Web.redirectOk(req, resp, WebConst.Path.NOTIFICATIONS, "The list has been cleared");
                }
                default -> Web.redirectErr(req, resp, WebConst.Path.NOTIFICATIONS, "Unknown action");
            }
        } catch (Exception e) {
            getServletContext().log("Notifications POST error", e);
            Web.redirectErr(req, resp, WebConst.Path.NOTIFICATIONS, "Error: " + e.getMessage());
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
