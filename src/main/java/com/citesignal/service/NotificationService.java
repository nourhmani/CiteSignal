package com.citesignal.service;

import com.citesignal.model.Notification;
import com.citesignal.model.User;
import com.citesignal.repository.NotificationRepository;
import com.citesignal.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    public Page<Notification> getNotificationsByUser(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        return notificationRepository.findByUser(user, pageable);
    }
    
    public List<Notification> getUnreadNotificationsByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        return notificationRepository.findByUserAndLu(user, false);
    }
    
    public Long countUnreadNotifications(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        return notificationRepository.countByUserAndLu(user, false);
    }
    
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification introuvable"));
        
        if (!notification.getUser().getId().equals(userId)) {
            throw new RuntimeException("Vous n'êtes pas autorisé à modifier cette notification");
        }
        
        notification.setLu(true);
        notificationRepository.save(notification);
    }
    
    @Transactional
    public void markAllAsRead(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        List<Notification> unreadNotifications = notificationRepository.findByUserAndLu(user, false);
        unreadNotifications.forEach(n -> n.setLu(true));
        notificationRepository.saveAll(unreadNotifications);
    }
}

