package com.javarush.apalinskiy.web.listener;

import com.javarush.apalinskiy.repositories.InMemoryUserRepository;
import com.javarush.apalinskiy.repositories.QuestRepository;
import com.javarush.apalinskiy.repositories.UserRepository;
import com.javarush.apalinskiy.save.SaveExpander;
import com.javarush.apalinskiy.service.*;
import com.javarush.apalinskiy.user.Role;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

public class AppBootstrap implements ServletContextListener {

    public static final String ATTR_USER_SERVICE = "userService";
    public static final String ATTR_QUEST_SERVICE = "questService";
    public static final String ATTR_SAVE_STATE_SERVICE = "saveStateService";
    public static final String ATTR_SAVE_EXPANDER = "saveExpander";

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
        final QuestService questService;
        try {
            QuestRepository repo = QuestRepository.fromClasspath(QUEST_RESOURCE, QUEST_START_ID);
            questService = new DefaultQuestService(repo);
            ctx.setAttribute(ATTR_QUEST_SERVICE, questService);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load quest resource: " + QUEST_RESOURCE, e);
        }
        SaveStateService saveStateService = new InMemorySaveStateService();
        ctx.setAttribute(ATTR_SAVE_STATE_SERVICE, saveStateService);
        SaveExpander expander = new SaveExpander(questService, saveStateService);
        ctx.setAttribute(ATTR_SAVE_EXPANDER, expander);
    }
}
