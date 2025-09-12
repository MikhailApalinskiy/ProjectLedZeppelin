package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.exceptions.DuplicateLoginException;
import com.javarush.apalinskiy.service.user.UserService;
import com.javarush.apalinskiy.domain.user.Role;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.web.util.Web;
import com.javarush.apalinskiy.app.WebConst;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

public class AuthServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(AuthServlet.class);

    private transient UserService userService;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            this.userService = Web.ctxBean(config.getServletContext(), WebConst.Ctx.USER_SERVICE, UserService.class);
            log.debug("AuthServlet initialized");
        } catch (IllegalStateException e) {
            log.error("Initialization failed: UserService not found in ServletContext", e);
            throw new UnavailableException("UserService not found in ServletContext.");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getServletPath();
        if (WebConst.Path.LOGIN.equals(path)) {
            log.debug("GET /login");
            Web.forward(req, resp, WebConst.Jsp.LOGIN);
        } else if (WebConst.Path.REGISTER.equals(path)) {
            log.debug("GET /register");
            Web.forward(req, resp, WebConst.Jsp.REGISTER);
        } else {
            log.warn("GET unknown path={}", path);
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getServletPath();
        if (WebConst.Path.LOGIN.equals(path)) {
            handleLogin(req, resp);
        } else if (WebConst.Path.REGISTER.equals(path)) {
            handleRegister(req, resp);
        } else {
            log.warn("POST unknown path={}", path);
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        String login = req.getParameter(WebConst.Param.USER_LOGIN);
        String pass = req.getParameter(WebConst.Param.PASSWORD);
        Optional<User> found = userService.login(login, pass);
        if (found.isPresent()) {
            Web.renewSessionAndPut(req, WebConst.Attr.USER, found.get());
            String target = Web.safeNextOrHome(req, req.getParameter(WebConst.Param.NEXT));
            log.info("Login success login='{}' userId={} redirect={}", login, found.get().getUserId(), target);
            resp.sendRedirect(resp.encodeRedirectURL(target));
        } else {
            log.warn("Login failed login='{}'", login);
            req.setAttribute(WebConst.Attr.ERROR, WebConst.Msg.BAD_CREDENTIALS);
            req.setAttribute("userLogin", login);
            Web.forward(req, resp, WebConst.Jsp.LOGIN);
        }
    }

    private void handleRegister(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        String name = req.getParameter(WebConst.Param.USER_NAME);
        String login = req.getParameter(WebConst.Param.USER_LOGIN);
        String pass = req.getParameter(WebConst.Param.PASSWORD);
        try {
            User user = userService.register(Role.USER, name, login, pass);
            Web.renewSessionAndPut(req, WebConst.Attr.USER, user);
            String target = Web.safeNextOrHome(req, req.getParameter(WebConst.Param.NEXT));
            log.info("Register success userId={} login='{}' redirect={}", user.getUserId(), user.getUserLogin(), target);
            resp.sendRedirect(resp.encodeRedirectURL(target));
        } catch (DuplicateLoginException | IllegalArgumentException e) {
            log.warn("Register failed login='{}' reason={}", login, e.getMessage());
            req.setAttribute(WebConst.Attr.ERROR, e.getMessage());
            req.setAttribute("userName", name);
            req.setAttribute("userLogin", login);
            Web.forward(req, resp, WebConst.Jsp.REGISTER);
        } catch (IllegalStateException e) {
            log.error("Register internal error login='{}'", login, e);
            req.setAttribute(WebConst.Attr.ERROR, WebConst.Msg.INTERNAL_ERROR);
            req.setAttribute("userName", name);
            req.setAttribute("userLogin", login);
            Web.forward(req, resp, WebConst.Jsp.REGISTER);
        }
    }
}