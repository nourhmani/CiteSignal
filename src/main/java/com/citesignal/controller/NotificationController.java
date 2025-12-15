package com.citesignal.controller;

import com.citesignal.model.Notification;
import com.citesignal.security.UserPrincipal;
import com.citesignal.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/notifications")
public class NotificationController {
    
    @Autowired
    private NotificationService notificationService;
    
    @GetMapping
    public String listNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model,
            Authentication authentication) {
        
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notifications = notificationService.getNotificationsByUser(
                userPrincipal.getId(), pageable);
        
        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", notificationService.countUnreadNotifications(userPrincipal.getId()));
        
        return "notification/list";
    }
    
    @PostMapping("/{id}/read")
    public String markAsRead(@PathVariable Long id,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        try {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            notificationService.markAsRead(id, userPrincipal.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Notification marquée comme lue");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur: " + e.getMessage());
        }
        return "redirect:/notifications";
    }
    
    @PostMapping("/read-all")
    public String markAllAsRead(Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            notificationService.markAllAsRead(userPrincipal.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Toutes les notifications ont été marquées comme lues");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur: " + e.getMessage());
        }
        return "redirect:/notifications";
    }
}

