package com.javarush.apalinskiy.web.filter;

import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.repository.notify.NotificationRepository;
import com.javarush.apalinskiy.web.util.Web;
import com.javarush.apalinskiy.app.WebConst;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Servlet filter that attaches the number of unread notifications to each request.
 * <p>
 * This filter checks the current HTTP session for a logged-in {@link User} and,
 * if present, queries the {@link NotificationRepository} for the count of unread
 * notifications. The result is then stored as a request attribute {@code "unreadCount"},
 * making it available to JSPs, templates, and downstream servlets.
 * </p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Initialize the {@link NotificationRepository} from servlet context.</li>
 *   <li>Skip processing for static asset requests (under {@code /assets/}).</li>
 *   <li>Retrieve the current user from the HTTP session (if any).</li>
 *   <li>Query the repository for the unread notification count.</li>
 *   <li>Expose this count as request attribute {@code "unreadCount"}.</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <p>
 * Typically mapped to all dynamic requests so that JSP pages can easily render
 * a badge with the number of unread notifications in the UI.
 * </p>
 */
public class NotificationsBadgeFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(NotificationsBadgeFilter.class);

    private NotificationRepository repo;

    @Override
    public void init(FilterConfig cfg) {
        this.repo = Web.ctxBean(cfg.getServletContext(), WebConst.Ctx.NOTIFY_REPO, NotificationRepository.class);
        log.debug("NotificationsBadgeFilter initialized with repo={}", repo.getClass().getSimpleName());
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
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
                log.debug("Unread notifications userId={} count={}", u.getUserId(), unread);
            }
        }
        request.setAttribute("unreadCount", unread);
        chain.doFilter(request, response);
    }
}
