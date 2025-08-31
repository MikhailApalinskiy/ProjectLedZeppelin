package com.javarush.apalinskiy.web.listener;

import com.javarush.apalinskiy.repositories.InMemoryUserRepository;
import com.javarush.apalinskiy.repositories.QuestRepository;
import com.javarush.apalinskiy.repositories.UserRepository;
import com.javarush.apalinskiy.service.DefaultQuestService;
import com.javarush.apalinskiy.service.DefaultUserService;
import com.javarush.apalinskiy.service.QuestService;
import com.javarush.apalinskiy.service.UserService;
import com.javarush.apalinskiy.user.Role;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

public class AppBootstrap implements ServletContextListener {

    public static final String ATTR_USER_SERVICE = "userService";
    public static final String ATTR_QUEST_SERVICE = "questService";
    private static final String QUEST_RESOURCE = "quest.json";
    private static final int QUEST_START_ID = 1;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext ctx = sce.getServletContext();
        UserRepository userRepo = new InMemoryUserRepository();
        UserService userService = new DefaultUserService(userRepo);
        try {
            userService.register(Role.ADMIN, "Admin", "admin", "admin");
        } catch (RuntimeException e) {
            if (!"DuplicateLoginException".equals(e.getClass().getSimpleName())) {
                throw e;
            }
        }
        ctx.setAttribute(ATTR_USER_SERVICE, userService);
        try {
            QuestRepository repo = QuestRepository.fromClasspath(QUEST_RESOURCE, QUEST_START_ID);
            QuestService questService = new DefaultQuestService(repo);
            ctx.setAttribute(ATTR_QUEST_SERVICE, questService);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load quest resource: " + QUEST_RESOURCE, e);
        }
    }
}
