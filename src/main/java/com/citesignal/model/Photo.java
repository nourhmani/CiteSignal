package com.citesignal.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "photos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Photo {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Size(max = 255)
    @Column(name = "nom_fichier", nullable = false, length = 255)
    private String nomFichier;
    
    @NotBlank
    @Size(max = 500)
    @Column(name = "chemin", nullable = false, length = 500)
    private String chemin;
    
    @Size(max = 50)
    @Column(name = "type_mime", length = 50)
    private String typeMime;
    
    @Column(name = "taille")
    private Long taille;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    @NotNull
    private Incident incident;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

