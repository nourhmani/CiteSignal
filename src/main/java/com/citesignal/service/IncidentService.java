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
        incident.setStatut(Incident.Statut.SIGNALE);
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
        
        // Vérifier les permissions
        if (user.hasRole(User.RoleName.AGENT_MUNICIPAL) && 
            incident.getAgent() != null && 
            !incident.getAgent().getId().equals(userId)) {
            throw new RuntimeException("Vous n'êtes pas autorisé à modifier cet incident");
        }
        
        // Mettre à jour les champs
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
        
        // Gérer le workflow des statuts
        Incident.Statut oldStatut = incident.getStatut();
        if (request.getStatut() != null && request.getStatut() != oldStatut) {
            updateIncidentStatus(incident, request.getStatut(), request.getCommentaireResolution());
        }
        
        // Assigner un agent
        if (request.getAgentId() != null) {
            User agent = userRepository.findById(request.getAgentId())
                    .orElseThrow(() -> new RuntimeException("Agent introuvable"));
            incident.setAgent(agent);
            
            // Assigner le département de l'agent
            if (agent.getDepartement() != null) {
                incident.setDepartement(agent.getDepartement());
            }
        }
        
        // Assigner un département
        if (request.getDepartementId() != null) {
            Departement departement = departementRepository.findById(request.getDepartementId())
                    .orElse(null);
            incident.setDepartement(departement);
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
    public void updateIncidentStatus(Incident incident, Incident.Statut newStatut, String commentaire) {
        Incident.Statut oldStatut = incident.getStatut();
        
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
    
    private boolean isValidStatusTransition(Incident.Statut oldStatut, Incident.Statut newStatut) {
        // Définir les transitions valides
        return switch (oldStatut) {
            case SIGNALE -> newStatut == Incident.Statut.PRIS_EN_CHARGE;
            case PRIS_EN_CHARGE -> newStatut == Incident.Statut.EN_RESOLUTION || 
                                   newStatut == Incident.Statut.SIGNALE;
            case EN_RESOLUTION -> newStatut == Incident.Statut.RESOLU || 
                                  newStatut == Incident.Statut.PRIS_EN_CHARGE;
            case RESOLU -> newStatut == Incident.Statut.CLOTURE || 
                           newStatut == Incident.Statut.EN_RESOLUTION;
            case CLOTURE -> false; // Une fois clôturé, on ne peut plus changer
        };
    }
    
    @Transactional
    public void closeIncident(Long incidentId, String feedbackCitoyen, Integer noteSatisfaction) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new RuntimeException("Incident introuvable"));
        
        if (incident.getStatut() != Incident.Statut.RESOLU) {
            throw new RuntimeException("Seuls les incidents résolus peuvent être clôturés");
        }
        
        incident.setStatut(Incident.Statut.CLOTURE);
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
            Incident.Statut statut,
            Incident.Categorie categorie,
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
    
    private void notifyCitizenOfStatusChange(Incident incident, Incident.Statut oldStatut) {
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
    
    private String getStatusChangeMessage(Incident.Statut statut, String titre) {
        return switch (statut) {
            case PRIS_EN_CHARGE -> "Votre signalement '" + titre + "' a été pris en charge par un agent municipal.";
            case EN_RESOLUTION -> "L'intervention pour votre signalement '" + titre + "' est en cours.";
            case RESOLU -> "Votre signalement '" + titre + "' a été résolu. Veuillez vérifier et donner votre avis.";
            case CLOTURE -> "Votre signalement '" + titre + "' a été clôturé.";
            default -> "Votre signalement '" + titre + "' a été mis à jour.";
        };
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

