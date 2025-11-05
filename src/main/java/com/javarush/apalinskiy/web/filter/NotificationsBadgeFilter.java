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
 * Servlet filter responsible for attaching the unread notifications count to each request.
 *
 * <p>This filter queries the {@link NotificationRepository} for the number of unread notifications
 * belonging to the currently logged-in user and exposes that count as a request attribute
 * named {@code "unreadCount"}. This allows JSP pages and controllers to easily display
 * notification badges in headers or navigation bars.</p>
 *
 * <p>Static assets under {@code /assets/} are skipped to avoid unnecessary database lookups.</p>
 */
public class NotificationsBadgeFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(NotificationsBadgeFilter.class);

    private NotificationRepository repo;

    /**
     * Initializes the filter and retrieves the {@link NotificationRepository} bean from the servlet context.
     *
     * @param cfg filter configuration provided by the servlet container
     */
    @Override
    public void init(FilterConfig cfg) {
        this.repo = Web.ctxBean(cfg.getServletContext(), WebConst.Ctx.NOTIFY_REPO, NotificationRepository.class);
        log.debug("NotificationsBadgeFilter initialized with repo={}", repo.getClass().getSimpleName());
    }

    /**
     * Adds an {@code unreadCount} attribute to the request for logged-in users.
     *
     * <p>The filter skips requests to static resources under {@code /assets/} for efficiency.
     * For authenticated users, it retrieves the number of unread notifications and attaches
     * it to the current request scope.</p>
     *
     * @param request  incoming servlet request
     * @param response outgoing servlet response
     * @param chain    filter chain to continue processing
     * @throws IOException      if an I/O error occurs
     * @throws ServletException if request forwarding fails
     */
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
