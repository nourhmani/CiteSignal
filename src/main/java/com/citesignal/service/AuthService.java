package com.citesignal.service;

import com.citesignal.dto.JwtAuthenticationResponse;
import com.citesignal.dto.LoginRequest;
import com.citesignal.model.User;
import com.citesignal.repository.UserRepository;
import com.citesignal.security.JwtTokenProvider;
import com.citesignal.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

// collectors removed after role-model change

@Service
public class AuthService {
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @Autowired
    private UserRepository userRepository;
    
    public JwtAuthenticationResponse authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        String jwt = tokenProvider.generateToken(authentication);
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        
        String roles = user.getRole() != null ? user.getRole().name() : "";
        
        return new JwtAuthenticationResponse(
                jwt,
                "Bearer",
                userPrincipal.getId(),
                userPrincipal.getEmail(),
                roles
        );
    }
}

