package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.exceptions.DuplicateLoginException;
import com.javarush.apalinskiy.service.UserService;
import com.javarush.apalinskiy.user.Role;
import com.javarush.apalinskiy.user.User;
import com.javarush.apalinskiy.web.listener.AppBootstrap;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Optional;

public class AuthServlet extends HttpServlet {

    private static final String PATH_LOGIN = "/login";
    private static final String PATH_REGISTER = "/register";
    private static final String JSP_LOGIN = "/WEB-INF/jsp/login.jsp";
    private static final String JSP_REGISTER = "/WEB-INF/jsp/register.jsp";
    private static final String P_USER = "userLogin";
    private static final String P_PASS = "password";
    private static final String P_NAME = "userName";
    private static final String P_NEXT = "next";
    private static final String A_ERROR = "error";
    private static final String A_USER = "user";
    private static final String A_USERLOGIN = "userLogin";
    private static final String A_USERNAME = "userName";
    private static final String MSG_BAD_CRED = "Incorrect login or password";
    private static final String MSG_INTERNAL = "Internal error. Please try again.";

    private transient UserService userService;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        ServletContext ctx = config.getServletContext();
        Object svc = ctx.getAttribute(AppBootstrap.ATTR_USER_SERVICE);
        if (!(svc instanceof UserService)) {
            throw new UnavailableException("UserService not found in ServletContext.");
        }
        this.userService = (UserService) svc;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getServletPath();
        if (PATH_LOGIN.equals(path)) {
            forward(req, resp, JSP_LOGIN);
        } else if (PATH_REGISTER.equals(path)) {
            forward(req, resp, JSP_REGISTER);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getServletPath();
        if (PATH_LOGIN.equals(path)) {
            handleLogin(req, resp);
        } else if (PATH_REGISTER.equals(path)) {
            handleRegister(req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        String login = req.getParameter(P_USER);
        String pass = req.getParameter(P_PASS);
        Optional<User> found = userService.login(login, pass);
        if (found.isPresent()) {
            renewSessionAndPutUser(req, found.get());
            String target = safeNextOrHome(req, req.getParameter(P_NEXT));
            resp.sendRedirect(resp.encodeRedirectURL(target));
        } else {
            req.setAttribute(A_ERROR, MSG_BAD_CRED);
            req.setAttribute(A_USERLOGIN, login);
            forward(req, resp, JSP_LOGIN);
        }
    }

    private void handleRegister(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        String name = req.getParameter(P_NAME);
        String login = req.getParameter(P_USER);
        String pass = req.getParameter(P_PASS);
        try {
            User user = userService.register(Role.USER, name, login, pass);
            renewSessionAndPutUser(req, user);
            String target = safeNextOrHome(req, req.getParameter(P_NEXT));
            resp.sendRedirect(resp.encodeRedirectURL(target));
        } catch (DuplicateLoginException | IllegalArgumentException e) {
            req.setAttribute(A_ERROR, e.getMessage());
            req.setAttribute(A_USERNAME, name);
            req.setAttribute(A_USERLOGIN, login);
            forward(req, resp, JSP_REGISTER);
        } catch (IllegalStateException e) {
            req.setAttribute(A_ERROR, MSG_INTERNAL);
            req.setAttribute(A_USERNAME, name);
            req.setAttribute(A_USERLOGIN, login);
            forward(req, resp, JSP_REGISTER);
        }
    }

    private void renewSessionAndPutUser(HttpServletRequest req, User user) {
        HttpSession old = req.getSession(false);
        if (old != null) {
            old.invalidate();
        }
        HttpSession fresh = req.getSession(true);
        fresh.setAttribute(A_USER, user);
    }

    private static String safeNextOrHome(HttpServletRequest req, String next) {
        return isSafeNext(req, next) ? next : (req.getContextPath() + "/");
    }

    private static boolean isSafeNext(HttpServletRequest req, String next) {
        if (next == null || next.isBlank()) {
            return false;
        }
        String ctx = req.getContextPath();
        return next.startsWith(ctx + "/");
    }

    private static void forward(HttpServletRequest req, HttpServletResponse resp, String jsp)
            throws ServletException, IOException {
        req.getRequestDispatcher(jsp).forward(req, resp);
    }
}