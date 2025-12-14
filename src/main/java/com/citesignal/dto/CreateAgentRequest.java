package com.citesignal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateAgentRequest {
    
    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 50, message = "Le nom ne doit pas dépasser 50 caractères")
    private String nom;
    
    @NotBlank(message = "Le prénom est obligatoire")
    @Size(max = 50, message = "Le prénom ne doit pas dépasser 50 caractères")
    private String prenom;
    
    @NotBlank(message = "L'email est obligatoire")
    @Size(max = 100, message = "L'email ne doit pas dépasser 100 caractères")
    @Email(message = "L'email doit être valide")
    private String email;
    
    @Size(max = 20, message = "Le téléphone ne doit pas dépasser 20 caractères")
    private String telephone;
    
    @Size(max = 255, message = "L'adresse ne doit pas dépasser 255 caractères")
    private String adresse;
}

