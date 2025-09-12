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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class AppBootstrap implements ServletContextListener {

    private static final Logger log = LoggerFactory.getLogger(AppBootstrap.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        log.info("App context initialization started");
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
            log.info("Default admin created login={}", WebConst.App.DEFAULT_ADMIN_LOGIN);
        } catch (RuntimeException e) {
            if (!"DuplicateLoginException".equals(e.getClass().getSimpleName())) {
                log.error("Failed to register default admin login={}", WebConst.App.DEFAULT_ADMIN_LOGIN, e);
                throw e;
            } else {
                log.warn("Default admin already exists login={}", WebConst.App.DEFAULT_ADMIN_LOGIN);
            }
        }
        ctx.setAttribute(WebConst.Ctx.USER_SERVICE, userService);
        log.debug("UserService registered in servlet context key={}", WebConst.Ctx.USER_SERVICE);
        final InMemoryQuestStore prodRepo;
        final QuestService questService;
        try {
            prodRepo = InMemoryQuestStore.fromClasspath(
                    WebConst.App.QUEST_RESOURCE,
                    WebConst.App.QUEST_START_ID
            );
            questService = new DefaultQuestService(prodRepo);
            log.info("Quest resource loaded resource={} startId={}",
                    WebConst.App.QUEST_RESOURCE, WebConst.App.QUEST_START_ID);
        } catch (IOException e) {
            log.error("Failed to load quest resource resource={}", WebConst.App.QUEST_RESOURCE, e);
            throw new RuntimeException("Failed to load quest resource: " + WebConst.App.QUEST_RESOURCE, e);
        }
        ctx.setAttribute(WebConst.Ctx.PROD_REPOSITORY, prodRepo);
        ctx.setAttribute(WebConst.Ctx.QUEST_SERVICE, questService);
        log.debug("ProdQuestRepo and QuestService registered keys=[{}, {}]",
                WebConst.Ctx.PROD_REPOSITORY, WebConst.Ctx.QUEST_SERVICE);
        InMemoryQuestStore editorRepo = InMemoryQuestStore.empty(WebConst.App.QUEST_START_ID);
        ctx.setAttribute(WebConst.Ctx.EDITOR_REPOSITORY, editorRepo);
        CustomQuestRepository catalog = new InMemoryCustomQuestRepository();
        QuestAuthoringService authoring = new QuestAuthoringService(editorRepo, prodRepo, catalog);
        ctx.setAttribute(WebConst.Ctx.AUTHORING_SERVICE, authoring);
        log.debug("EditorRepo and AuthoringService registered keys=[{}, {}]",
                WebConst.Ctx.EDITOR_REPOSITORY, WebConst.Ctx.AUTHORING_SERVICE);
        SaveStateService saveStateService = new InMemorySaveStateService();
        ctx.setAttribute(WebConst.Ctx.SAVE_STATE_SERVICE, saveStateService);
        NotificationRepository notifyRepo = new InMemoryNotificationRepository();
        ctx.setAttribute(WebConst.Ctx.NOTIFY_REPO, notifyRepo);
        NotificationService notifyService = new DefaultNotificationService(notifyRepo, userService);
        ctx.setAttribute(WebConst.Ctx.NOTIFY_SERVICE, notifyService);
        log.debug("SaveState/Notify registered keys=[{}, {}, {}]",
                WebConst.Ctx.SAVE_STATE_SERVICE, WebConst.Ctx.NOTIFY_REPO, WebConst.Ctx.NOTIFY_SERVICE);
        FriendRepository friendRepo = new InMemoryFriendRepository();
        ctx.setAttribute(WebConst.Ctx.FRIEND_REPOSITORY, friendRepo);
        FriendService friendService = new DefaultFriendService(friendRepo, userService, notifyService);
        ctx.setAttribute(WebConst.Ctx.FRIEND_SERVICE, friendService);
        UserStatsService userStats = new InMemoryUserStatsService();
        ctx.setAttribute(WebConst.Ctx.USER_STATS_SERVICE, userStats);
        log.debug("Friends/UserStats registered keys=[{}, {}]",
                WebConst.Ctx.FRIEND_REPOSITORY, WebConst.Ctx.USER_STATS_SERVICE);
        try {
            Uploads.resolveBaseDir(ctx);
            log.info("Uploads base directory resolved");
        } catch (RuntimeException e) {
            log.error("Failed to resolve uploads base directory", e);
            throw e;
        }
        log.info("App context initialization finished successfully");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log.info("App context is being destroyed");
    }
}
