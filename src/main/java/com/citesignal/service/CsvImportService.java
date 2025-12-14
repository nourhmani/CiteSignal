package com.citesignal.service;

import com.citesignal.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class CsvImportService {
    
    private static final Logger logger = LoggerFactory.getLogger(CsvImportService.class);
    
    @Autowired
    private UserService userService;
    
    public static class ImportResult {
        private int totalRows;
        private int successCount;
        private int errorCount;
        private List<String> errors;
        
        public ImportResult() {
            this.errors = new ArrayList<>();
        }
        
        public int getTotalRows() {
            return totalRows;
        }
        
        public void setTotalRows(int totalRows) {
            this.totalRows = totalRows;
        }
        
        public int getSuccessCount() {
            return successCount;
        }
        
        public void setSuccessCount(int successCount) {
            this.successCount = successCount;
        }
        
        public int getErrorCount() {
            return errorCount;
        }
        
        public void setErrorCount(int errorCount) {
            this.errorCount = errorCount;
        }
        
        public List<String> getErrors() {
            return errors;
        }
        
        public void addError(String error) {
            this.errors.add(error);
            this.errorCount++;
        }
        
        public void incrementSuccess() {
            this.successCount++;
        }
    }
    
    @Transactional
    public ImportResult importAgentsFromCsv(MultipartFile file) {
        ImportResult result = new ImportResult();
        
        if (file.isEmpty()) {
            result.addError("Le fichier est vide");
            return result;
        }
        
        if (!file.getOriginalFilename().endsWith(".csv")) {
            result.addError("Le fichier doit être au format CSV");
            return result;
        }
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            
            String line;
            int lineNumber = 0;
            
            // Lire l'en-tête (première ligne)
            String headerLine = reader.readLine();
            if (headerLine == null) {
                result.addError("Le fichier CSV est vide");
                return result;
            }
            
            // Vérifier que l'en-tête contient les colonnes attendues
            String[] headers = parseCsvLine(headerLine);
            if (!isValidHeader(headers)) {
                result.addError("L'en-tête du CSV est invalide. Colonnes attendues: nom, prenom, email, telephone, adresse");
                return result;
            }
            
            // Lire les données
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                result.setTotalRows(lineNumber);
                
                if (line.trim().isEmpty()) {
                    continue; // Ignorer les lignes vides
                }
                
                try {
                    String[] values = parseCsvLine(line);
                    
                    if (values.length < 3) {
                        result.addError("Ligne " + lineNumber + ": Nombre de colonnes insuffisant");
                        continue;
                    }
                    
                    // Extraire les valeurs
                    String nom = values.length > 0 ? values[0].trim() : "";
                    String prenom = values.length > 1 ? values[1].trim() : "";
                    String email = values.length > 2 ? values[2].trim() : "";
                    String telephone = values.length > 3 ? values[3].trim() : "";
                    String adresse = values.length > 4 ? values[4].trim() : "";
                    
                    // Valider les champs obligatoires
                    if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty()) {
                        result.addError("Ligne " + lineNumber + ": Les champs nom, prénom et email sont obligatoires");
                        continue;
                    }
                    
                    // Valider l'email
                    if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                        result.addError("Ligne " + lineNumber + ": Email invalide (" + email + ")");
                        continue;
                    }
                    
                    // Créer l'agent
                    User agent = new User();
                    agent.setNom(nom);
                    agent.setPrenom(prenom);
                    agent.setEmail(email);
                    agent.setTelephone(telephone.isEmpty() ? null : telephone);
                    agent.setAdresse(adresse.isEmpty() ? null : adresse);
                    
                    // Générer un mot de passe et créer l'agent
                    String generatedPassword = userService.generateRandomPassword();
                    userService.createAgentMunicipal(agent, generatedPassword);
                    
                    result.incrementSuccess();
                    logger.info("Agent créé depuis CSV: {} {} ({})", prenom, nom, email);
                    
                } catch (Exception e) {
                    result.addError("Ligne " + lineNumber + ": " + e.getMessage());
                    logger.error("Erreur lors du traitement de la ligne {}: {}", lineNumber, e.getMessage());
                }
            }
            
        } catch (Exception e) {
            result.addError("Erreur lors de la lecture du fichier: " + e.getMessage());
            logger.error("Erreur lors de l'import CSV", e);
        }
        
        return result;
    }
    
    private String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder currentValue = new StringBuilder();
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(currentValue.toString());
                currentValue = new StringBuilder();
            } else {
                currentValue.append(c);
            }
        }
        values.add(currentValue.toString());
        
        return values.toArray(new String[0]);
    }
    
    private boolean isValidHeader(String[] headers) {
        if (headers.length < 3) {
            return false;
        }
        
        // Convertir en minuscules pour la comparaison
        String first = headers[0].trim().toLowerCase();
        String second = headers[1].trim().toLowerCase();
        String third = headers[2].trim().toLowerCase();
        
        // Vérifier que les premières colonnes sont nom, prenom, email
        return (first.equals("nom") || first.equals("name")) &&
               (second.equals("prenom") || second.equals("prénom") || second.equals("prenom") || second.equals("firstname")) &&
               (third.equals("email") || third.equals("e-mail") || third.equals("mail"));
    }
}

