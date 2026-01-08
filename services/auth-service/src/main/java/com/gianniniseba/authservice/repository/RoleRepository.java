package com.gianniniseba.authservice.repository;

import com.gianniniseba.authservice.entity.Role;
import com.gianniniseba.authservice.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);
}
