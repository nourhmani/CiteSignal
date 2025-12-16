package com.citesignal.service;

import com.citesignal.dto.CreateIncidentRequest;
import com.citesignal.dto.UpdateIncidentRequest;
import com.citesignal.model.*;
import com.citesignal.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class IncidentService {
    
    private static final Logger logger = LoggerFactory.getLogger(IncidentService.class);
    
    @Autowired
    private IncidentRepository incidentRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private QuartierRepository quartierRepository;
    
    @Autowired
    private DepartementRepository departementRepository;
    
    @Autowired
    private PhotoRepository photoRepository;
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private FileStorageService fileStorageService;
    
    @Autowired
    private EmailService emailService;
    
    @Transactional
    public Incident createIncident(CreateIncidentRequest request, Long citoyenId) {
        User citoyen = userRepository.findById(citoyenId)
                .orElseThrow(() -> new RuntimeException("Citoyen introuvable"));
        
        Incident incident = new Incident();
        incident.setTitre(request.getTitre());
        incident.setDescription(request.getDescription());
        incident.setCategorie(request.getCategorie());
        incident.setAdresse(request.getAdresse());
        incident.setLatitude(request.getLatitude());
        incident.setLongitude(request.getLongitude());
        incident.setPriorite(request.getPriorite());
        incident.setStatut(StatutIncident.SIGNALE);
        incident.setCitoyen(citoyen);
        
        // Associer le quartier si fourni
        if (request.getQuartierId() != null) {
            Quartier quartier = quartierRepository.findById(request.getQuartierId())
                    .orElse(null);
            incident.setQuartier(quartier);
        }
        
        Incident savedIncident = incidentRepository.save(incident);
        
        // Gérer les photos
        if (request.getPhotos() != null && !request.getPhotos().isEmpty()) {
            List<Photo> photos = new ArrayList<>();
            for (MultipartFile file : request.getPhotos()) {
                if (file != null && !file.isEmpty()) {
                    try {
                        String filename = fileStorageService.storeFile(file);
                        Photo photo = new Photo();
                        photo.setNomFichier(file.getOriginalFilename());
                        photo.setChemin(fileStorageService.getFileUrl(filename));
                        photo.setTypeMime(file.getContentType());
                        photo.setTaille(file.getSize());
                        photo.setIncident(savedIncident);
                        photos.add(photo);
                    } catch (Exception e) {
                        logger.error("Erreur lors de l'upload de la photo", e);
                    }
                }
            }
            photoRepository.saveAll(photos);
        }
        
        // Créer une notification pour le citoyen
        createNotification(
                citoyen,
                savedIncident,
                "Signalement créé",
                "Votre signalement '" + savedIncident.getTitre() + "' a été créé avec succès.",
                Notification.TypeNotification.INCIDENT_CREE
        );
        
        logger.info("Incident créé: {} par le citoyen {}", savedIncident.getId(), citoyenId);
        return savedIncident;
    }
    
    @Transactional
    @PreAuthorize("hasAnyRole('AGENT_MUNICIPAL', 'ADMINISTRATEUR', 'SUPERADMIN')")
    public Incident updateIncident(Long incidentId, UpdateIncidentRequest request, Long userId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new RuntimeException("Incident introuvable"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        
        // Sauvegarder l'ancien statut pour les notifications
        StatutIncident oldStatut = incident.getStatut();
        
        // Vérifier les permissions
        if (user.hasRole(RoleName.AGENT_MUNICIPAL) && 
            incident.getAgent() != null && 
            !incident.getAgent().getId().equals(userId)) {
            throw new RuntimeException("Vous n'êtes pas autorisé à modifier cet incident");
        }
        
        // Mettre à jour les champs - restrictions selon le rôle
        boolean isAgent = user.hasRole(RoleName.AGENT_MUNICIPAL);
        boolean isAdmin = user.hasRole(RoleName.ADMINISTRATEUR) || user.hasRole(RoleName.SUPERADMIN);
        
        // Seuls les admins peuvent modifier ces champs
        if (!isAgent) {
            if (request.getTitre() != null) {
                incident.setTitre(request.getTitre());
            }
            if (request.getDescription() != null) {
                incident.setDescription(request.getDescription());
            }
            if (request.getCategorie() != null) {
                incident.setCategorie(request.getCategorie());
            }
            if (request.getPriorite() != null) {
                incident.setPriorite(request.getPriorite());
            }
            if (request.getAdresse() != null) {
                incident.setAdresse(request.getAdresse());
            }
            if (request.getLatitude() != null) {
                incident.setLatitude(request.getLatitude());
            }
            if (request.getLongitude() != null) {
                incident.setLongitude(request.getLongitude());
            }
            if (request.getQuartierId() != null) {
                Quartier quartier = quartierRepository.findById(request.getQuartierId())
                        .orElse(null);
                incident.setQuartier(quartier);
            }
            
            // Assigner un département
            if (request.getDepartementId() != null) {
                Departement departement = departementRepository.findById(request.getDepartementId())
                        .orElseThrow(() -> new RuntimeException("Département introuvable"));
                incident.setDepartement(departement);
            }
            
            // Assigner un agent
            User oldAgent = incident.getAgent();
            if (request.getAgentId() != null) {
                User agent = userRepository.findById(request.getAgentId())
                        .orElseThrow(() -> new RuntimeException("Agent introuvable"));
                incident.setAgent(agent);
                
                // Si un agent est assigné pour la première fois ou si l'agent change, passer le statut à PRIS_EN_CHARGE
                if (oldAgent == null || !oldAgent.getId().equals(agent.getId())) {
                    if (incident.getStatut() == StatutIncident.SIGNALE) {
                        updateIncidentStatus(incident, StatutIncident.PRIS_EN_CHARGE, null);
                    }
                    
                    // Notifier l'agent de l'assignation
                    notifyAgentOfAssignment(incident, agent);
                }
            }
        }
        
        // Gérer le workflow des statuts (accessible à tous les rôles)
        if (request.getStatut() != null && request.getStatut() != oldStatut) {
            updateIncidentStatus(incident, request.getStatut(), request.getCommentaireResolution());
        }
        
        // Ajouter de nouvelles photos
        if (request.getPhotos() != null && !request.getPhotos().isEmpty()) {
            List<Photo> photos = new ArrayList<>();
            for (MultipartFile file : request.getPhotos()) {
                if (file != null && !file.isEmpty()) {
                    try {
                        String filename = fileStorageService.storeFile(file);
                        Photo photo = new Photo();
                        photo.setNomFichier(file.getOriginalFilename());
                        photo.setChemin(fileStorageService.getFileUrl(filename));
                        photo.setTypeMime(file.getContentType());
                        photo.setTaille(file.getSize());
                        photo.setIncident(incident);
                        photos.add(photo);
                    } catch (Exception e) {
                        logger.error("Erreur lors de l'upload de la photo", e);
                    }
                }
            }
            photoRepository.saveAll(photos);
        }
        
        Incident updatedIncident = incidentRepository.save(incident);
        
        // Notifier le citoyen si le statut a changé
        if (oldStatut != updatedIncident.getStatut()) {
            notifyCitizenOfStatusChange(updatedIncident, oldStatut);
        }
        
        logger.info("Incident mis à jour: {} par l'utilisateur {}", incidentId, userId);
        return updatedIncident;
    }
    
    @Transactional
    public void updateIncidentStatus(Incident incident, StatutIncident newStatut, String commentaire) {
        StatutIncident oldStatut = incident.getStatut();
        
        // Validation des transitions de statut
        if (!isValidStatusTransition(oldStatut, newStatut)) {
            throw new RuntimeException("Transition de statut invalide: " + oldStatut + " -> " + newStatut);
        }
        
        incident.setStatut(newStatut);
        
        // Actions spécifiques selon le nouveau statut
        switch (newStatut) {
            case PRIS_EN_CHARGE:
                // L'incident est pris en charge par un agent
                break;
            case EN_RESOLUTION:
                // L'intervention est en cours
                break;
            case RESOLU:
                incident.setDateResolution(LocalDateTime.now());
                incident.setCommentaireResolution(commentaire);
                break;
            case CLOTURE:
                // L'incident est clôturé
                break;
        }
    }
    
    private boolean isValidStatusTransition(StatutIncident oldStatut, StatutIncident newStatut) {
        // Définir les transitions valides
        return switch (oldStatut) {
            case SIGNALE -> newStatut == StatutIncident.PRIS_EN_CHARGE;
            case PRIS_EN_CHARGE -> newStatut == StatutIncident.EN_RESOLUTION || 
                                   newStatut == StatutIncident.SIGNALE;
            case EN_RESOLUTION -> newStatut == StatutIncident.RESOLU || 
                                  newStatut == StatutIncident.PRIS_EN_CHARGE;
            case RESOLU -> newStatut == StatutIncident.CLOTURE || 
                           newStatut == StatutIncident.EN_RESOLUTION;
            case CLOTURE -> false; // Une fois clôturé, on ne peut plus changer
        };
    }
    
    @Transactional
    public void closeIncident(Long incidentId, String feedbackCitoyen, Integer noteSatisfaction) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new RuntimeException("Incident introuvable"));
        
        if (incident.getStatut() != StatutIncident.RESOLU) {
            throw new RuntimeException("Seuls les incidents résolus peuvent être clôturés");
        }
        
        incident.setStatut(StatutIncident.CLOTURE);
        incident.setFeedbackCitoyen(feedbackCitoyen);
        incident.setNoteSatisfaction(noteSatisfaction);
        
        incidentRepository.save(incident);
        
        // Notifier l'agent
        if (incident.getAgent() != null) {
            createNotification(
                    incident.getAgent(),
                    incident,
                    "Incident clôturé",
                    "L'incident '" + incident.getTitre() + "' a été clôturé par le citoyen.",
                    Notification.TypeNotification.INCIDENT_CLOTURE
            );
        }
        
        logger.info("Incident clôturé: {}", incidentId);
    }
    
    @Transactional(readOnly = true)
    public Incident getIncidentById(Long id) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Incident introuvable"));
        
        // Forcer le chargement des relations pour éviter LazyInitializationException
        if (incident.getCitoyen() != null) {
            incident.getCitoyen().getEmail(); // Force le chargement
        }
        if (incident.getAgent() != null) {
            incident.getAgent().getEmail(); // Force le chargement
        }
        if (incident.getQuartier() != null) {
            incident.getQuartier().getNom(); // Force le chargement
        }
        if (incident.getDepartement() != null) {
            incident.getDepartement().getNom(); // Force le chargement
        }
        // Forcer le chargement de la collection photos
        if (incident.getPhotos() != null) {
            int photoCount = incident.getPhotos().size(); // Force le chargement de la collection
            // S'assurer que chaque photo est chargée
            for (com.citesignal.model.Photo photo : incident.getPhotos()) {
                if (photo != null) {
                    photo.getChemin(); // Force le chargement
                }
            }
        }
        
        return incident;
    }
    
    public Page<Incident> getIncidentsByCitoyen(Long citoyenId, Pageable pageable) {
        User citoyen = userRepository.findById(citoyenId)
                .orElseThrow(() -> new RuntimeException("Citoyen introuvable"));
        return incidentRepository.findByCitoyen(citoyen, pageable);
    }
    
    public Page<Incident> getIncidentsByAgent(Long agentId, Pageable pageable) {
        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent introuvable"));
        return incidentRepository.findByAgent(agent, pageable);
    }
    
    public Page<Incident> searchIncidents(
            StatutIncident statut,
            CategorieIncident categorie,
            Long quartierId,
            Long departementId,
            LocalDateTime dateDebut,
            LocalDateTime dateFin,
            String recherche,
            Pageable pageable
    ) {
        return incidentRepository.searchIncidents(
                statut, categorie, quartierId, departementId,
                dateDebut, dateFin, recherche, pageable
        );
    }
    
    private void notifyCitizenOfStatusChange(Incident incident, StatutIncident oldStatut) {
        String message = getStatusChangeMessage(incident.getStatut(), incident.getTitre());
        
        // Notification en base
        createNotification(
                incident.getCitoyen(),
                incident,
                "Mise à jour de votre signalement",
                message,
                Notification.TypeNotification.INCIDENT_MIS_A_JOUR
        );
        
        // Email
        try {
            emailService.sendIncidentUpdateEmail(
                    incident.getCitoyen().getEmail(),
                    incident.getTitre(),
                    message
            );
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de l'email de mise à jour", e);
        }
    }
    
    private String getStatusChangeMessage(StatutIncident statut, String titre) {
        return switch (statut) {
            case PRIS_EN_CHARGE -> "Votre signalement '" + titre + "' a été pris en charge par un agent municipal.";
            case EN_RESOLUTION -> "L'intervention pour votre signalement '" + titre + "' est en cours.";
            case RESOLU -> "Votre signalement '" + titre + "' a été résolu. Veuillez vérifier et donner votre avis.";
            case CLOTURE -> "Votre signalement '" + titre + "' a été clôturé.";
            default -> "Votre signalement '" + titre + "' a été mis à jour.";
        };
    }
    
    private void notifyAgentOfAssignment(Incident incident, User agent) {
        String message = "Vous avez été assigné au signalement '" + incident.getTitre() + 
                        "' situé à " + incident.getAdresse() + ".";
        
        // Notification en base
        createNotification(
                agent,
                incident,
                "Nouvelle assignation d'incident",
                message,
                Notification.TypeNotification.INCIDENT_ASSIGNE
        );
        
        // Email
        try {
            emailService.sendAgentAssignmentEmail(
                    agent.getEmail(),
                    incident.getTitre(),
                    incident.getAdresse(),
                    incident.getDescription()
            );
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de l'email d'assignation à l'agent", e);
        }
    }
    
    private void createNotification(User user, Incident incident, String titre, String message, 
                                    Notification.TypeNotification type) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setIncident(incident);
        notification.setTitre(titre);
        notification.setMessage(message);
        notification.setType(type);
        notification.setLu(false);
        notificationRepository.save(notification);
    }
}

