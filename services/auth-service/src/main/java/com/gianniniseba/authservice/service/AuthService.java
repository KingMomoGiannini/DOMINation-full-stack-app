package com.gianniniseba.authservice.service;

import com.gianniniseba.authservice.dto.AuthResponse;
import com.gianniniseba.authservice.dto.LoginRequest;
import com.gianniniseba.authservice.dto.RegisterRequest;
import com.gianniniseba.authservice.entity.Role;
import com.gianniniseba.authservice.entity.RoleName;
import com.gianniniseba.authservice.entity.User;
import com.gianniniseba.authservice.exception.InvalidCredentialsException;
import com.gianniniseba.authservice.exception.UserAlreadyExistsException;
import com.gianniniseba.authservice.repository.RoleRepository;
import com.gianniniseba.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService implements IAuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final OAuth2TokenService oAuth2TokenService;

    @Override
    public AuthResponse register(RegisterRequest request) {

        if(userRepository.existsByUsername(request.getUsername())){
            throw new UserAlreadyExistsException("El nombre de usuario ingresado ya se encuentra en uso.");
        }

        if(userRepository.existsByEmail(request.getEmail())){
            throw new UserAlreadyExistsException("El email ingresado ya se encuentra en uso.");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow( () -> new IllegalStateException("ROLE_USER no esta configurado en la base.") );

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(encodedPassword)
                .enabled(true)
                .roles(Set.of(userRole))
                .build();

        userRepository.save(user);

        return AuthResponse.builder()
                .message("Usuario registrado exitosamente.")
                .token(null)
                .build();

    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(
                        () -> new InvalidCredentialsException("Usuario o contraseña incorrectos.")
                );

        if(!passwordEncoder.matches(request.getPassword(), user.getPassword())){
            throw new InvalidCredentialsException("Usuario o contraseña incorrectos.");

        }

        String token = oAuth2TokenService.generateAccessToken(user);

        return AuthResponse.builder()
                .message("Login exitoso.")
                .token(token)
                .build();
    }
}
