package com.banking.auth.service;

import com.banking.auth.dto.SignupRequest;
import com.banking.auth.entity.ERole;
import com.banking.auth.entity.Role;
import com.banking.auth.entity.User;
import com.banking.auth.repository.RoleRepository;
import com.banking.auth.repository.UserRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @CircuitBreaker(name = "user-service", fallbackMethod = "createUserFallback")
    @Retry(name = "user-service")
    public User createUser(SignupRequest signUpRequest) {
        User user = new User(signUpRequest.getUsername(),
                           signUpRequest.getEmail(),
                           encoder.encode(signUpRequest.getPassword()),
                           signUpRequest.getFirstName(),
                           signUpRequest.getLastName());

        user.setPhoneNumber(signUpRequest.getPhoneNumber());

        Set<String> strRoles = signUpRequest.getRole();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null) {
            Role userRole = roleRepository.findByName(ERole.ROLE_CUSTOMER)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(userRole);
        } else {
            strRoles.forEach(role -> {
                switch (role) {
                    case "admin":
                        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(adminRole);
                        break;
                    case "manager":
                        Role managerRole = roleRepository.findByName(ERole.ROLE_MANAGER)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(managerRole);
                        break;
                    case "employee":
                        Role employeeRole = roleRepository.findByName(ERole.ROLE_EMPLOYEE)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(employeeRole);
                        break;
                    default:
                        Role userRole = roleRepository.findByName(ERole.ROLE_CUSTOMER)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(userRole);
                }
            });
        }

        user.setRoles(roles);
        User savedUser = userRepository.save(user);

        // Send user creation event to Kafka
        sendUserCreatedEvent(savedUser);

        return savedUser;
    }

    public User createUserFallback(SignupRequest signUpRequest, Exception ex) {
        logger.error("Failed to create user: {}", ex.getMessage());
        throw new RuntimeException("User creation service is currently unavailable");
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public Boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    private void sendUserCreatedEvent(User user) {
        try {
            UserCreatedEvent event = new UserCreatedEvent(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName()
            );
            kafkaTemplate.send("user-events", event);
            logger.info("User created event sent for user: {}", user.getUsername());
        } catch (Exception e) {
            logger.error("Failed to send user created event: {}", e.getMessage());
        }
    }

    // Inner class for Kafka event
    public static class UserCreatedEvent {
        private Long userId;
        private String username;
        private String email;
        private String firstName;
        private String lastName;

        public UserCreatedEvent(Long userId, String username, String email, String firstName, String lastName) {
            this.userId = userId;
            this.username = username;
            this.email = email;
            this.firstName = firstName;
            this.lastName = lastName;
        }

        // Getters and setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
    }
}
