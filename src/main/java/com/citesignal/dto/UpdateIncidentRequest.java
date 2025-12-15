package com.citesignal.dto;

import com.citesignal.model.Incident;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class UpdateIncidentRequest {
    
    @Size(max = 200, message = "Le titre ne doit pas dépasser 200 caractères")
    private String titre;
    
    @Size(max = 2000, message = "La description ne doit pas dépasser 2000 caractères")
    private String description;
    
    private Incident.Categorie categorie;
    
    private Incident.Statut statut;
    
    private Incident.Priorite priorite;
    
    @Size(max = 255, message = "L'adresse ne doit pas dépasser 255 caractères")
    private String adresse;
    
    private Double latitude;
    
    private Double longitude;
    
    private Long quartierId;
    
    private Long agentId;
    
    private Long departementId;
    
    private String commentaireResolution;
    
    private List<MultipartFile> photos;
}

