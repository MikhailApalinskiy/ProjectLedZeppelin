package com.javarush.apalinskiy.mail;

public interface NotificationService {

    void add(String userId, NotificationType type, String title, String body);

    void notify(NotificationEvent ev);
}
