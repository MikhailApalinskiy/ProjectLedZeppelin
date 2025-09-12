package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.service.user.UserService;
import com.javarush.apalinskiy.service.user.UserStatsService;
import com.javarush.apalinskiy.web.util.Web;
import com.javarush.apalinskiy.app.WebConst;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

public class UserPublicProfileServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(UserPublicProfileServlet.class);

    private transient UserService userService;
    private transient UserStatsService userStatsService;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        this.userService = Web.ctxBean(config.getServletContext(), WebConst.Ctx.USER_SERVICE, UserService.class);
        try {
            this.userStatsService = Web.ctxBean(config.getServletContext(), WebConst.Ctx.USER_STATS_SERVICE, UserStatsService.class);
        } catch (IllegalStateException ignore) {
            this.userStatsService = null;
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Web.copyParamsToAttrs(req, WebConst.Attr.OK, WebConst.Attr.ERROR);
        Web.pullFlash(req, WebConst.Attr.OK);
        Web.pullFlash(req, WebConst.Attr.ERROR);
        String userId = Web.trimOrNull(req.getParameter("id"));
        User viewUser = null;
        if (userId != null) {
            Optional<User> opt = userService.findById(userId);
            viewUser = opt.orElse(null);
            if (viewUser != null) {
                log.info("Public profile view userId={} login={}", viewUser.getUserId(), viewUser.getUserLogin());
            } else {
                log.warn("Public profile requested but not found id={}", userId);
            }
        } else {
            log.warn("Public profile requested without id param");
        }
        req.setAttribute("profileUser", viewUser);
        if (viewUser != null && userStatsService != null) {
            req.setAttribute("stats", userStatsService.statsOf(viewUser.getUserId()));
        }
        Web.forward(req, resp, WebConst.Jsp.USER_PUBLIC);
    }
}
