package com.citesignal.service;

import com.citesignal.model.RoleName;
import com.citesignal.model.User;
import com.citesignal.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private EmailService emailService;
    
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int TOKEN_LENGTH = 32;
    
    @Transactional
    public User registerCitizen(User user) {
        // Vérifier si l'email existe déjà
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Un compte avec cet email existe déjà");
        }
        
        // Encoder le mot de passe
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        // Assigner le rôle CITOYEN par défaut
        user.setRole(RoleName.CITOYEN);
        
        // Générer un token de vérification
        String verificationToken = generateToken();
        user.setVerificationToken(verificationToken);
        user.setEmailVerified(false);
        user.setActive(true);
        
        // Sauvegarder l'utilisateur
        User savedUser = userRepository.save(user);
        
        // Envoyer l'email de vérification
        try {
            emailService.sendVerificationEmail(user.getEmail(), verificationToken);
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de l'email de vérification", e);
        }
        
        logger.info("Nouvel utilisateur enregistré: {}", user.getEmail());
        return savedUser;
    }
    
    @Transactional
    public User createAgentOrAdmin(User user, RoleName roleName) {
        // Vérifier si l'email existe déjà
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Un compte avec cet email existe déjà");
        }
        
        // Encoder le mot de passe
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        // Assigner le rôle spécifié
        user.setRole(roleName);
        
        // Les agents et admins n'ont pas besoin de vérification d'email
        user.setEmailVerified(true);
        user.setActive(true);
        
        User savedUser = userRepository.save(user);
        logger.info("Nouvel {} créé: {}", roleName, user.getEmail());
        return savedUser;
    }
    
    @Transactional
    public User createAgentMunicipal(User user, String generatedPassword) {
        // Vérifier si l'email existe déjà
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Un compte avec cet email existe déjà");
        }
        
        // Encoder le mot de passe généré
        user.setPassword(passwordEncoder.encode(generatedPassword));
        
        // Assigner le rôle AGENT_MUNICIPAL
        user.setRole(RoleName.AGENT_MUNICIPAL);
        
        // Les agents n'ont pas besoin de vérification d'email
        user.setEmailVerified(true);
        user.setActive(true);
        
        User savedUser = userRepository.save(user);
        
        // Envoyer les identifiants par email
        try {
            emailService.sendAgentCredentials(user.getEmail(), user.getEmail(), generatedPassword);
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de l'email avec les identifiants", e);
        }
        
        logger.info("Nouvel agent municipal créé: {}", user.getEmail());
        return savedUser;
    }
    
    @Transactional
    public User createAdministrator(User user, String generatedPassword) {
        // Vérifier si l'email existe déjà
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Un compte avec cet email existe déjà");
        }
        
        // Encoder le mot de passe généré
        user.setPassword(passwordEncoder.encode(generatedPassword));
        
        // Assigner le rôle ADMINISTRATEUR
        user.setRole(RoleName.ADMINISTRATEUR);
        
        // Les admins n'ont pas besoin de vérification d'email
        user.setEmailVerified(true);
        user.setActive(true);
        
        User savedUser = userRepository.save(user);
        
        // Envoyer les identifiants par email
        try {
            emailService.sendAdminCredentials(user.getEmail(), user.getEmail(), generatedPassword);
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de l'email avec les identifiants", e);
        }
        
        logger.info("Nouvel administrateur créé: {}", user.getEmail());
        return savedUser;
    }
    
    @Transactional
    public User createSuperAdmin(User user, String generatedPassword) {
        // Vérifier si l'email existe déjà
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Un compte avec cet email existe déjà");
        }
        
        // Encoder le mot de passe généré
        user.setPassword(passwordEncoder.encode(generatedPassword));
        
        // Assigner le rôle SUPERADMIN
        user.setRole(RoleName.SUPERADMIN);
        
        // Les super admins n'ont pas besoin de vérification d'email
        user.setEmailVerified(true);
        user.setActive(true);
        
        User savedUser = userRepository.save(user);
        
        logger.info("Nouveau super administrateur créé: {}", user.getEmail());
        return savedUser;
    }
    
    public String generateRandomPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(12);
        String upperCase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerCase = "abcdefghijklmnopqrstuvwxyz";
        String numbers = "0123456789";
        String specialChars = "!@#$%&*";
        String allChars = upperCase + lowerCase + numbers + specialChars;
        
        // Assurer au moins un caractère de chaque type
        password.append(upperCase.charAt(random.nextInt(upperCase.length())));
        password.append(lowerCase.charAt(random.nextInt(lowerCase.length())));
        password.append(numbers.charAt(random.nextInt(numbers.length())));
        password.append(specialChars.charAt(random.nextInt(specialChars.length())));
        
        // Remplir le reste
        for (int i = 4; i < 12; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }
        
        // Mélanger les caractères
        char[] passwordArray = password.toString().toCharArray();
        for (int i = passwordArray.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = passwordArray[i];
            passwordArray[i] = passwordArray[j];
            passwordArray[j] = temp;
        }
        
        return new String(passwordArray);
    }
    
    @Transactional
    public boolean verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Token de vérification invalide"));
        
        user.setEmailVerified(true);
        user.setVerificationToken(null);
        userRepository.save(user);
        
        logger.info("Email vérifié pour l'utilisateur: {}", user.getEmail());
        return true;
    }
    
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
    }
    
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
    }
    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    public List<User> getUsersByRole(RoleName role) {
        return userRepository.findAll().stream()
                .filter(user -> user.getRole() == role)
                .collect(java.util.stream.Collectors.toList());
    }
    
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }
    
    @Transactional
    public User updateUser(User user) {
        return userRepository.save(user);
    }
    
    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
        logger.info("Utilisateur supprimé: {}", id);
    }
    
    private String generateToken() {
        SecureRandom random = new SecureRandom();
        StringBuilder token = new StringBuilder(TOKEN_LENGTH);
        
        for (int i = 0; i < TOKEN_LENGTH; i++) {
            token.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        
        return token.toString();
    }
}

