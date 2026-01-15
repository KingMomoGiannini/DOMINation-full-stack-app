package com.gianniniseba.authservice.service;

import com.gianniniseba.authservice.entity.ProviderRequest;
import com.gianniniseba.authservice.entity.Role;
import com.gianniniseba.authservice.entity.RoleName;
import com.gianniniseba.authservice.entity.User;
import com.gianniniseba.authservice.repository.ProviderRequestRepository;
import com.gianniniseba.authservice.repository.RoleRepository;
import com.gianniniseba.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProviderRequestService {

    private final ProviderRequestRepository providerRequestRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Transactional
    public ProviderRequest createRequest(Long userId) {
        if (providerRequestRepository.existsByUserIdAndStatus(userId, ProviderRequest.RequestStatus.PENDING)) {
            throw new IllegalStateException("Ya tienes una solicitud pendiente");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));

        boolean hasProviderRole = user.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.ROLE_PROVIDER);

        if (hasProviderRole) {
            throw new IllegalStateException("Ya eres prestador");
        }

        ProviderRequest request = ProviderRequest.builder()
                .userId(userId)
                .status(ProviderRequest.RequestStatus.PENDING)
                .build();

        return providerRequestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public ProviderRequest getMyRequest(Long userId) {
        return providerRequestRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<ProviderRequest> getRequestsByStatus(ProviderRequest.RequestStatus status) {
        if (status == null) {
            return providerRequestRepository.findAll();
        }
        return providerRequestRepository.findByStatus(status);
    }

    @Transactional
    public ProviderRequest approveRequest(Long requestId) {
        ProviderRequest request = providerRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalStateException("Solicitud no encontrada"));

        if (request.getStatus() != ProviderRequest.RequestStatus.PENDING) {
            throw new IllegalStateException("La solicitud ya fue procesada");
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));

        Role providerRole = roleRepository.findByName(RoleName.ROLE_PROVIDER)
                .orElseThrow(() -> new IllegalStateException("ROLE_PROVIDER no configurado"));

        user.getRoles().add(providerRole);
        userRepository.save(user);

        request.setStatus(ProviderRequest.RequestStatus.APPROVED);
        return providerRequestRepository.save(request);
    }

    @Transactional
    public ProviderRequest rejectRequest(Long requestId) {
        ProviderRequest request = providerRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalStateException("Solicitud no encontrada"));

        if (request.getStatus() != ProviderRequest.RequestStatus.PENDING) {
            throw new IllegalStateException("La solicitud ya fue procesada");
        }

        request.setStatus(ProviderRequest.RequestStatus.REJECTED);
        return providerRequestRepository.save(request);
    }
}

