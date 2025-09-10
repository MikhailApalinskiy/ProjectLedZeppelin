package com.javarush.apalinskiy.service.notify;

import com.javarush.apalinskiy.domain.notify.NotificationEvent;
import com.javarush.apalinskiy.domain.notify.NotificationType;

public interface NotificationService {

    void add(String userId, NotificationType type, String title, String body);

    void notify(NotificationEvent ev);
}
