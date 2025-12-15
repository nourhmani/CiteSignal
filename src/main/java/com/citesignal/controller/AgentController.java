package com.citesignal.controller;

import com.citesignal.model.Incident;
import com.citesignal.security.UserPrincipal;
import com.citesignal.service.IncidentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/agent")
@PreAuthorize("hasAnyRole('AGENT_MUNICIPAL', 'ADMINISTRATEUR', 'SUPERADMIN')")
public class AgentController {
    
    @Autowired
    private IncidentService incidentService;
    
    @GetMapping("/incidents")
    public String listIncidents(
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        
        if (userPrincipal == null) {
            return "redirect:/auth/login";
        }
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Incident> incidents;
            
            // Filtrer selon le statut si fourni
            if (status != null && !status.isEmpty()) {
                Incident.Statut statut = null;
                try {
                    statut = Incident.Statut.valueOf(status.toUpperCase());
                } catch (IllegalArgumentException e) {
                    // Statut invalide, ignorer
                }
                if (statut != null) {
                    incidents = incidentService.searchIncidents(
                            statut, null, null, null, null, null, null, pageable);
                } else {
                    incidents = incidentService.getIncidentsByAgent(userPrincipal.getId(), pageable);
                }
            } else {
                incidents = incidentService.getIncidentsByAgent(userPrincipal.getId(), pageable);
            }
            
            model.addAttribute("incidents", incidents);
            model.addAttribute("statuts", Incident.Statut.values());
            model.addAttribute("currentStatus", status);
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Erreur lors du chargement des incidents: " + e.getMessage());
            model.addAttribute("incidents", org.springframework.data.domain.Page.empty());
        }
        
        return "agent/incidents";
    }
}

