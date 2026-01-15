package com.gianniniseba.authservice.config;

import com.gianniniseba.authservice.entity.Role;
import com.gianniniseba.authservice.entity.RoleName;
import com.gianniniseba.authservice.entity.User;
import com.gianniniseba.authservice.repository.RoleRepository;
import com.gianniniseba.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * DataSeeder para inicializar la base de datos con roles y usuario administrador
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedRoles();
        seedAdminUser();
        seedProviderUser();
    }

    /**
     * Crea los roles básicos si no existen
     */
    private void seedRoles() {
        log.info("🌱 Verificando roles...");

        // Crear ROLE_ADMIN
        if (roleRepository.findByName(RoleName.ROLE_ADMIN).isEmpty()) {
            Role adminRole = Role.builder()
                    .name(RoleName.ROLE_ADMIN)
                    .build();
            roleRepository.save(adminRole);
            log.info("✅ Rol ROLE_ADMIN creado");
        } else {
            log.info("ℹ️ Rol ROLE_ADMIN ya existe");
        }

        // Crear ROLE_USER
        if (roleRepository.findByName(RoleName.ROLE_USER).isEmpty()) {
            Role userRole = Role.builder()
                    .name(RoleName.ROLE_USER)
                    .build();
            roleRepository.save(userRole);
            log.info("✅ Rol ROLE_USER creado");
        } else {
            log.info("ℹ️ Rol ROLE_USER ya existe");
        }

        // Crear ROLE_PROVIDER
        if (roleRepository.findByName(RoleName.ROLE_PROVIDER).isEmpty()) {
            Role providerRole = Role.builder()
                    .name(RoleName.ROLE_PROVIDER)
                    .build();
            roleRepository.save(providerRole);
            log.info("✅ Rol ROLE_PROVIDER creado");
        } else {
            log.info("ℹ️ Rol ROLE_PROVIDER ya existe");
        }
    }

    /**
     * Crea el usuario administrador si no existe
     */
    private void seedAdminUser() {
        log.info("🌱 Verificando usuario administrador...");

        String adminUsername = "adminSeba";
        String adminPassword = "123456admin";
        String adminEmail = "admin@domination.com";

        if (userRepository.findByUsername(adminUsername).isEmpty()) {
            Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                    .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN no está configurado en la base"));

            Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                    .orElseThrow(() -> new IllegalStateException("ROLE_USER no está configurado en la base"));

            User adminUser = User.builder()
                    .username(adminUsername)
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .enabled(true)
                    .roles(Set.of(adminRole, userRole))
                    .build();

            userRepository.save(adminUser);
            log.info("✅ Usuario administrador creado:");
            log.info("   👤 Username: {}", adminUsername);
            log.info("   📧 Email: {}", adminEmail);
            log.info("   🔑 Password: {}", adminPassword);
            log.info("   ⚠️ IMPORTANTE: Cambia esta contraseña en producción");
        } else {
            log.info("ℹ️ Usuario administrador '{}' ya existe", adminUsername);
        }
    }

    /**
     * Crea el usuario provider demo si no existe
     */
    private void seedProviderUser() {
        log.info("🌱 Verificando usuario provider demo...");

        String providerUsername = "providerDemo";
        String providerPassword = "provider123";
        String providerEmail = "provider@domination.com";

        if (userRepository.findByUsername(providerUsername).isEmpty()) {
            Role providerRole = roleRepository.findByName(RoleName.ROLE_PROVIDER)
                    .orElseThrow(() -> new IllegalStateException("ROLE_PROVIDER no está configurado en la base"));

            Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                    .orElseThrow(() -> new IllegalStateException("ROLE_USER no está configurado en la base"));

            User providerUser = User.builder()
                    .username(providerUsername)
                    .email(providerEmail)
                    .password(passwordEncoder.encode(providerPassword))
                    .enabled(true)
                    .roles(Set.of(providerRole, userRole))
                    .build();

            userRepository.save(providerUser);
            log.info("✅ Usuario provider demo creado:");
            log.info("   👤 Username: {}", providerUsername);
            log.info("   📧 Email: {}", providerEmail);
            log.info("   🔑 Password: {}", providerPassword);
            log.info("   🎭 Roles: ROLE_PROVIDER, ROLE_USER");
            log.info("   ⚠️ IMPORTANTE: Solo para desarrollo");
        } else {
            log.info("ℹ️ Usuario provider '{}' ya existe", providerUsername);
        }
    }
}

