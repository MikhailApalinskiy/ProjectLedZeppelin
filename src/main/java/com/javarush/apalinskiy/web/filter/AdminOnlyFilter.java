package com.javarush.apalinskiy.web.filter;

import com.javarush.apalinskiy.domain.user.Role;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.web.util.Web;
import com.javarush.apalinskiy.app.WebConst;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class AdminOnlyFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(AdminOnlyFilter.class);

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest r = (HttpServletRequest) req;
        HttpServletResponse w = (HttpServletResponse) resp;
        User me = (User) r.getSession().getAttribute(WebConst.Attr.USER);
        String path = r.getRequestURI();
        if (me == null) {
            log.warn("Access denied: anonymous user tried to access {}", path);
            Web.redirectErr(r, w, WebConst.Path.HOME, "Access denied");
            return;
        }
        if (me.getRole() != Role.ADMIN) {
            log.warn("Access denied: user id={} login='{}' role={} tried to access {}",
                    me.getUserId(), me.getUserLogin(), me.getRole(), path);
            Web.redirectErr(r, w, WebConst.Path.HOME, "Access denied");
            return;
        }
        log.info("Admin access granted id={} login='{}' path={}", me.getUserId(), me.getUserLogin(), path);
        chain.doFilter(req, resp);
    }
}
