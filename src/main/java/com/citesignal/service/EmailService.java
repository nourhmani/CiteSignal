package com.citesignal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Value("${app.url}")
    private String appUrl;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    public void sendVerificationEmail(String to, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Vérification de votre compte CiteSignal");
            message.setText("Bonjour,\n\n" +
                    "Merci de vous être inscrit sur CiteSignal.\n\n" +
                    "Pour activer votre compte, veuillez cliquer sur le lien suivant :\n" +
                    appUrl + "/verify-email?token=" + token + "\n\n" +
                    "Ce lien est valide pendant 24 heures.\n\n" +
                    "Cordialement,\n" +
                    "L'équipe CiteSignal");
            
            mailSender.send(message);
            logger.info("Verification email sent to: {}", to);
        } catch (Exception e) {
            logger.error("Error sending verification email to: {}", to, e);
        }
    }
    
    public void sendPasswordResetEmail(String to, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Réinitialisation de votre mot de passe");
            message.setText("Bonjour,\n\n" +
                    "Vous avez demandé la réinitialisation de votre mot de passe.\n\n" +
                    "Pour réinitialiser votre mot de passe, veuillez cliquer sur le lien suivant :\n" +
                    appUrl + "/reset-password?token=" + token + "\n\n" +
                    "Ce lien est valide pendant 1 heure.\n\n" +
                    "Si vous n'avez pas demandé cette réinitialisation, ignorez cet email.\n\n" +
                    "Cordialement,\n" +
                    "L'équipe CiteSignal");
            
            mailSender.send(message);
            logger.info("Password reset email sent to: {}", to);
        } catch (Exception e) {
            logger.error("Error sending password reset email to: {}", to, e);
        }
    }
    
    public void sendIncidentUpdateEmail(String to, String incidentTitle, String updateMessage) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Mise à jour de votre signalement - " + incidentTitle);
            message.setText("Bonjour,\n\n" +
                    "Votre signalement '" + incidentTitle + "' a été mis à jour.\n\n" +
                    "Détails :\n" + updateMessage + "\n\n" +
                    "Vous pouvez consulter votre signalement sur votre tableau de bord.\n\n" +
                    "Cordialement,\n" +
                    "L'équipe CiteSignal");
            
            mailSender.send(message);
            logger.info("Incident update email sent to: {}", to);
        } catch (Exception e) {
            logger.error("Error sending incident update email to: {}", to, e);
        }
    }
    
    public void sendAgentCredentials(String to, String email, String password) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Vos identifiants de connexion - CiteSignal");
            message.setText("Bonjour,\n\n" +
                    "Votre compte Agent Municipal a été créé sur CiteSignal.\n\n" +
                    "Voici vos identifiants de connexion :\n\n" +
                    "Email : " + email + "\n" +
                    "Mot de passe : " + password + "\n\n" +
                    "Vous pouvez vous connecter à l'adresse suivante :\n" +
                    appUrl + "/auth/login\n\n" +
                    "Pour des raisons de sécurité, nous vous recommandons de changer votre mot de passe après votre première connexion.\n\n" +
                    "Cordialement,\n" +
                    "L'équipe CiteSignal");
            
            mailSender.send(message);
            logger.info("Agent credentials email sent to: {}", to);
        } catch (Exception e) {
            logger.error("Error sending agent credentials email to: {}", to, e);
        }
    }
    
    public void sendAdminCredentials(String to, String email, String password) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Vos identifiants de connexion - CiteSignal");
            message.setText("Bonjour,\n\n" +
                    "Votre compte Administrateur a été créé sur CiteSignal.\n\n" +
                    "Voici vos identifiants de connexion :\n\n" +
                    "Email : " + email + "\n" +
                    "Mot de passe : " + password + "\n\n" +
                    "Vous pouvez vous connecter à l'adresse suivante :\n" +
                    appUrl + "/auth/login\n\n" +
                    "Pour des raisons de sécurité, nous vous recommandons de changer votre mot de passe après votre première connexion.\n\n" +
                    "Cordialement,\n" +
                    "L'équipe CiteSignal");
            
            mailSender.send(message);
            logger.info("Admin credentials email sent to: {}", to);
        } catch (Exception e) {
            logger.error("Error sending admin credentials email to: {}", to, e);
        }
    }
}

