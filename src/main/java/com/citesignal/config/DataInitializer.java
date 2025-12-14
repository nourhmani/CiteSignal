package com.citesignal.config;

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
    }
}

