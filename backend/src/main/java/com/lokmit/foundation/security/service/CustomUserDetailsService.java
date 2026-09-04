package com.lokmit.foundation.security.service;

import com.lokmit.foundation.security.entity.Permission;
import com.lokmit.foundation.security.entity.Role;
import com.lokmit.foundation.security.entity.User;
import com.lokmit.foundation.security.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Spring Security UserDetailsService that loads users from the database.
 * Maps roles to ROLE_ authorities and permissions to permission authorities.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger LOG = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    LOG.debug("User not found: {}", email);
                    return new UsernameNotFoundException("Invalid email or password");
                });

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash() != null ? user.getPasswordHash() : "",
                true, // enabled
                true, // accountNonExpired
                true, // credentialsNonExpired
                !isAccountLocked(user.getStatus()),
                getAuthorities(user)
        );
    }

    /**
     * Determines if the account is locked based on user status.
     */
    private boolean isAccountLocked(String status) {
        return "LOCKED".equals(status) || "SUSPENDED".equals(status) || "DELETED".equals(status);
    }

    /**
     * Gets the authorities (roles and permissions) for a user.
     * Roles are prefixed with "ROLE_" for Spring Security compatibility.
     */
    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        for (Role role : user.getRoles()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getCode()));
            for (Permission permission : role.getPermissions()) {
                authorities.add(new SimpleGrantedAuthority(permission.getCode()));
            }
        }

        return authorities;
    }
}