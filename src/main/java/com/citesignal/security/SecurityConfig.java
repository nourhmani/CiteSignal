package com.citesignal.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    @Autowired
    private CustomUserDetailsService customUserDetailsService;
    
    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/auth/api/**", "/logout")) 
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(jwtAuthenticationEntryPoint))
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/register", "/login", "/verify-email").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**", "/uploads/**", "/exemple-agents.csv").permitAll()
                .requestMatchers("/user/admin/create-agent", "/user/admin/import-agents", "/user/admin/users", "/user/admin/statistics").hasAnyAuthority("ROLE_ADMINISTRATEUR", "ROLE_SUPERADMIN")
                .requestMatchers("/user/admin/**").hasAnyAuthority("ROLE_ADMINISTRATEUR", "ROLE_SUPERADMIN")
                .requestMatchers("/user/superadmin/**").hasAuthority("ROLE_SUPERADMIN")
                .requestMatchers("/admin/**").hasAnyAuthority("ROLE_ADMINISTRATEUR", "ROLE_SUPERADMIN")
                .requestMatchers("/agent/**").hasAnyAuthority("ROLE_AGENT_MUNICIPAL", "ROLE_ADMINISTRATEUR", "ROLE_SUPERADMIN")
                .requestMatchers("/incidents/create").hasAuthority("ROLE_CITOYEN")
                .requestMatchers("/incidents/*/edit").hasAnyAuthority("ROLE_AGENT_MUNICIPAL", "ROLE_ADMINISTRATEUR", "ROLE_SUPERADMIN")
                .requestMatchers("/incidents/*/close").hasAuthority("ROLE_CITOYEN")
                .requestMatchers("/incidents/**").authenticated()
                .requestMatchers("/notifications/**").authenticated()
                .requestMatchers("/user/profile", "/user/history").authenticated()
                .requestMatchers("/", "/dashboard").authenticated()
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            .formLogin(form -> form
                .loginPage("/auth/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/auth/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/auth/login?logout=true")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID", "XSRF-TOKEN")
                .permitAll()
            );
        
        return http.build();
    }
}

