package com.javarush.apalinskiy.web.filter;

import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.repository.notify.NotificationRepository;
import com.javarush.apalinskiy.web.util.Web;
import com.javarush.apalinskiy.app.WebConst;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

public class NotificationsBadgeFilter implements Filter {

    private NotificationRepository repo;

    @Override
    public void init(FilterConfig cfg) {
        this.repo = Web.ctxBean(cfg.getServletContext(), WebConst.Ctx.NOTIFY_REPO, NotificationRepository.class);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String ctx = req.getContextPath();
        String uri = req.getRequestURI();
        if (uri.startsWith(ctx + "/assets/")) {
            chain.doFilter(request, response);
            return;
        }
        int unread = 0;
        HttpSession session = req.getSession(false);
        if (session != null) {
            User u = (User) session.getAttribute(WebConst.Attr.USER);
            if (u != null) {
                unread = repo.unreadCount(u.getUserId());
            }
        }
        request.setAttribute("unreadCount", unread);
        chain.doFilter(request, response);
    }
}
