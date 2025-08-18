package com.javarush.apalinskiy.web;

import com.javarush.apalinskiy.repositories.UserRepository;
import com.javarush.apalinskiy.user.User;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

public class LoginServlet extends HttpServlet {
    private UserRepository users;

    @Override
    public void init() {
        this.users = (UserRepository) getServletContext().getAttribute("users");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String login = req.getParameter("userLogin");
        String pass = req.getParameter("password");

        Optional<User> u = users.findByLogin(login)
                .filter(it -> it.getPassword().equals(pass));
        if (u.isPresent()) {
            req.getSession(true).setAttribute("user", u.get());
            resp.sendRedirect(req.getContextPath() + "/");
        } else {
            req.setAttribute("error", "Неверный логин или пароль");
            req.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(req, resp);
        }
    }
}
