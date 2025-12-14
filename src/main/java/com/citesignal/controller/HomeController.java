package com.citesignal.controller;

import com.citesignal.model.User;
import com.citesignal.security.UserPrincipal;
import com.citesignal.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HomeController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }
    
    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/auth/login";
        }
        
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userService.getUserById(userPrincipal.getId());
        model.addAttribute("user", user);
        
        // Rediriger vers le dashboard approprié selon le rôle
        boolean isSuperAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPERADMIN"));
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRATEUR"));
        boolean isAgent = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_AGENT_MUNICIPAL"));
        
        if (isSuperAdmin) {
            return "dashboard-superadmin";
        } else if (isAdmin) {
            return "dashboard-admin";
        } else if (isAgent) {
            return "dashboard-agent";
        } else {
            return "dashboard-citizen";
        }
    }
    
    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam String token, RedirectAttributes redirectAttributes) {
        try {
            userService.verifyEmail(token);
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Votre email a été vérifié avec succès ! Vous pouvez maintenant vous connecter.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "Lien de vérification invalide ou expiré.");
        }
        return "redirect:/auth/login";
    }
}

