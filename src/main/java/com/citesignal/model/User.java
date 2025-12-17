package com.citesignal.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users", 
       uniqueConstraints = {
           @UniqueConstraint(columnNames = "email")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String nom;
    
    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String prenom;
    
    @NotBlank
    @Size(max = 100)
    @Email
    @Column(nullable = false, unique = true, length = 100)
    private String email;
    
    @Size(max = 20)
    @Column(length = 20)
    private String telephone;
    
    @Size(max = 255)
    @Column(length = 255)
    private String adresse;
    
    @NotBlank
    @Size(min = 8, max = 100)
    @Column(nullable = false, length = 100)
    private String password;
    
    @Column(name = "email_verified")
    private Boolean emailVerified = false;
    
    @Column(name = "verification_token", length = 100)
    private String verificationToken;
    
    @Column(name = "reset_token", length = 100)
    private String resetToken;
    
    @Column(name = "reset_token_expiry")
    private LocalDateTime resetTokenExpiry;
    
    @Column(name = "active")
    private Boolean active = true;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private RoleName role;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departement_id")
    private Departement departement;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Méthodes utilitaires
    public boolean hasRole(RoleName roleName) {
        return role != null && role == roleName;
    }
}

