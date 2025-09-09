package com.javarush.apalinskiy.mail;

import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.service.UserService;

import java.util.Map;

public class DefaultNotificationService implements NotificationService {

    private final NotificationRepository repo;
    private final UserService users;

    public DefaultNotificationService(NotificationRepository repo, UserService users) {
        this.repo = repo;
        this.users = users;
    }

    @Override
    public void add(String userId, NotificationType type, String title, String body) {
        repo.save(Notification.of(userId, type, title, body));
    }

    @Override
    public void notify(NotificationEvent e) {
        String actorName = (e.actorUserId() == null)
                ? "System"
                : users.findById(e.actorUserId()).map(User::getUserName).orElse("User");
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
