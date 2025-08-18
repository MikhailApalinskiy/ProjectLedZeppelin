package com.javarush.apalinskiy.web;

import com.javarush.apalinskiy.repositories.UserRepository;
import com.javarush.apalinskiy.user.Role;
import com.javarush.apalinskiy.user.User;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RegisterServlet extends HttpServlet {
    private UserRepository users;

    @Override
    public void init() {
        this.users = (UserRepository) getServletContext().getAttribute("users");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/jsp/register.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String name  = req.getParameter("userName");
        String login = req.getParameter("userLogin");
        String pass  = req.getParameter("password");
        try {
            User u = User.of(Role.USER, name, login, pass);
            users.save(u);
            req.getSession(true).setAttribute("user", u);
            resp.sendRedirect(req.getContextPath() + "/");
        } catch (IllegalStateException e) {
            req.setAttribute("error", "Логин уже занят");
            req.getRequestDispatcher("/WEB-INF/jsp/register.jsp").forward(req, resp);
        }
    }
}
