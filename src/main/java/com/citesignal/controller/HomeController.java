package com.citesignal.controller;

import com.citesignal.model.RoleName;
import com.citesignal.model.StatutIncident;
import com.citesignal.model.User;
import com.citesignal.security.UserPrincipal;
import com.citesignal.service.UserService;
import com.citesignal.service.IncidentService;
import com.citesignal.service.NotificationService;
import com.citesignal.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;

@Controller
public class HomeController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private IncidentService incidentService;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private StatisticsService statisticsService;
    
    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }
    
    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/auth/login";
        }
        
        try {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            User user = userService.getUserById(userPrincipal.getId());
            model.addAttribute("user", user);
            
            // Ajouter les notifications non lues
            try {
                Long unreadCount = notificationService.countUnreadNotifications(userPrincipal.getId());
                model.addAttribute("unreadNotifications", unreadCount);
            } catch (Exception e) {
                model.addAttribute("unreadNotifications", 0L);
            }
            
            // Rediriger vers le dashboard approprié selon le rôle
            boolean isSuperAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_SUPERADMIN"));
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRATEUR"));
            boolean isAgent = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_AGENT_MUNICIPAL"));
            
            if (isSuperAdmin || isAdmin) {
                // Ajouter les statistiques pour les admins
                try {
                    model.addAttribute("statistics", statisticsService.getGeneralStatistics());
                    // Derniers incidents
                    model.addAttribute("recentIncidents",
                            incidentService.searchIncidents(null, null, null, null, null, null, null,
                                    PageRequest.of(0, 10)).getContent());

                    // Statistiques utilisateurs pour superadmin
                    if (isSuperAdmin) {
                        long totalUsers = userService.getAllUsers().size();
                        long adminCount = userService.getUsersByRole(RoleName.ADMINISTRATEUR).size();
                        long agentCount = userService.getUsersByRole(RoleName.AGENT_MUNICIPAL).size();
                        long citizenCount = userService.getUsersByRole(RoleName.CITOYEN).size();

                        model.addAttribute("totalUsers", totalUsers);
                        model.addAttribute("adminCount", adminCount);
                        model.addAttribute("agentCount", agentCount);
                        model.addAttribute("citizenCount", citizenCount);
                    }
                } catch (Exception e) {
                    model.addAttribute("statistics", null);
                    model.addAttribute("recentIncidents", Collections.emptyList());
                    if (isSuperAdmin) {
                        model.addAttribute("totalUsers", 0L);
                        model.addAttribute("adminCount", 0L);
                        model.addAttribute("agentCount", 0L);
                        model.addAttribute("citizenCount", 0L);
                    }
                }
                return isSuperAdmin ? "dashboard-superadmin" : "dashboard-admin";
            } else if (isAgent) {
                // Incidents assignés à l'agent et statistiques
                try {
                    List<com.citesignal.model.Incident> agentIncidents = incidentService.getIncidentsByAgent(userPrincipal.getId(), PageRequest.of(0, 1000)).getContent();
                    model.addAttribute("myIncidents", agentIncidents.subList(0, Math.min(10, agentIncidents.size())));

                    // Calculer les statistiques pour l'agent
                    long pendingIncidents = agentIncidents.stream()
                            .filter(i -> i.getStatut() == StatutIncident.SIGNALE)
                            .count();
                    long inProgressIncidents = agentIncidents.stream()
                            .filter(i -> i.getStatut() == StatutIncident.PRIS_EN_CHARGE ||
                                        i.getStatut() == StatutIncident.EN_RESOLUTION)
                            .count();
                    long resolvedIncidents = agentIncidents.stream()
                            .filter(i -> i.getStatut() == StatutIncident.RESOLU ||
                                        i.getStatut() == StatutIncident.CLOTURE)
                            .count();
                    long todayIncidents = agentIncidents.stream()
                            .filter(i -> i.getCreatedAt().toLocalDate().equals(java.time.LocalDate.now()))
                            .count();

                    model.addAttribute("pendingIncidents", pendingIncidents);
                    model.addAttribute("inProgressIncidents", inProgressIncidents);
                    model.addAttribute("resolvedIncidents", resolvedIncidents);
                    model.addAttribute("todayIncidents", todayIncidents);
                } catch (Exception e) {
                    model.addAttribute("myIncidents", Collections.emptyList());
                    model.addAttribute("pendingIncidents", 0L);
                    model.addAttribute("inProgressIncidents", 0L);
                    model.addAttribute("resolvedIncidents", 0L);
                    model.addAttribute("todayIncidents", 0L);
                }
                return "dashboard-agent";
            } else {
                // Incidents du citoyen
                try {
                    List<com.citesignal.model.Incident> allMyIncidents = 
                            incidentService.getIncidentsByCitoyen(userPrincipal.getId(), 
                                    PageRequest.of(0, 100)).getContent();
                    model.addAttribute("myIncidents", allMyIncidents);
                    
                    // Calculer les statistiques
                    long totalIncidents = allMyIncidents.size();
                    long enAttente = allMyIncidents.stream()
                            .filter(i -> i.getStatut() == StatutIncident.SIGNALE)
                            .count();
                    long enCours = allMyIncidents.stream()
                            .filter(i -> i.getStatut() == StatutIncident.PRIS_EN_CHARGE ||
                                        i.getStatut() == StatutIncident.EN_RESOLUTION)
                            .count();
                    long resolus = allMyIncidents.stream()
                            .filter(i -> i.getStatut() == StatutIncident.RESOLU ||
                                        i.getStatut() == StatutIncident.CLOTURE)
                            .count();
                    
                    model.addAttribute("totalIncidents", totalIncidents);
                    model.addAttribute("enAttente", enAttente);
                    model.addAttribute("enCours", enCours);
                    model.addAttribute("resolus", resolus);
                } catch (Exception e) {
                    model.addAttribute("myIncidents", Collections.emptyList());
                    model.addAttribute("totalIncidents", 0L);
                    model.addAttribute("enAttente", 0L);
                    model.addAttribute("enCours", 0L);
                    model.addAttribute("resolus", 0L);
                }
                return "dashboard-citizen";
            }
        } catch (Exception e) {
            // En cas d'erreur, rediriger vers la page de login
            return "redirect:/auth/login?error=true";
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

