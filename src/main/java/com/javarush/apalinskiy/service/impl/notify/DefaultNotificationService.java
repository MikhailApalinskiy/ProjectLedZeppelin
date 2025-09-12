package com.javarush.apalinskiy.service.impl.notify;

import com.javarush.apalinskiy.domain.notify.Notification;
import com.javarush.apalinskiy.domain.notify.NotificationEvent;
import com.javarush.apalinskiy.repository.notify.NotificationRepository;
import com.javarush.apalinskiy.domain.notify.NotificationType;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.service.user.UserService;
import com.javarush.apalinskiy.service.notify.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class DefaultNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(DefaultNotificationService.class);

    private final NotificationRepository repo;
    private final UserService users;

    public DefaultNotificationService(NotificationRepository repo, UserService users) {
        this.repo = repo;
        this.users = users;
    }

    @Override
    public void add(String userId, NotificationType type, String title, String body) {
        log.info("Notify.add type={} targetUserId={} title='{}'", type, userId, title);
        try {
            repo.save(Notification.of(userId, type, title, body));
        } catch (RuntimeException e) {
            log.error("Notify.add failed type={} targetUserId={} title='{}'", type, userId, title, e);
            throw e;
        }
    }

    @Override
    public void notify(NotificationEvent e) {
        log.info("Notify.event type={} targetUserId={} actorUserId={}", e.type(), e.targetUserId(), e.actorUserId());
        String actorName;
        if (e.actorUserId() == null) {
            actorName = "System";
        } else {
            actorName = users.findById(e.actorUserId())
                    .map(User::getUserName)
                    .orElseGet(() -> {
                        log.warn("Notify.event actor not found actorUserId={}", e.actorUserId());
                        return "User";
                    });
        }
        switch (e.type()) {
            case FRIEND_REQUEST -> add(e.targetUserId(), e.type(),
                    "New friend request",
                    "User <b>" + actorName + "</b> wants to add you as a friend.");
            case FRIEND_ACCEPTED -> add(e.targetUserId(), e.type(),
                    "The application has been accepted",
                    "<b>" + actorName + "</b> accepted your request — now you're friends.");
            case FRIEND_PUBLISHED_QUEST -> {
                String name = safe(e.data(), "questName", "A new quest");
                add(e.targetUserId(), e.type(),
                        "A friend posted a quest",
                        "<b>" + actorName + "</b> published a quest: <b>" + name + "</b>.");
            }
            case QUEST_MODERATED -> {
                String q = safe(e.data(), "questName", "Quest");
                String res = safe(e.data(), "result", "updated");
                add(e.targetUserId(), e.type(),
                        "Moderation of the quest",
                        "Quest <b>" + q + "</b> passed moderation: <b>" + res + "</b>.");
            }
            case QUEST_ADMIN_CHANGED -> {
                String q = safe(e.data(), "questName", "Quest");
                String what = safe(e.data(), "what", "The parameters have been updated");
                add(e.targetUserId(), e.type(),
                        "The admin changed your quest.",
                        "Changes in <b>" + q + "</b>: " + what + ".");
            }
            case USER_ADMIN_CHANGED -> {
                String what = safe(e.data(), "what", "The parameters have been updated\n");
                add(e.targetUserId(), e.type(),
                        "The admin changed the profile",
                        "The administrator has updated your profile settings: " + what + ".");
            }
            case FRIEND_REMOVED -> add(e.targetUserId(), e.type(),
                    "Friend removed",
                    "User <b>" + actorName + "</b> removed you from friends.");
        }
    }

    private static String safe(Map<String, String> m, String k, String def) {
        return (m == null) ? def : m.getOrDefault(k, def);
    }
}
