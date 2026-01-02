package com.example.financeapp.security;

import com.example.financeapp.user.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {
    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    public User getUser() {
        return this.user;
    }

    public Long getUserId() {
        return user != null ? user.getUserId() : null;
    }

    public Long getId() {
        return user != null ? user.getUserId() : null;
    }

    public Role getRole() {
        return user != null && user.getRole() != null ? user.getRole() : Role.USER;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Role role = getRole();
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return user != null && !user.isLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user != null && user.isEnabled() && !user.isDeleted();
    }
}