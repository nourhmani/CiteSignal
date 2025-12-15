package com.citesignal.repository;

import com.citesignal.model.Notification;
import com.citesignal.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUser(User user);
    Page<Notification> findByUser(User user, Pageable pageable);
    List<Notification> findByUserAndLu(User user, Boolean lu);
    Long countByUserAndLu(User user, Boolean lu);
}

