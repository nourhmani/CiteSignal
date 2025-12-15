package com.citesignal.config;

import com.citesignal.model.Departement;
import com.citesignal.model.Quartier;
import com.citesignal.model.User;
import com.citesignal.repository.DepartementRepository;
import com.citesignal.repository.QuartierRepository;
import com.citesignal.repository.UserRepository;
import com.citesignal.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private QuartierRepository quartierRepository;
    
    @Autowired
    private DepartementRepository departementRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public void run(String... args) throws Exception {
        // Corriger la taille de la colonne 'name' dans la table 'roles' si nécessaire
        try {
            // Vérifier si la table existe
            Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'roles'",
                Integer.class
            );
            
            if (tableCount != null && tableCount > 0) {
                // La table existe, on peut modifier la colonne
                jdbcTemplate.execute("ALTER TABLE roles MODIFY COLUMN name VARCHAR(50) NOT NULL");
                logger.info("Colonne 'name' de la table 'roles' mise à jour avec succès");
            }
        } catch (Exception e) {
            // Ignorer l'erreur si la colonne a déjà la bonne taille ou si la table n'existe pas encore
            logger.debug("Tentative de modification de la colonne 'name' : {}", e.getMessage());
        }
        
        // Initialiser les rôles au démarrage
        // Plus nécessaire car les rôles sont maintenant un enum
        
        // Initialiser les quartiers de base
        initializeQuartiers();
        
        // Initialiser les départements de base
        initializeDepartements();
        
        // Initialiser le super administrateur
        initializeSuperAdmin();
    }
    
    private void initializeQuartiers() {
        if (quartierRepository.count() == 0) {
            // Quartiers authentiques de Tunis, Tunisie
            String[] quartiers = {
                "Bab Souika", "Bab El Khadra", "Bab El Bhar", "Bab Jedid",
                "Le Bardo", "Carthage", "Sidi Bou Said", "La Marsa",
                "El Menzah", "Mutuelleville", "Lafayette", "Belvédère",
                "Centre-ville", "Kasbah", "Medina", "Ariana",
                "Ezzouhour", "El Omrane", "El Omrane Supérieur", "Cité El Khadra"
            };
            
            for (String nom : quartiers) {
                Quartier quartier = new Quartier();
                quartier.setNom(nom);
                // Codes postaux tunisiens selon les quartiers
                if (nom.contains("Ariana") || nom.contains("La Marsa") || nom.contains("Carthage") || nom.contains("Sidi Bou Said")) {
                    quartier.setCodePostal("2030"); // Ariana
                } else if (nom.contains("Le Bardo")) {
                    quartier.setCodePostal("2000"); // Le Bardo
                } else {
                    quartier.setCodePostal("1000"); // Tunis centre
                }
                quartierRepository.save(quartier);
            }
            logger.info("Quartiers tunisiens initialisés: {}", quartiers.length);
        }
    }
    
    private void initializeDepartements() {
        if (departementRepository.count() == 0) {
            String[] departements = {
                "Voirie et Infrastructure",
                "Propreté Urbaine",
                "Éclairage Public",
                "Eau et Assainissement",
                "Sécurité et Circulation"
            };
            
            for (String nom : departements) {
                Departement departement = new Departement();
                departement.setNom(nom);
                departement.setDescription("Département " + nom);
                departementRepository.save(departement);
            }
            logger.info("Départements initialisés: {}", departements.length);
        }
    }
    
    private void initializeSuperAdmin() {
        try {
            userRepository.findByEmail("superadmin@citesignal.com");
            // Si on arrive ici, l'utilisateur existe déjà
            logger.info("Super administrateur existe déjà");
        } catch (Exception e) {
            // L'utilisateur n'existe pas, on le crée
            User superAdmin = new User();
            superAdmin.setNom("Admin");
            superAdmin.setPrenom("Super");
            superAdmin.setEmail("superadmin@citesignal.com");
            superAdmin.setRole(User.RoleName.SUPERADMIN);
            superAdmin.setActive(true);
            superAdmin.setEmailVerified(true);
            
            String defaultPassword = "admin123";
            userService.createSuperAdmin(superAdmin, defaultPassword);
            
            logger.info("Super administrateur créé: {}", superAdmin.getEmail());
            logger.info("Mot de passe par défaut: {}", defaultPassword);
        }
    }
}

