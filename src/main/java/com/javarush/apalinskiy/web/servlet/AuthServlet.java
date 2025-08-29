package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.exceptions.DuplicateLoginException;
import com.javarush.apalinskiy.service.UserService;
import com.javarush.apalinskiy.user.Role;
import com.javarush.apalinskiy.user.User;
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

    private UserService userService;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        ServletContext ctx = config.getServletContext();
        UserService service = (UserService) ctx.getAttribute("userService");
        if (service == null) {
            throw new UnavailableException("UserService not found in ServletContext (attribute 'userService').");
        }
        this.userService = service;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
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
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();
        if (PATH_LOGIN.equals(path)) {
            handleLogin(req, resp);
        } else if (PATH_REGISTER.equals(path)) {
            handleRegister(req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String login = req.getParameter("userLogin");
        String pass = req.getParameter("password");
        Optional<User> found = userService.login(login, pass);
        if (found.isPresent()) {
            HttpSession old = req.getSession(false);
            if (old != null) {
                old.invalidate();
            }
            HttpSession fresh = req.getSession(true);
            fresh.setAttribute("user", found.get());
            resp.sendRedirect(req.getContextPath() + "/");
        } else {
            req.setAttribute("error", "Incorrect login or password");
            req.setAttribute("userLogin", login);
            forward(req, resp, JSP_LOGIN);
        }
    }

    private void handleRegister(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String name = req.getParameter("userName");
        String login = req.getParameter("userLogin");
        String pass = req.getParameter("password");
        try {
            User user = userService.register(Role.USER, name, login, pass);
            HttpSession old = req.getSession(false);
            if (old != null) old.invalidate();
            HttpSession fresh = req.getSession(true);
            fresh.setAttribute("user", user);
            resp.sendRedirect(req.getContextPath() + "/");
        } catch (DuplicateLoginException | IllegalArgumentException e) {
            req.setAttribute("error", e.getMessage());
            req.setAttribute("userName", name);
            req.setAttribute("userLogin", login);
            forward(req, resp, JSP_REGISTER);
        } catch (IllegalStateException e) {
            req.setAttribute("error", "Internal error. Please try again.");
            req.setAttribute("userName", name);
            req.setAttribute("userLogin", login);
            forward(req, resp, JSP_REGISTER);
        }
    }

    private void forward(HttpServletRequest req, HttpServletResponse resp, String jsp) throws ServletException, IOException {
        req.getRequestDispatcher(jsp).forward(req, resp);
    }
}
