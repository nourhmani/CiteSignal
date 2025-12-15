package com.citesignal.service;

import com.citesignal.model.Incident;
import com.citesignal.repository.IncidentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatisticsService {
    
    @Autowired
    private IncidentRepository incidentRepository;
    
    public Map<String, Object> getGeneralStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Total des incidents
        long totalIncidents = incidentRepository.count();
        stats.put("totalIncidents", totalIncidents);
        
        // Incidents par statut
        Map<String, Long> incidentsByStatus = new HashMap<>();
        for (Incident.Statut statut : Incident.Statut.values()) {
            Long count = incidentRepository.countByStatut(statut);
            incidentsByStatus.put(statut.name(), count != null ? count : 0L);
        }
        stats.put("incidentsByStatus", incidentsByStatus);
        
        // Incidents par catégorie
        Map<String, Long> incidentsByCategory = new HashMap<>();
        for (Incident.Categorie categorie : Incident.Categorie.values()) {
            Long count = incidentRepository.countByCategorie(categorie);
            incidentsByCategory.put(categorie.name(), count != null ? count : 0L);
        }
        stats.put("incidentsByCategory", incidentsByCategory);
        
        // Incidents des 30 derniers jours
        LocalDateTime date30DaysAgo = LocalDateTime.now().minusDays(30);
        List<Incident> recentIncidents = incidentRepository.findByDateRange(
                date30DaysAgo, LocalDateTime.now()
        );
        stats.put("recentIncidents", recentIncidents.size());
        
        // Incidents résolus dans les 30 derniers jours
        long resolvedLast30Days = recentIncidents.stream()
                .filter(i -> i.getStatut() == Incident.Statut.RESOLU || 
                            i.getStatut() == Incident.Statut.CLOTURE)
                .count();
        stats.put("resolvedLast30Days", resolvedLast30Days);
        
        // Taux de résolution
        double resolutionRate = 0;
        if (recentIncidents.size() > 0) {
            resolutionRate = (double) resolvedLast30Days / recentIncidents.size() * 100;
        }
        stats.put("resolutionRate", Math.round(resolutionRate * 100.0) / 100.0);
        
        return stats;
    }
    
    public Map<String, Long> getIncidentsByQuartier() {
        // Cette méthode nécessiterait une requête personnalisée
        // Pour l'instant, retournons une structure vide
        return new HashMap<>();
    }
    
    public Map<String, Long> getIncidentsByDepartement() {
        // Cette méthode nécessiterait une requête personnalisée
        // Pour l'instant, retournons une structure vide
        return new HashMap<>();
    }
}

