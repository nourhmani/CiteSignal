package com.citesignal.controller;

import com.citesignal.dto.CreateAgentRequest;
import com.citesignal.dto.CreateAdminRequest;
import com.citesignal.model.User;
import com.citesignal.security.UserPrincipal;
import com.citesignal.service.CsvImportService;
import com.citesignal.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/user")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private CsvImportService csvImportService;
    
    @GetMapping("/profile")
    public String showProfile(@AuthenticationPrincipal UserPrincipal userPrincipal, Model model) {
        User user = userService.getUserById(userPrincipal.getId());
        model.addAttribute("user", user);
        return "user/profile";
    }
    
    @GetMapping("/history")
    public String showHistory(@AuthenticationPrincipal UserPrincipal userPrincipal, Model model) {
        User user = userService.getUserById(userPrincipal.getId());
        model.addAttribute("user", user);
        // TODO: Ajouter l'historique des signalements quand l'entité Incident sera créée
        return "user/history";
    }
    
    @PreAuthorize("hasRole('ADMINISTRATEUR') or hasRole('SUPERADMIN')")
    @GetMapping("/admin/users")
    public String listUsers(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        return "admin/users";
    }
    
    @PreAuthorize("hasRole('ADMINISTRATEUR') or hasRole('SUPERADMIN')")
    @PostMapping("/admin/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("successMessage", "Utilisateur supprimé avec succès");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur lors de la suppression");
        }
        return "redirect:/user/admin/users";
    }
    
    @PreAuthorize("hasRole('ADMINISTRATEUR') or hasRole('SUPERADMIN')")
    @GetMapping("/admin/create-agent")
    public String showCreateAgentForm(Model model) {
        model.addAttribute("createAgentRequest", new CreateAgentRequest());
        return "admin/create-agent";
    }
    
    @PreAuthorize("hasRole('ADMINISTRATEUR') or hasRole('SUPERADMIN')")
    @PostMapping("/admin/create-agent")
    public String createAgent(@Valid @ModelAttribute("createAgentRequest") CreateAgentRequest createAgentRequest,
                            BindingResult result,
                            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/create-agent";
        }
        
        try {
            // Générer un mot de passe sécurisé
            String generatedPassword = userService.generateRandomPassword();
            
            // Créer l'utilisateur agent
            User agent = new User();
            agent.setNom(createAgentRequest.getNom());
            agent.setPrenom(createAgentRequest.getPrenom());
            agent.setEmail(createAgentRequest.getEmail());
            agent.setTelephone(createAgentRequest.getTelephone());
            agent.setAdresse(createAgentRequest.getAdresse());
            
            // Créer l'agent avec le mot de passe généré
            userService.createAgentMunicipal(agent, generatedPassword);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Agent municipal créé avec succès ! Les identifiants ont été envoyés par email.");
            return "redirect:/user/admin/users";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("createAgentRequest", createAgentRequest);
            return "redirect:/user/admin/create-agent";
        }
    }
    
    @PreAuthorize("hasRole('ADMINISTRATEUR') or hasRole('SUPERADMIN')")
    @GetMapping("/admin/import-agents")
    public String showImportAgentsForm(Model model) {
        return "admin/import-agents";
    }
    
    @PreAuthorize("hasRole('ADMINISTRATEUR') or hasRole('SUPERADMIN')")
    @PostMapping("/admin/import-agents")
    public String importAgentsFromCsv(@RequestParam("file") MultipartFile file,
                                    RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Veuillez sélectionner un fichier CSV");
            return "redirect:/user/admin/import-agents";
        }
        
        try {
            CsvImportService.ImportResult result = csvImportService.importAgentsFromCsv(file);
            
            if (result.getSuccessCount() > 0) {
                String successMsg = String.format(
                    "Import terminé : %d agent(s) créé(s) avec succès sur %d ligne(s).",
                    result.getSuccessCount(),
                    result.getTotalRows()
                );
                redirectAttributes.addFlashAttribute("successMessage", successMsg);
            }
            
            if (result.getErrorCount() > 0) {
                StringBuilder errorMsg = new StringBuilder();
                errorMsg.append(String.format("Erreurs rencontrées (%d) :\n", result.getErrorCount()));
                for (int i = 0; i < Math.min(result.getErrors().size(), 10); i++) {
                    errorMsg.append("- ").append(result.getErrors().get(i)).append("\n");
                }
                if (result.getErrors().size() > 10) {
                    errorMsg.append("... et ").append(result.getErrors().size() - 10).append(" autre(s) erreur(s)");
                }
                redirectAttributes.addFlashAttribute("errorMessage", errorMsg.toString());
            }
            
            if (result.getSuccessCount() == 0 && result.getErrorCount() == 0) {
                redirectAttributes.addFlashAttribute("errorMessage", "Aucun agent n'a pu être créé. Vérifiez le format du fichier CSV.");
            }
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Erreur lors de l'import : " + e.getMessage());
        }
        
        return "redirect:/user/admin/users";
    }
    
    // Endpoints pour SUPERADMIN - Création d'administrateurs
    @PreAuthorize("hasRole('SUPERADMIN')")
    @GetMapping("/superadmin/create-admin")
    public String showCreateAdminForm(Model model) {
        model.addAttribute("createAdminRequest", new CreateAdminRequest());
        return "superadmin/create-admin";
    }
    
    @PreAuthorize("hasRole('SUPERADMIN')")
    @PostMapping("/superadmin/create-admin")
    public String createAdmin(@Valid @ModelAttribute("createAdminRequest") CreateAdminRequest createAdminRequest,
                            BindingResult result,
                            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "superadmin/create-admin";
        }
        
        try {
            // Générer un mot de passe sécurisé
            String generatedPassword = userService.generateRandomPassword();
            
            // Créer l'utilisateur admin
            User admin = new User();
            admin.setNom(createAdminRequest.getNom());
            admin.setPrenom(createAdminRequest.getPrenom());
            admin.setEmail(createAdminRequest.getEmail());
            admin.setTelephone(createAdminRequest.getTelephone());
            admin.setAdresse(createAdminRequest.getAdresse());
            
            // Créer l'admin avec le mot de passe généré
            userService.createAdministrator(admin, generatedPassword);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Administrateur créé avec succès ! Les identifiants ont été envoyés par email.");
            return "redirect:/user/admin/users";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("createAdminRequest", createAdminRequest);
            return "redirect:/user/superadmin/create-admin";
        }
    }
    
    @PreAuthorize("hasRole('SUPERADMIN')")
    @GetMapping("/superadmin/create-agent")
    public String showCreateAgentFormSuperAdmin(Model model) {
        model.addAttribute("createAgentRequest", new CreateAgentRequest());
        return "admin/create-agent";
    }
}

