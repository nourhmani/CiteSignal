package com.citesignal.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);
    
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        logger.error("Unauthorized error: {}", authException.getMessage());
        
        // Check if the request is for an API endpoint
        String requestPath = request.getRequestURI();
        String acceptHeader = request.getHeader("Accept");
        
        // If it's an API request (starts with /api or accepts JSON), return 401
        if (requestPath.startsWith("/api") || 
            (acceptHeader != null && acceptHeader.contains("application/json"))) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Error: Unauthorized");
        } else {
            // For HTML requests, redirect to login page
            response.sendRedirect("/auth/login");
        }
    }
}

