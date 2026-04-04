package com.gianniniseba.authservice.controller;

import com.gianniniseba.authservice.dto.UserHandleForProviderResponse;
import com.gianniniseba.authservice.dto.UserResponse;
import com.gianniniseba.authservice.entity.User;
import com.gianniniseba.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    public UserResponse getCurrentUser(Authentication authentication){
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado " + username));

        Set<String> roleNames = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(roleNames)
                .build();
    }

    /**
     * Permite a un prestador obtener el nombre de usuario público de un cliente por id.
     * No expone email. La autorización es por rol PROVIDER (vía configuración de seguridad).
     */
    @GetMapping("/{userId}/handle-for-provider")
    @PreAuthorize("hasRole('PROVIDER')")
    public UserHandleForProviderResponse getHandleForProvider(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        return UserHandleForProviderResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .build();
    }

}
