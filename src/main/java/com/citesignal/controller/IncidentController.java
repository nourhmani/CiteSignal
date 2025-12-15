package com.citesignal.controller;

import com.citesignal.dto.CreateIncidentRequest;
import com.citesignal.dto.IncidentSearchRequest;
import com.citesignal.dto.UpdateIncidentRequest;
import com.citesignal.model.Incident;
import com.citesignal.model.User;
import com.citesignal.repository.QuartierRepository;
import com.citesignal.repository.DepartementRepository;
import com.citesignal.repository.UserRepository;
import com.citesignal.security.UserPrincipal;
import com.citesignal.service.IncidentService;
import com.citesignal.service.NotificationService;
import com.citesignal.service.StatisticsService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Controller
@RequestMapping("/incidents")
public class IncidentController {
    
    @Autowired
    private IncidentService incidentService;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private StatisticsService statisticsService;
    
    @Autowired
    private QuartierRepository quartierRepository;
    
    @Autowired
    private DepartementRepository departementRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @GetMapping("/create")
    @PreAuthorize("hasRole('CITOYEN')")
    public String showCreateForm(Model model, Authentication authentication) {
        try {
            model.addAttribute("incident", new CreateIncidentRequest());
            model.addAttribute("categories", Incident.Categorie.values());
            model.addAttribute("priorites", Incident.Priorite.values());
            model.addAttribute("quartiers", quartierRepository.findAll());
        } catch (Exception e) {
            model.addAttribute("quartiers", java.util.Collections.emptyList());
        }
        return "incident/create";
    }
    
