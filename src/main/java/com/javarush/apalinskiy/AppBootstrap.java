package com.javarush.apalinskiy;

import com.javarush.apalinskiy.repositories.InMemoryUserRepository;
import com.javarush.apalinskiy.repositories.UserRepository;
import com.javarush.apalinskiy.service.DefaultUserService;
import com.javarush.apalinskiy.service.UserService;
import com.javarush.apalinskiy.user.Role;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

public class AppBootstrap implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        UserRepository userRepo = new InMemoryUserRepository();
        UserService userService = new DefaultUserService(userRepo);
        try {
            userService.register(Role.ADMIN, "Admin", "admin", "admin");
        } catch (RuntimeException e) {
            if (!e.getClass().getSimpleName().equals("DuplicateLoginException")) {
                throw e;
            }
        }
        sce.getServletContext().setAttribute("userService", userService);
    }
}
