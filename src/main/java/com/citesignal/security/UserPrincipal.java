package com.citesignal.security;

import com.citesignal.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class UserPrincipal implements UserDetails {
    
    private Long id;
    private String email;
    private String password;
    private Collection<? extends GrantedAuthority> authorities;
    private Boolean active;
    private Boolean emailVerified;
    
    public UserPrincipal(Long id, String email, String password,
                        Collection<? extends GrantedAuthority> authorities,
                        Boolean active, Boolean emailVerified) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
        this.active = active;
        this.emailVerified = emailVerified;
    }
    
        public static UserPrincipal create(User user) {
        String roleName = user.getRole() != null ? user.getRole().name() : null;
        List<GrantedAuthority> authorities = roleName != null
            ? List.of(new SimpleGrantedAuthority("ROLE_" + roleName))
            : List.of();

        return new UserPrincipal(
            user.getId(),
            user.getEmail(),
            user.getPassword(),
            authorities,
            user.getActive(),
            user.getEmailVerified()
        );
        }
    
    public Long getId() {
        return id;
    }
    
    public String getEmail() {
        return email;
    }
    
    @Override
    public String getPassword() {
        return password;
    }
    
    @Override
    public String getUsername() {
        return email;
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return active != null && active && emailVerified != null && emailVerified;
    }
}

