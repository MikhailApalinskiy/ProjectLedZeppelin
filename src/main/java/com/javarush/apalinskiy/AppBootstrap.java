package com.javarush.apalinskiy;

import com.javarush.apalinskiy.repositories.InMemoryUserRepository;
import com.javarush.apalinskiy.repositories.UserRepository;
import com.javarush.apalinskiy.user.Role;
import com.javarush.apalinskiy.user.User;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class AppBootstrap implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext ctx = sce.getServletContext();
        UserRepository users = new InMemoryUserRepository();
        users.save(User.of(Role.ADMIN, "Admin", "admin", "admin"));
        ctx.setAttribute("users", users);
    }
}
