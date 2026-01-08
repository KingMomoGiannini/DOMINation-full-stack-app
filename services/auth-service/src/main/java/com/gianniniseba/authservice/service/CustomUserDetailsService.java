package com.gianniniseba.authservice.service;

import com.gianniniseba.authservice.entity.User;
import com.gianniniseba.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

                User user = userRepository.findByUsername(username)
                        .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado " + username));

                List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                        .toList();

                return new org.springframework.security.core.userdetails.User(
                        user.getUsername(),
                        user.getPassword(),
                        user.isEnabled(),
                        true,
                        true,
                        true,
                        authorities
                );

    }
}
