package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.service.user.UserService;
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
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class UsersServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(UsersServlet.class);

    private UserService userService;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        this.userService = Web.ctxBean(config.getServletContext(), WebConst.Ctx.USER_SERVICE, UserService.class);
        log.debug("UsersServlet initialized userService={}", userService.getClass().getSimpleName());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Web.copyParamsToAttrs(req, WebConst.Attr.OK, WebConst.Attr.ERROR);
        Web.pullFlash(req, WebConst.Attr.OK);
        Web.pullFlash(req, WebConst.Attr.ERROR);
        String q = Web.trimOrNull(req.getParameter("q"));
        List<User> result;
        if (q == null) {
            result = userService.findAll();
            log.info("Users list requested: all users count={}", result.size());
        } else {
            Optional<User> byId = userService.findById(q);
            if (byId.isPresent()) {
                result = List.of(byId.get());
                log.info("Users search by id hit q={} userLogin={}", q, byId.get().getUserLogin());
            } else {
                Optional<User> byLogin = userService.findByLogin(q.toLowerCase(Locale.ROOT));
                result = byLogin.map(List::of).orElseGet(List::of);
                if (byLogin.isPresent()) {
                    log.info("Users search by login hit q={} userId={}", q, byLogin.get().getUserId());
                } else {
                    log.warn("Users search miss q={}", q);
                }
            }
        }
        req.setAttribute("users", result);
        Web.forward(req, resp, WebConst.Jsp.USERS);
    }
}
