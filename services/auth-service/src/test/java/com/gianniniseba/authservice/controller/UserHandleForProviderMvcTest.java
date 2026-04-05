package com.gianniniseba.authservice.controller;

import com.gianniniseba.authservice.entity.Role;
import com.gianniniseba.authservice.entity.User;
import com.gianniniseba.authservice.entity.RoleName;
import com.gianniniseba.authservice.repository.RoleRepository;
import com.gianniniseba.authservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contexto MVC acotado: el {@link com.gianniniseba.authservice.AuthServiceApplication} declara
 * {@code CommandLineRunner initRoles}; en slice se debe proveer {@link RoleRepository}.
 */
@WebMvcTest(controllers = UserController.class)
@ActiveProfiles("test")
class UserHandleForProviderMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private RoleRepository roleRepository;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void stubs() {
        lenient().when(roleRepository.findByName(any(RoleName.class))).thenReturn(Optional.empty());
        lenient().when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(jwtDecoder.decode(anyString())).thenAnswer(invocation -> {
            String token = invocation.getArgument(0);
            return Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .claim("sub", "provider-test")
                    .claim("authorities", "ROLE_PROVIDER")
                    .build();
        });
    }

    @Test
    void handleForProvider_returnsUsername_whenProvider() throws Exception {
        User user = User.builder()
                .id(99L)
                .username("alice")
                .email("alice@test.com")
                .password("x")
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(userRepository.findById(99L)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/users/99/handle-for-provider")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                        .with(jwt().authorities(() -> "ROLE_PROVIDER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(99))
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void handleForProvider_forbidden_whenNotProvider() throws Exception {
        mockMvc.perform(get("/users/99/handle-for-provider")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                        .with(jwt().authorities(() -> "ROLE_CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void handleForProvider_notFound_whenUserMissing() throws Exception {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/users/99/handle-for-provider")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                        .with(jwt().authorities(() -> "ROLE_PROVIDER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void handleForProvider_badRequest_whenUserIdNotPositive() throws Exception {
        mockMvc.perform(get("/users/0/handle-for-provider")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                        .with(jwt().authorities(() -> "ROLE_PROVIDER")))
                .andExpect(status().isBadRequest());
    }
}
