package com.javarush.apalinskiy.app;

import com.javarush.apalinskiy.repository.quest.CustomQuestRepository;
import com.javarush.apalinskiy.repository.user.UserRepository;
import com.javarush.apalinskiy.service.impl.notify.DefaultNotificationService;
import com.javarush.apalinskiy.service.impl.quest.DefaultQuestService;
import com.javarush.apalinskiy.service.impl.social.DefaultFriendService;
import com.javarush.apalinskiy.service.impl.user.DefaultUserService;
import com.javarush.apalinskiy.service.impl.user.InMemoryUserStatsService;
import com.javarush.apalinskiy.service.save.SaveStateService;
import com.javarush.apalinskiy.repository.social.FriendRepository;
import com.javarush.apalinskiy.repository.inmemory.social.InMemoryFriendRepository;
import com.javarush.apalinskiy.repository.inmemory.quest.InMemoryCustomQuestRepository;
import com.javarush.apalinskiy.repository.inmemory.quest.InMemoryQuestStore;
import com.javarush.apalinskiy.service.impl.save.InMemorySaveStateService;
import com.javarush.apalinskiy.repository.inmemory.user.InMemoryUserRepository;
import com.javarush.apalinskiy.repository.inmemory.notify.InMemoryNotificationRepository;
import com.javarush.apalinskiy.repository.notify.NotificationRepository;
import com.javarush.apalinskiy.domain.user.Role;
import com.javarush.apalinskiy.service.notify.NotificationService;
import com.javarush.apalinskiy.service.social.FriendService;
import com.javarush.apalinskiy.service.user.UserStatsService;
import com.javarush.apalinskiy.service.quest.QuestAuthoringService;
import com.javarush.apalinskiy.service.quest.QuestService;
import com.javarush.apalinskiy.service.user.UserService;
import com.javarush.apalinskiy.web.util.Uploads;
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
        UserStatsService userStats = new InMemoryUserStatsService();
        ctx.setAttribute(WebConst.Ctx.USER_STATS_SERVICE, userStats);
        Uploads.resolveBaseDir(ctx);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }
}
