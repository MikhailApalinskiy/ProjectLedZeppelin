package com.javarush.apalinskiy.web.listener;

import com.javarush.apalinskiy.application.ports.CustomQuestRepository;
import com.javarush.apalinskiy.application.ports.UserRepository;
import com.javarush.apalinskiy.application.quests.*;
import com.javarush.apalinskiy.application.save.SaveStateService;
import com.javarush.apalinskiy.friends.DefaultFriendService;
import com.javarush.apalinskiy.friends.FriendRepository;
import com.javarush.apalinskiy.friends.FriendService;
import com.javarush.apalinskiy.friends.InMemoryFriendRepository;
import com.javarush.apalinskiy.infra.catalog.InMemoryCustomQuestRepository;
import com.javarush.apalinskiy.infra.quest.InMemoryQuestStore;
import com.javarush.apalinskiy.infra.save.InMemorySaveStateService;
import com.javarush.apalinskiy.infra.users.InMemoryUserRepository;
import com.javarush.apalinskiy.mail.DefaultNotificationService;
import com.javarush.apalinskiy.mail.InMemoryNotificationRepository;
import com.javarush.apalinskiy.mail.NotificationRepository;
import com.javarush.apalinskiy.mail.NotificationService;
import com.javarush.apalinskiy.service.*;
import com.javarush.apalinskiy.domain.user.Role;
import com.javarush.apalinskiy.web.util.WebConst;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import java.io.IOException;

public class AppBootstrap implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext ctx = sce.getServletContext();
        UserRepository userRepo = new InMemoryUserRepository();
        UserService userService = new DefaultUserService(userRepo);
        try {
            userService.register(
                    Role.ADMIN,
                    WebConst.App.DEFAULT_ADMIN_NAME,
                    WebConst.App.DEFAULT_ADMIN_LOGIN,
                    WebConst.App.DEFAULT_ADMIN_PASS
            );
        } catch (RuntimeException e) {
            if (!"DuplicateLoginException".equals(e.getClass().getSimpleName())) {
                throw e;
            }
        }
        ctx.setAttribute(WebConst.Ctx.USER_SERVICE, userService);
        final InMemoryQuestStore prodRepo;
        final QuestService questService;
        try {
            prodRepo = InMemoryQuestStore.fromClasspath(
                    WebConst.App.QUEST_RESOURCE,
                    WebConst.App.QUEST_START_ID
            );
            questService = new DefaultQuestService(prodRepo);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load quest resource: " + WebConst.App.QUEST_RESOURCE, e);
        }
        ctx.setAttribute(WebConst.Ctx.PROD_REPOSITORY, prodRepo);
        ctx.setAttribute(WebConst.Ctx.QUEST_SERVICE, questService);
        InMemoryQuestStore editorRepo = InMemoryQuestStore.empty(WebConst.App.QUEST_START_ID);
        ctx.setAttribute(WebConst.Ctx.EDITOR_REPOSITORY, editorRepo);
        CustomQuestRepository catalog = new InMemoryCustomQuestRepository();
        QuestAuthoringService authoring = new QuestAuthoringService(editorRepo, prodRepo, catalog);
        ctx.setAttribute(WebConst.Ctx.AUTHORING_SERVICE, authoring);
        SaveStateService saveStateService = new InMemorySaveStateService();
        ctx.setAttribute(WebConst.Ctx.SAVE_STATE_SERVICE, saveStateService);
        NotificationRepository notifyRepo = new InMemoryNotificationRepository();
        ctx.setAttribute(WebConst.Ctx.NOTIFY_REPO, notifyRepo);
        NotificationService notifyService = new DefaultNotificationService(notifyRepo, userService);
        ctx.setAttribute(WebConst.Ctx.NOTIFY_SERVICE, notifyService);
        FriendRepository friendRepo = new InMemoryFriendRepository();
        ctx.setAttribute(WebConst.Ctx.FRIEND_REPOSITORY, friendRepo);
        FriendService friendService = new DefaultFriendService(friendRepo, userService, notifyService);
        ctx.setAttribute(WebConst.Ctx.FRIEND_SERVICE, friendService);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // no-op
    }
}
