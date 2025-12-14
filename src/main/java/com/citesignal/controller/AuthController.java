package com.citesignal.controller;

import com.citesignal.dto.JwtAuthenticationResponse;
import com.citesignal.dto.LoginRequest;
import com.citesignal.dto.SignUpRequest;
import com.citesignal.model.User;
import com.citesignal.service.AuthService;
import com.citesignal.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
public class AuthController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private AuthService authService;
    
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("signUpRequest", new SignUpRequest());
        return "auth/register";
    }
    
    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("signUpRequest") SignUpRequest signUpRequest,
                              BindingResult result,
                              RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "auth/register";
        }
        
        try {
            User user = new User();
            user.setNom(signUpRequest.getNom());
            user.setPrenom(signUpRequest.getPrenom());
            user.setEmail(signUpRequest.getEmail());
            user.setTelephone(signUpRequest.getTelephone());
            user.setAdresse(signUpRequest.getAdresse());
            user.setPassword(signUpRequest.getPassword());
            
            userService.registerCitizen(user);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Inscription réussie ! Veuillez vérifier votre email pour activer votre compte.");
            return "redirect:/auth/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/auth/register";
        }
    }
    
    @GetMapping("/login")
    public String showLoginForm(@RequestParam(required = false) String error,
                               @RequestParam(required = false) String logout,
                               Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Email ou mot de passe incorrect");
        }
        if (logout != null) {
            model.addAttribute("successMessage", "Vous avez été déconnecté avec succès");
        }
        return "auth/login";
    }
    
    // API REST endpoints
    @PostMapping("/api/register")
    @ResponseBody
    public ResponseEntity<?> registerUserApi(@Valid @RequestBody SignUpRequest signUpRequest) {
        try {
            User user = new User();
            user.setNom(signUpRequest.getNom());
            user.setPrenom(signUpRequest.getPrenom());
            user.setEmail(signUpRequest.getEmail());
            user.setTelephone(signUpRequest.getTelephone());
            user.setAdresse(signUpRequest.getAdresse());
            user.setPassword(signUpRequest.getPassword());
            
            userService.registerCitizen(user);
            return ResponseEntity.ok().body("Inscription réussie. Veuillez vérifier votre email.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
    
    @PostMapping("/api/login")
    @ResponseBody
    public ResponseEntity<?> authenticateUserApi(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            JwtAuthenticationResponse response = authService.authenticateUser(loginRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Email ou mot de passe incorrect");
        }
    }
}

