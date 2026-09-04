package com.lokmit.foundation.security.bootstrap;

import com.lokmit.foundation.security.entity.User;
import com.lokmit.foundation.security.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Initializes the bootstrap administrator password on application startup.
 * Only sets the password if it is currently NULL and a bootstrap password is configured.
 */
@Component
public class BootstrapAdminInitializer {

    private static final Logger LOG = LoggerFactory.getLogger(BootstrapAdminInitializer.class);
    private static final String BOOTSTRAP_ADMIN_EMAIL = "admin@lokmitfoundation.org";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String bootstrapPassword;

    public BootstrapAdminInitializer(UserRepository userRepository,
                                     PasswordEncoder passwordEncoder,
                                     @Value("${app.security.bootstrap.admin-password:}") String bootstrapPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapPassword = bootstrapPassword;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeBootstrapAdmin() {
        if (bootstrapPassword == null || bootstrapPassword.isBlank()) {
            LOG.debug("BOOTSTRAP_ADMIN_PASSWORD not set - skipping bootstrap admin initialization");
            return;
        }

        userRepository.findByEmail(BOOTSTRAP_ADMIN_EMAIL).ifPresentOrElse(
                user -> {
                    if (user.getPasswordHash() == null) {
                        user.setPasswordHash(passwordEncoder.encode(bootstrapPassword));
                        userRepository.save(user);
                        LOG.info("Bootstrap admin password initialized successfully");
                    } else {
                        LOG.debug("Bootstrap admin already has a password - skipping initialization");
                    }
                },
                () -> LOG.warn("Bootstrap admin user not found: {}", BOOTSTRAP_ADMIN_EMAIL)
        );
    }
}