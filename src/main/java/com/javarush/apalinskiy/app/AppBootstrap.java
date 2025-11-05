package com.javarush.apalinskiy.app;

import com.javarush.apalinskiy.utils.CurrentUserContext;
import com.javarush.apalinskiy.utils.CurrentUserProvider;
import com.javarush.apalinskiy.utils.HibernateUtil;
import com.javarush.apalinskiy.repository.hibernate.quest.HDraftRepository;
import com.javarush.apalinskiy.repository.quest.CustomQuestRepository;
import com.javarush.apalinskiy.repository.user.UserRepository;
import com.javarush.apalinskiy.service.impl.notify.DefaultNotificationService;
import com.javarush.apalinskiy.service.impl.quest.DefaultQuestService;
import com.javarush.apalinskiy.service.impl.social.DefaultFriendService;
import com.javarush.apalinskiy.service.impl.user.DefaultUserService;
import com.javarush.apalinskiy.service.impl.user.HUserStatsService;
import com.javarush.apalinskiy.service.save.SaveStateService;
import com.javarush.apalinskiy.repository.social.FriendRepository;
import com.javarush.apalinskiy.repository.hibernate.social.HFriendRepository;
import com.javarush.apalinskiy.repository.hibernate.quest.HCustomQuestRepository;
import com.javarush.apalinskiy.repository.hibernate.quest.InMemoryQuestStore;
import com.javarush.apalinskiy.service.impl.save.HSaveStateService;
import com.javarush.apalinskiy.repository.hibernate.user.HUserRepository;
import com.javarush.apalinskiy.repository.hibernate.notify.HNotificationRepository;
import com.javarush.apalinskiy.repository.notify.NotificationRepository;
import com.javarush.apalinskiy.service.notify.NotificationService;
import com.javarush.apalinskiy.service.social.FriendService;
import com.javarush.apalinskiy.service.quest.QuestAuthoringService;
import com.javarush.apalinskiy.service.quest.QuestService;
import com.javarush.apalinskiy.service.user.UserService;
import com.javarush.apalinskiy.web.util.Uploads;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Initializes and shuts down the core application infrastructure for the TextQuest web platform.
 *
 * <p>This {@link ServletContextListener} is automatically invoked by the servlet container
 * (e.g. Tomcat) when the application is deployed or undeployed. During startup,
 * it prepares and registers all primary services, repositories, and utilities
 * as context attributes in the {@link ServletContext}.</p>
 *
 * <p>The listener builds and wires both in-memory and Hibernate-backed repositories,
 * sets up quest authoring and notification systems, and configures upload directories.
 * Upon shutdown, it ensures that {@link HibernateUtil#shutdown()} is called to release
 * all database resources.</p>
 *
 * <h2>Registered context attributes</h2>
 * <ul>
 *   <li>{@code WebConst.Ctx.USER_SERVICE} — {@link UserService}</li>
 *   <li>{@code WebConst.Ctx.PROD_REPOSITORY} — {@link InMemoryQuestStore}</li>
 *   <li>{@code WebConst.Ctx.QUEST_SERVICE} — {@link QuestService}</li>
 *   <li>{@code WebConst.Ctx.EDITOR_REPOSITORY} — {@link InMemoryQuestStore}</li>
 *   <li>{@code WebConst.Ctx.AUTHORING_SERVICE} — {@link QuestAuthoringService}</li>
 *   <li>{@code WebConst.Ctx.SAVE_STATE_SERVICE} — {@link SaveStateService}</li>
 *   <li>{@code WebConst.Ctx.NOTIFY_REPO} — {@link NotificationRepository}</li>
 *   <li>{@code WebConst.Ctx.NOTIFY_SERVICE} — {@link NotificationService}</li>
 *   <li>{@code WebConst.Ctx.FRIEND_REPOSITORY} — {@link FriendRepository}</li>
 *   <li>{@code WebConst.Ctx.FRIEND_SERVICE} — {@link FriendService}</li>
 *   <li>{@code WebConst.Ctx.USER_STATS_SERVICE} — {@link HUserStatsService}</li>
 * </ul>
 *
 * <h2>Lifecycle summary</h2>
 * <ul>
 *   <li><b>{@link #contextInitialized(ServletContextEvent)}:</b> Creates and binds all beans to the servlet context.</li>
 *   <li><b>{@link #contextDestroyed(ServletContextEvent)}:</b> Shuts down Hibernate and logs cleanup completion.</li>
 * </ul>
 *
 * @see ServletContext
 * @see HibernateUtil
 * @see QuestAuthoringService
 * @see InMemoryQuestStore
 * @see DefaultUserService
 * @see DefaultNotificationService
 * @see DefaultFriendService
 */
public class AppBootstrap implements ServletContextListener {

    /**
     * Application logger instance.
     */
    private static final Logger log = LoggerFactory.getLogger(AppBootstrap.class);

    /**
     * Called when the web application context is being initialized.
     * <p>Registers all major application services, repositories, and supporting
     * infrastructure components into the {@link ServletContext}.</p>
     *
     * @param sce the {@link ServletContextEvent} containing the servlet context
     * @throws RuntimeException if any critical service (e.g. quest resources)
     *                          cannot be loaded or initialized
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        log.info("App context initialization started");
        ServletContext ctx = sce.getServletContext();
        UserRepository userRepo = new HUserRepository();
        UserService userService = new DefaultUserService(userRepo);
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
        CustomQuestRepository catalog = new HCustomQuestRepository();
        QuestAuthoringService authoring = new QuestAuthoringService(editorRepo, prodRepo, catalog);
        SessionFactory sf = HibernateUtil.getSessionFactory();
        HDraftRepository draftRepo = new HDraftRepository(sf);
        CurrentUserProvider currentUser = CurrentUserContext::require;
        authoring.enableDrafts(draftRepo, currentUser);
        ctx.setAttribute(WebConst.Ctx.AUTHORING_SERVICE, authoring);
        log.debug("Registered: EDITOR_REPOSITORY, AUTHORING_SERVICE (drafts enabled)");
        SaveStateService saveStateService = new HSaveStateService();
        ctx.setAttribute(WebConst.Ctx.SAVE_STATE_SERVICE, saveStateService);
        NotificationRepository notifyRepo = new HNotificationRepository();
        ctx.setAttribute(WebConst.Ctx.NOTIFY_REPO, notifyRepo);
        NotificationService notifyService = new DefaultNotificationService(notifyRepo, userService);
        ctx.setAttribute(WebConst.Ctx.NOTIFY_SERVICE, notifyService);
        FriendRepository friendRepo = new HFriendRepository();
        ctx.setAttribute(WebConst.Ctx.FRIEND_REPOSITORY, friendRepo);
        FriendService friendService = new DefaultFriendService(friendRepo, userService, notifyService);
        ctx.setAttribute(WebConst.Ctx.FRIEND_SERVICE, friendService);
        com.javarush.apalinskiy.service.user.UserStatsService userStats = new HUserStatsService();
        ctx.setAttribute(WebConst.Ctx.USER_STATS_SERVICE, userStats);
        log.debug("Registered: SAVE_STATE_SERVICE, NOTIFY_REPO, NOTIFY_SERVICE, FRIEND_REPOSITORY, FRIEND_SERVICE, USER_STATS_SERVICE");
        try {
            Uploads.resolveBaseDir(ctx);
            log.info("Uploads base dir resolved");
        } catch (RuntimeException e) {
            log.error("Uploads base dir resolve failed", e);
            throw e;
        }
        log.info("App context init: done");
    }

    /**
     * Called when the web application context is about to be destroyed.
     * <p>Ensures that Hibernate is properly shut down and all allocated
     * resources are released before the application stops.</p>
     *
     * @param sce the {@link ServletContextEvent} associated with this shutdown
     */
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log.info("App context destroy: shutting down Hibernate");
        HibernateUtil.shutdown();
        log.info("App context is being destroyed");
    }
}
