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

public class NotificationsServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(NotificationsServlet.class);

    private NotificationRepository repo;

    @Override
    public void init() {
        this.repo = Web.ctxBean(getServletContext(), WebConst.Ctx.NOTIFY_REPO, NotificationRepository.class);
        log.debug("NotificationsServlet initialized with NotificationRepository={}", repo.getClass().getSimpleName());
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
        log.debug("Notifications page userId={} limit={} offset={}", me.getUserId(), limit, offset);
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

    private User requireAuth(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User u = (User) req.getSession(true).getAttribute(WebConst.Attr.USER);
        if (u == null) {
            log.warn("Unauthorized access to notifications, redirecting to login");
            Web.redirectErr(req, resp, WebConst.Path.LOGIN, "Please login");
        }
        return u;
    }
}
