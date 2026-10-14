package com.helpdesk.service;

import com.helpdesk.model.NotificationRecord;
import com.helpdesk.model.Organization;
import com.helpdesk.model.User;
import com.helpdesk.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public NotificationRecord createNotification(User user, Organization org, String title, String message, String linkUrl) {
        NotificationRecord record = new NotificationRecord(user, org, title, message, linkUrl);
        return notificationRepository.save(record);
    }

    @Transactional(readOnly = true)
    public List<NotificationRecord> getRecentNotifications(Long userId) {
        return notificationRepository.findTop15ByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAllRead(Long userId) {
        var unread = notificationRepository.findTop15ByUserIdOrderByCreatedAtDesc(userId);
        for (var n : unread) {
            n.setRead(true);
        }
        notificationRepository.saveAll(unread);
    }
}
