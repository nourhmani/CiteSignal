package com.citesignal.dto;

import com.citesignal.model.Incident;
import com.citesignal.model.CategorieIncident;
import com.citesignal.model.PrioriteIncident;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class CreateIncidentRequest {
    
    @NotBlank(message = "Le titre est obligatoire")
    @Size(max = 200, message = "Le titre ne doit pas dépasser 200 caractères")
    private String titre;
    
    @NotBlank(message = "La description est obligatoire")
    @Size(max = 2000, message = "La description ne doit pas dépasser 2000 caractères")
    private String description;
    
    @NotNull(message = "La catégorie est obligatoire")
    private CategorieIncident categorie;
    
    @NotBlank(message = "L'adresse est obligatoire")
    @Size(max = 255, message = "L'adresse ne doit pas dépasser 255 caractères")
    private String adresse;
    
    private Double latitude;
    
    private Double longitude;
    
    private Long quartierId;
    
    private List<MultipartFile> photos;
    
    private PrioriteIncident priorite = PrioriteIncident.MOYENNE;
}

