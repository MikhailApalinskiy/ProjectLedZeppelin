package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.service.UserService;
import com.javarush.apalinskiy.web.util.Web;
import com.javarush.apalinskiy.web.util.WebConst;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class UsersServlet extends HttpServlet {
    private UserService userService;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        this.userService = Web.ctxBean(config.getServletContext(), WebConst.Ctx.USER_SERVICE, UserService.class);
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
        } else {
            Optional<User> byId = userService.findById(q);
            if (byId.isPresent()) {
                result = List.of(byId.get());
            } else {
                Optional<User> byLogin = userService.findByLogin(q.toLowerCase(Locale.ROOT));
                result = byLogin.map(List::of).orElseGet(List::of);
            }
        }
        req.setAttribute("users", result);
        Web.forward(req, resp, WebConst.Jsp.USERS);
    }
}
