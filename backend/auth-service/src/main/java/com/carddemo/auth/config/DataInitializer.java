package com.carddemo.auth.config;

import com.carddemo.auth.repository.UserRepository;
import com.carddemo.common.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByUserId("admin")) {
            User adminUser = User.builder()
                    .userId("admin")
                    .firstName("Admin")
                    .lastName("User")
                    .passwordHash(passwordEncoder.encode("Admin@123"))
                    .userType("A")
                    .isActive(true)
                    .build();
            
            userRepository.save(adminUser);
            log.info("Default admin user created: admin / Admin@123");
        }
        
        if (!userRepository.existsByUserId("user")) {
            User regularUser = User.builder()
                    .userId("user")
                    .firstName("Regular")
                    .lastName("User")
                    .passwordHash(passwordEncoder.encode("User@123"))
                    .userType("U")
                    .isActive(true)
                    .build();
            
            userRepository.save(regularUser);
            log.info("Default regular user created: user / User@123");
        }
    }
}
