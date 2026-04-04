package com.gianniniseba.authservice;

import com.gianniniseba.authservice.entity.Role;
import com.gianniniseba.authservice.entity.RoleName;
import com.gianniniseba.authservice.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableMethodSecurity
public class AuthServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthServiceApplication.class, args);
	}

    @Bean
    public CommandLineRunner initRoles(RoleRepository roleRepository){
        return args ->{
            if(roleRepository.findByName(RoleName.ROLE_ADMIN).isEmpty()){
                roleRepository.save(Role.builder().name(RoleName.ROLE_ADMIN).build());
            }

            if (roleRepository.findByName(RoleName.ROLE_USER).isEmpty()){
                roleRepository.save(Role.builder().name(RoleName.ROLE_USER).build());
            }
        };
    }

}