    @PostMapping("/create")
    @PreAuthorize("hasRole('CITOYEN')")
    public String createIncident(@Valid @ModelAttribute("incident") CreateIncidentRequest request,
                                BindingResult result,
                                Model model,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            try {
                model.addAttribute("categories", Incident.Categorie.values());
                model.addAttribute("priorites", Incident.Priorite.values());
                model.addAttribute("quartiers", quartierRepository.findAll());
            } catch (Exception e) {
                model.addAttribute("quartiers", java.util.Collections.emptyList());
            }
            return "incident/create";
        }
        
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        try {
            Incident incident = incidentService.createIncident(request, userPrincipal.getId());
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Votre signalement a été créé avec succès !");
            return "redirect:/incidents/" + incident.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "Erreur lors de la création du signalement: " + e.getMessage());
            return "redirect:/incidents/create";
        }
    }
    
    @GetMapping("/{id}")
    public String viewIncident(@PathVariable Long id, Model model, Authentication authentication) {
        try {
            // Vérifier que l'ID est valide
            if (id == null || id <= 0) {
                return "redirect:/incidents";
            }
            
            Incident incident = incidentService.getIncidentById(id);
            
            // Vérifier que l'incident existe
            if (incident == null) {
                return "redirect:/incidents";
            }
            
            // Vérifier les permissions
            boolean canEdit = false;
            if (authentication != null && authentication.isAuthenticated()) {
                try {
                    UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
                    if (userPrincipal != null) {
                        User user = userRepository.findById(userPrincipal.getId()).orElse(null);
                        if (user != null) {
                            canEdit = user.hasRole(User.RoleName.ADMINISTRATEUR) || 
                                     user.hasRole(User.RoleName.SUPERADMIN) ||
                                     (user.hasRole(User.RoleName.AGENT_MUNICIPAL) && 
                                      incident.getAgent() != null && 
                                      incident.getAgent().getId().equals(user.getId())) ||
                                     (user.hasRole(User.RoleName.CITOYEN) && 
                                      incident.getCitoyen() != null &&
                                      incident.getCitoyen().getId().equals(user.getId()));
                        }
                    }
                } catch (Exception e) {
                    // Ignorer les erreurs d'authentification
                }
            }
            
            model.addAttribute("incident", incident);
            model.addAttribute("canEdit", canEdit);
            model.addAttribute("statuts", Incident.Statut.values());
            model.addAttribute("priorites", Incident.Priorite.values());
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("introuvable")) {
                model.addAttribute("errorMessage", "Incident introuvable");
            } else {
                model.addAttribute("errorMessage", "Erreur lors du chargement de l'incident: " + e.getMessage());
            }
            return "redirect:/incidents";
        } catch (Exception e) {
            // Logger l'erreur pour le débogage
            org.slf4j.LoggerFactory.getLogger(IncidentController.class)
                .error("Erreur lors de l'affichage de l'incident " + id, e);
            model.addAttribute("errorMessage", "Erreur lors du chargement de l'incident. Veuillez réessayer.");
            return "redirect:/incidents";
        }
        
        return "incident/view";
    }
    
    @GetMapping
    public String listIncidents(
            @RequestParam(required = false) Incident.Statut statut,
            @RequestParam(required = false) Incident.Categorie categorie,
            @RequestParam(required = false) Long quartierId,
            @RequestParam(required = false) Long departementId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(required = false) String recherche,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            Model model,
            Authentication authentication) {
        
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(userPrincipal.getId()).orElse(null);
        
        Pageable pageable = PageRequest.of(page, size, 
                Sort.by(sortDir.equals("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy));
        
        Page<Incident> incidents;
        
        // Filtrer selon le rôle
        if (user != null && user.hasRole(User.RoleName.CITOYEN)) {
            incidents = incidentService.getIncidentsByCitoyen(user.getId(), pageable);
        } else if (user != null && user.hasRole(User.RoleName.AGENT_MUNICIPAL)) {
            incidents = incidentService.getIncidentsByAgent(user.getId(), pageable);
        } else {
            // Recherche avancée pour admins
            LocalDateTime dateDebutDateTime = dateDebut != null ? 
                    LocalDateTime.of(dateDebut, LocalTime.MIN) : null;
            LocalDateTime dateFinDateTime = dateFin != null ? 
                    LocalDateTime.of(dateFin, LocalTime.MAX) : null;
            
            incidents = incidentService.searchIncidents(
                    statut, categorie, quartierId, departementId,
                    dateDebutDateTime, dateFinDateTime, recherche, pageable
            );
        }
        
        model.addAttribute("incidents", incidents);
        model.addAttribute("statuts", Incident.Statut.values());
        model.addAttribute("categories", Incident.Categorie.values());
        try {
            model.addAttribute("quartiers", quartierRepository.findAll());
            model.addAttribute("departements", departementRepository.findAll());
        } catch (Exception e) {
            model.addAttribute("quartiers", java.util.Collections.emptyList());
            model.addAttribute("departements", java.util.Collections.emptyList());
        }
        model.addAttribute("currentStatut", statut);
        model.addAttribute("currentCategorie", categorie);
        model.addAttribute("currentQuartierId", quartierId);
        model.addAttribute("currentDepartementId", departementId);
        model.addAttribute("currentDateDebut", dateDebut);
        model.addAttribute("currentDateFin", dateFin);
        model.addAttribute("currentRecherche", recherche);
        
        return "incident/list";
    }
    
    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('AGENT_MUNICIPAL', 'ADMINISTRATEUR', 'SUPERADMIN')")
    public String showEditForm(@PathVariable Long id, Model model, Authentication authentication) {
        try {
            Incident incident = incidentService.getIncidentById(id);
            model.addAttribute("incident", incident);
            model.addAttribute("updateRequest", new UpdateIncidentRequest());
            model.addAttribute("categories", Incident.Categorie.values());
            model.addAttribute("statuts", Incident.Statut.values());
            model.addAttribute("priorites", Incident.Priorite.values());
            model.addAttribute("quartiers", quartierRepository.findAll());
            model.addAttribute("departements", departementRepository.findAll());
            model.addAttribute("agents", userRepository.findByRole(User.RoleName.AGENT_MUNICIPAL));
        } catch (Exception e) {
            model.addAttribute("quartiers", java.util.Collections.emptyList());
            model.addAttribute("departements", java.util.Collections.emptyList());
            model.addAttribute("agents", java.util.Collections.emptyList());
        }
        return "incident/edit";
    }
    
    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('AGENT_MUNICIPAL', 'ADMINISTRATEUR', 'SUPERADMIN')")
    public String updateIncident(@PathVariable Long id,
                                @Valid @ModelAttribute("updateRequest") UpdateIncidentRequest request,
                                BindingResult result,
                                Model model,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            try {
                Incident incident = incidentService.getIncidentById(id);
                model.addAttribute("incident", incident);
                model.addAttribute("categories", Incident.Categorie.values());
                model.addAttribute("statuts", Incident.Statut.values());
                model.addAttribute("priorites", Incident.Priorite.values());
                model.addAttribute("quartiers", quartierRepository.findAll());
                model.addAttribute("departements", departementRepository.findAll());
                model.addAttribute("agents", userRepository.findByRole(User.RoleName.AGENT_MUNICIPAL));
            } catch (Exception e) {
                model.addAttribute("quartiers", java.util.Collections.emptyList());
                model.addAttribute("departements", java.util.Collections.emptyList());
                model.addAttribute("agents", java.util.Collections.emptyList());
            }
            return "incident/edit";
        }
        
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        try {
            incidentService.updateIncident(id, request, userPrincipal.getId());
            redirectAttributes.addFlashAttribute("successMessage", 
                    "L'incident a été mis à jour avec succès !");
            return "redirect:/incidents/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "Erreur lors de la mise à jour: " + e.getMessage());
            return "redirect:/incidents/" + id + "/edit";
        }
    }
    
    @PostMapping("/{id}/close")
    @PreAuthorize("hasRole('CITOYEN')")
    public String closeIncident(@PathVariable Long id,
                               @RequestParam(required = false) String feedbackCitoyen,
                               @RequestParam(required = false) Integer noteSatisfaction,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            incidentService.closeIncident(id, feedbackCitoyen, noteSatisfaction);
            redirectAttributes.addFlashAttribute("successMessage", 
                    "L'incident a été clôturé avec succès !");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "Erreur lors de la clôture: " + e.getMessage());
        }
        return "redirect:/incidents/" + id;
    }
}

