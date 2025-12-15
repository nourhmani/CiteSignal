package com.citesignal.controller;

import com.citesignal.dto.CreateAgentRequest;
import com.citesignal.dto.CreateAdminRequest;
import com.citesignal.model.User;
import com.citesignal.security.UserPrincipal;
import com.citesignal.service.CsvImportService;
import com.citesignal.service.UserService;
import com.citesignal.service.StatisticsService;
import com.citesignal.service.IncidentService;
import com.citesignal.model.Incident;
import com.citesignal.repository.DepartementRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.util.List;
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
    
    @Autowired
    private StatisticsService statisticsService;
    
    @Autowired
    private IncidentService incidentService;
    
    @Autowired
    private DepartementRepository departementRepository;
    
    @GetMapping("/profile")
    public String showProfile(@AuthenticationPrincipal UserPrincipal userPrincipal, Model model) {
        if (userPrincipal == null) {
            return "redirect:/auth/login";
        }
        try {
            User user = userService.getUserById(userPrincipal.getId());
            model.addAttribute("user", user);
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Erreur lors du chargement du profil: " + e.getMessage());
        }
        return "user/profile";
    }
    
    @GetMapping("/history")
    public String showHistory(
            @AuthenticationPrincipal UserPrincipal userPrincipal, 
            Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (userPrincipal == null) {
            return "redirect:/auth/login";
        }
        try {
            User user = userService.getUserById(userPrincipal.getId());
            model.addAttribute("user", user);
            
            // Charger tous les incidents du citoyen pour les statistiques
            List<com.citesignal.model.Incident> allIncidents = 
                    incidentService.getIncidentsByCitoyen(userPrincipal.getId(), 
                            PageRequest.of(0, 1000)).getContent();
            
            // Calculer les statistiques
            long total = allIncidents.size();
            long enAttente = allIncidents.stream()
                    .filter(i -> i.getStatut() == com.citesignal.model.Incident.Statut.SIGNALE)
                    .count();
            long enCours = allIncidents.stream()
                    .filter(i -> i.getStatut() == com.citesignal.model.Incident.Statut.PRIS_EN_CHARGE ||
                                i.getStatut() == com.citesignal.model.Incident.Statut.EN_RESOLUTION)
                    .count();
            long resolus = allIncidents.stream()
                    .filter(i -> i.getStatut() == com.citesignal.model.Incident.Statut.RESOLU ||
                                i.getStatut() == com.citesignal.model.Incident.Statut.CLOTURE)
                    .count();
            
            // Charger les incidents paginés pour l'affichage
            Pageable pageable = PageRequest.of(page, size);
            var incidentsPage = incidentService.getIncidentsByCitoyen(userPrincipal.getId(), pageable);
            
            model.addAttribute("incidents", incidentsPage);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", incidentsPage.getTotalPages());
            model.addAttribute("statuts", Incident.Statut.values());
            model.addAttribute("totalIncidents", total);
            model.addAttribute("enAttente", enAttente);
            model.addAttribute("enCours", enCours);
            model.addAttribute("resolus", resolus);
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Erreur lors du chargement de l'historique: " + e.getMessage());
            model.addAttribute("incidents", org.springframework.data.domain.Page.empty());
            model.addAttribute("totalIncidents", 0L);
            model.addAttribute("enAttente", 0L);
            model.addAttribute("enCours", 0L);
            model.addAttribute("resolus", 0L);
        }
        return "user/history";
    }
    
    @PreAuthorize("hasRole('ADMINISTRATEUR') or hasRole('SUPERADMIN')")
    @GetMapping("/admin/users")
    public String listUsers(Model model) {
        try {
            model.addAttribute("users", userService.getAllUsers());
        } catch (Exception e) {
            model.addAttribute("users", java.util.Collections.emptyList());
            model.addAttribute("errorMessage", "Erreur lors du chargement des utilisateurs: " + e.getMessage());
        }
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
        try {
            model.addAttribute("departements", departementRepository.findAll());
        } catch (Exception e) {
            model.addAttribute("departements", java.util.Collections.emptyList());
        }
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
            
            // Assigner le département si fourni
            if (createAgentRequest.getDepartementId() != null) {
                departementRepository.findById(createAgentRequest.getDepartementId())
                    .ifPresent(agent::setDepartement);
            }
            
            // Créer l'agent avec le mot de passe généré
            userService.createAgentMunicipal(agent, generatedPassword);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Agent municipal créé avec succès ! Les identifiants ont été envoyés par email.");
            return "redirect:/user/admin/agents";
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
        
        return "redirect:/user/admin/agents";
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
            return "redirect:/user/superadmin/admins";
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
        try {
            model.addAttribute("departements", departementRepository.findAll());
        } catch (Exception e) {
            model.addAttribute("departements", java.util.Collections.emptyList());
        }
        return "admin/create-agent";
    }
    
    @PreAuthorize("hasRole('ADMINISTRATEUR') or hasRole('SUPERADMIN')")
    @GetMapping("/admin/statistics")
    public String showStatistics(Model model) {
        try {
            model.addAttribute("statistics", statisticsService.getGeneralStatistics());
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Erreur lors du chargement des statistiques: " + e.getMessage());
        }
        return "admin/statistics";
    }
    
    // Liste des agents (accessible aux admins et superadmins)
    @PreAuthorize("hasRole('ADMINISTRATEUR') or hasRole('SUPERADMIN')")
    @GetMapping("/admin/agents")
    public String listAgents(Model model) {
        try {
            List<User> agents = userService.getUsersByRole(User.RoleName.AGENT_MUNICIPAL);
            model.addAttribute("agents", agents);
            model.addAttribute("title", "Liste des Agents Municipaux");
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Erreur lors du chargement des agents: " + e.getMessage());
            model.addAttribute("agents", java.util.Collections.emptyList());
        }
        return "admin/list-agents";
    }
    
    // Liste des citoyens (accessible aux admins et superadmins)
    @PreAuthorize("hasRole('ADMINISTRATEUR') or hasRole('SUPERADMIN')")
    @GetMapping("/admin/citoyens")
    public String listCitoyens(Model model) {
        try {
            List<User> citoyens = userService.getUsersByRole(User.RoleName.CITOYEN);
            model.addAttribute("citoyens", citoyens);
            model.addAttribute("title", "Liste des Citoyens");
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Erreur lors du chargement des citoyens: " + e.getMessage());
            model.addAttribute("citoyens", java.util.Collections.emptyList());
        }
        return "admin/list-citoyens";
    }
    
    // Liste des administrateurs (accessible uniquement aux superadmins)
    @PreAuthorize("hasRole('SUPERADMIN')")
    @GetMapping("/superadmin/admins")
    public String listAdmins(Model model) {
        try {
            List<User> admins = userService.getUsersByRole(User.RoleName.ADMINISTRATEUR);
            model.addAttribute("admins", admins);
            model.addAttribute("title", "Liste des Administrateurs");
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Erreur lors du chargement des administrateurs: " + e.getMessage());
            model.addAttribute("admins", java.util.Collections.emptyList());
        }
        return "superadmin/list-admins";
    }
    
    // Liste de tous les utilisateurs (accessible uniquement aux superadmins)
    @PreAuthorize("hasRole('SUPERADMIN')")
    @GetMapping("/superadmin/users")
    public String listAllUsers(Model model) {
        try {
            List<User> allUsers = userService.getAllUsers();
            model.addAttribute("users", allUsers);
            model.addAttribute("title", "Liste de tous les Utilisateurs");

            // Statistiques pour les cartes
            long citizenCount = allUsers.stream().filter(u -> u.getRole() == User.RoleName.CITOYEN).count();
            long agentCount = allUsers.stream().filter(u -> u.getRole() == User.RoleName.AGENT_MUNICIPAL).count();
            long adminCount = allUsers.stream().filter(u -> u.getRole() == User.RoleName.ADMINISTRATEUR).count();
            long superadminCount = allUsers.stream().filter(u -> u.getRole() == User.RoleName.SUPERADMIN).count();

            model.addAttribute("citizenCount", citizenCount);
            model.addAttribute("agentCount", agentCount);
            model.addAttribute("adminCount", adminCount);
            model.addAttribute("superadminCount", superadminCount);
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Erreur lors du chargement des utilisateurs: " + e.getMessage());
            model.addAttribute("users", java.util.Collections.emptyList());
            model.addAttribute("citizenCount", 0L);
            model.addAttribute("agentCount", 0L);
            model.addAttribute("adminCount", 0L);
            model.addAttribute("superadminCount", 0L);
        }
        return "superadmin/list-all-users";
    }

    // Édition d'un agent (accessible aux admins et superadmins)
    @PreAuthorize("hasRole('ADMINISTRATEUR') or hasRole('SUPERADMIN')")
    @GetMapping("/admin/edit-agent/{id}")
    public String showEditAgentForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            User agent = userService.getUserById(id);
            if (agent.getRole() != User.RoleName.AGENT_MUNICIPAL) {
                redirectAttributes.addFlashAttribute("errorMessage", "Cet utilisateur n'est pas un agent municipal.");
                return "redirect:/user/admin/agents";
            }

            CreateAgentRequest editRequest = new CreateAgentRequest();
            editRequest.setNom(agent.getNom());
            editRequest.setPrenom(agent.getPrenom());
            editRequest.setEmail(agent.getEmail());
            editRequest.setTelephone(agent.getTelephone());
            editRequest.setAdresse(agent.getAdresse());
            if (agent.getDepartement() != null) {
                editRequest.setDepartementId(agent.getDepartement().getId());
            }

            model.addAttribute("editAgentRequest", editRequest);
            model.addAttribute("agentId", id);
            model.addAttribute("departements", departementRepository.findAll());
            model.addAttribute("title", "Modifier l'Agent Municipal");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur lors du chargement de l'agent: " + e.getMessage());
            return "redirect:/user/admin/agents";
        }
        return "admin/edit-agent";
    }

    @PreAuthorize("hasRole('ADMINISTRATEUR') or hasRole('SUPERADMIN')")
    @PostMapping("/admin/edit-agent/{id}")
    public String editAgent(@PathVariable Long id,
                           @Valid @ModelAttribute("editAgentRequest") CreateAgentRequest editRequest,
                           BindingResult result,
                           RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/edit-agent";
        }

        try {
            User agent = userService.getUserById(id);
            if (agent.getRole() != User.RoleName.AGENT_MUNICIPAL) {
                redirectAttributes.addFlashAttribute("errorMessage", "Cet utilisateur n'est pas un agent municipal.");
                return "redirect:/user/admin/agents";
            }

            // Mettre à jour les informations
            agent.setNom(editRequest.getNom());
            agent.setPrenom(editRequest.getPrenom());
            agent.setEmail(editRequest.getEmail());
            agent.setTelephone(editRequest.getTelephone());
            agent.setAdresse(editRequest.getAdresse());

            // Mettre à jour le département si fourni
            if (editRequest.getDepartementId() != null) {
                departementRepository.findById(editRequest.getDepartementId())
                    .ifPresent(agent::setDepartement);
            } else {
                agent.setDepartement(null);
            }

            userService.updateUser(agent);
            redirectAttributes.addFlashAttribute("successMessage", "Agent municipal modifié avec succès.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur lors de la modification: " + e.getMessage());
            return "redirect:/user/admin/edit-agent/" + id;
        }
        return "redirect:/user/admin/agents";
    }

    // Suppression d'un agent (accessible aux admins et superadmins)
    @PreAuthorize("hasRole('ADMINISTRATEUR') or hasRole('SUPERADMIN')")
    @PostMapping("/admin/delete-agent/{id}")
    public String deleteAgent(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            User agent = userService.getUserById(id);
            if (agent.getRole() != User.RoleName.AGENT_MUNICIPAL) {
                redirectAttributes.addFlashAttribute("errorMessage", "Cet utilisateur n'est pas un agent municipal.");
                return "redirect:/user/admin/agents";
            }

            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("successMessage", "Agent municipal supprimé avec succès.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur lors de la suppression: " + e.getMessage());
        }
        return "redirect:/user/admin/agents";
    }

    // Édition d'un citoyen (accessible aux admins et superadmins)
    @PreAuthorize("hasRole('ADMINISTRATEUR') or hasRole('SUPERADMIN')")
    @GetMapping("/admin/edit-citoyen/{id}")
    public String showEditCitoyenForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            User citoyen = userService.getUserById(id);
            if (citoyen.getRole() != User.RoleName.CITOYEN) {
                redirectAttributes.addFlashAttribute("errorMessage", "Cet utilisateur n'est pas un citoyen.");
                return "redirect:/user/admin/citoyens";
            }

            model.addAttribute("citoyen", citoyen);
            model.addAttribute("title", "Modifier le Citoyen");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur lors du chargement du citoyen: " + e.getMessage());
            return "redirect:/user/admin/citoyens";
        }
        return "admin/edit-citoyen";
    }

    @PreAuthorize("hasRole('ADMINISTRATEUR') or hasRole('SUPERADMIN')")
    @PostMapping("/admin/edit-citoyen/{id}")
    public String editCitoyen(@PathVariable Long id,
                             @RequestParam String nom,
                             @RequestParam String prenom,
                             @RequestParam String email,
                             @RequestParam(required = false) String telephone,
                             @RequestParam(required = false) String adresse,
                             @RequestParam(defaultValue = "true") boolean active,
                             RedirectAttributes redirectAttributes) {
        try {
            User citoyen = userService.getUserById(id);
            if (citoyen.getRole() != User.RoleName.CITOYEN) {
                redirectAttributes.addFlashAttribute("errorMessage", "Cet utilisateur n'est pas un citoyen.");
                return "redirect:/user/admin/citoyens";
            }

            // Mettre à jour les informations
            citoyen.setNom(nom);
            citoyen.setPrenom(prenom);
            citoyen.setEmail(email);
            citoyen.setTelephone(telephone);
            citoyen.setAdresse(adresse);
            citoyen.setActive(active);

            userService.updateUser(citoyen);
            redirectAttributes.addFlashAttribute("successMessage", "Citoyen modifié avec succès.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur lors de la modification: " + e.getMessage());
            return "redirect:/user/admin/edit-citoyen/" + id;
        }
        return "redirect:/user/admin/citoyens";
    }

    // Suppression d'un citoyen (accessible aux admins et superadmins)
    @PreAuthorize("hasRole('ADMINISTRATEUR') or hasRole('SUPERADMIN')")
    @PostMapping("/admin/delete-citoyen/{id}")
    public String deleteCitoyen(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            User citoyen = userService.getUserById(id);
            if (citoyen.getRole() != User.RoleName.CITOYEN) {
                redirectAttributes.addFlashAttribute("errorMessage", "Cet utilisateur n'est pas un citoyen.");
                return "redirect:/user/admin/citoyens";
            }

            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("successMessage", "Citoyen supprimé avec succès.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur lors de la suppression: " + e.getMessage());
        }
        return "redirect:/user/admin/citoyens";
    }

    // Édition d'un administrateur (accessible uniquement aux superadmins)
    @PreAuthorize("hasRole('SUPERADMIN')")
    @GetMapping("/superadmin/edit-admin/{id}")
    public String showEditAdminForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            User admin = userService.getUserById(id);
            if (admin.getRole() != User.RoleName.ADMINISTRATEUR) {
                redirectAttributes.addFlashAttribute("errorMessage", "Cet utilisateur n'est pas un administrateur.");
                return "redirect:/user/superadmin/admins";
            }

            CreateAdminRequest editRequest = new CreateAdminRequest();
            editRequest.setNom(admin.getNom());
            editRequest.setPrenom(admin.getPrenom());
            editRequest.setEmail(admin.getEmail());
            editRequest.setTelephone(admin.getTelephone());
            editRequest.setAdresse(admin.getAdresse());

            model.addAttribute("editAdminRequest", editRequest);
            model.addAttribute("adminId", id);
            model.addAttribute("title", "Modifier l'Administrateur");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur lors du chargement de l'administrateur: " + e.getMessage());
            return "redirect:/user/superadmin/admins";
        }
        return "superadmin/edit-admin";
    }

    @PreAuthorize("hasRole('SUPERADMIN')")
    @PostMapping("/superadmin/edit-admin/{id}")
    public String editAdmin(@PathVariable Long id,
                           @Valid @ModelAttribute("editAdminRequest") CreateAdminRequest editRequest,
                           BindingResult result,
                           RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "superadmin/edit-admin";
        }

        try {
            User admin = userService.getUserById(id);
            if (admin.getRole() != User.RoleName.ADMINISTRATEUR) {
                redirectAttributes.addFlashAttribute("errorMessage", "Cet utilisateur n'est pas un administrateur.");
                return "redirect:/user/superadmin/admins";
            }

            // Mettre à jour les informations
            admin.setNom(editRequest.getNom());
            admin.setPrenom(editRequest.getPrenom());
            admin.setEmail(editRequest.getEmail());
            admin.setTelephone(editRequest.getTelephone());
            admin.setAdresse(editRequest.getAdresse());

            userService.updateUser(admin);
            redirectAttributes.addFlashAttribute("successMessage", "Administrateur modifié avec succès.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur lors de la modification: " + e.getMessage());
            return "redirect:/user/superadmin/edit-admin/" + id;
        }
        return "redirect:/user/superadmin/admins";
    }

    // Suppression d'un administrateur (accessible uniquement aux superadmins)
    @PreAuthorize("hasRole('SUPERADMIN')")
    @PostMapping("/superadmin/delete-admin/{id}")
    public String deleteAdmin(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            User admin = userService.getUserById(id);
            if (admin.getRole() != User.RoleName.ADMINISTRATEUR) {
                redirectAttributes.addFlashAttribute("errorMessage", "Cet utilisateur n'est pas un administrateur.");
                return "redirect:/user/superadmin/admins";
            }

            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("successMessage", "Administrateur supprimé avec succès.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur lors de la suppression: " + e.getMessage());
        }
        return "redirect:/user/superadmin/admins";
    }

    // Édition d'un utilisateur (accessible uniquement aux superadmins)
    @PreAuthorize("hasRole('SUPERADMIN')")
    @GetMapping("/superadmin/edit-user/{id}")
    public String showEditUserForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            User user = userService.getUserById(id);
            model.addAttribute("user", user);
            model.addAttribute("roles", User.RoleName.values());
            model.addAttribute("departements", departementRepository.findAll());
            model.addAttribute("title", "Modifier l'Utilisateur");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur lors du chargement de l'utilisateur: " + e.getMessage());
            return "redirect:/user/superadmin/users";
        }
        return "superadmin/edit-user";
    }

    @PreAuthorize("hasRole('SUPERADMIN')")
    @PostMapping("/superadmin/edit-user/{id}")
    public String editUser(@PathVariable Long id,
                          @RequestParam String nom,
                          @RequestParam String prenom,
                          @RequestParam String email,
                          @RequestParam User.RoleName role,
                          @RequestParam(required = false) String telephone,
                          @RequestParam(required = false) String adresse,
                          @RequestParam(defaultValue = "true") boolean active,
                          @RequestParam(required = false) Long departementId,
                          RedirectAttributes redirectAttributes) {
        try {
            User user = userService.getUserById(id);

            // Mettre à jour les informations
            user.setNom(nom);
            user.setPrenom(prenom);
            user.setEmail(email);
            user.setRole(role);
            user.setTelephone(telephone);
            user.setAdresse(adresse);
            user.setActive(active);

            // Mettre à jour le département si fourni et si l'utilisateur est un agent
            if (departementId != null && role == User.RoleName.AGENT_MUNICIPAL) {
                departementRepository.findById(departementId).ifPresent(user::setDepartement);
            } else {
                user.setDepartement(null);
            }

            userService.updateUser(user);
            redirectAttributes.addFlashAttribute("successMessage", "Utilisateur modifié avec succès.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur lors de la modification: " + e.getMessage());
            return "redirect:/user/superadmin/edit-user/" + id;
        }
        return "redirect:/user/superadmin/users";
    }

    // Suppression d'un utilisateur (accessible uniquement aux superadmins)
    @PreAuthorize("hasRole('SUPERADMIN')")
    @PostMapping("/superadmin/delete-user/{id}")
    public String deleteUserSuperAdmin(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("successMessage", "Utilisateur supprimé avec succès.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur lors de la suppression: " + e.getMessage());
        }
        return "redirect:/user/superadmin/users";
    }
}

