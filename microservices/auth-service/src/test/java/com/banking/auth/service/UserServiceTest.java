package com.banking.auth.service;

import com.banking.auth.dto.SignupRequest;
import com.banking.auth.entity.ERole;
import com.banking.auth.entity.Role;
import com.banking.auth.entity.User;
import com.banking.auth.repository.RoleRepository;
import com.banking.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private UserService userService;

    private SignupRequest signupRequest;
    private Role customerRole;

    @BeforeEach
    void setUp() {
        signupRequest = new SignupRequest();
        signupRequest.setUsername("testuser");
        signupRequest.setEmail("test@example.com");
        signupRequest.setPassword("password123");
        signupRequest.setFirstName("Test");
        signupRequest.setLastName("User");

        customerRole = new Role(ERole.ROLE_CUSTOMER);
    }

    @Test
    void testCreateUserSuccess() {
        // Given
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(roleRepository.findByName(ERole.ROLE_CUSTOMER)).thenReturn(Optional.of(customerRole));
        
        User savedUser = new User("testuser", "test@example.com", "encoded-password", "Test", "User");
        savedUser.setId(1L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When
        User result = userService.createUser(signupRequest);

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        verify(userRepository).save(any(User.class));
        verify(kafkaTemplate).send(eq("user-events"), any());
    }

    @Test
    void testExistsByUsername() {
        // Given
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        // When
        Boolean exists = userService.existsByUsername("testuser");

        // Then
        assertTrue(exists);
        verify(userRepository).existsByUsername("testuser");
    }

    @Test
    void testExistsByEmail() {
        // Given
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // When
        Boolean exists = userService.existsByEmail("test@example.com");

        // Then
        assertTrue(exists);
        verify(userRepository).existsByEmail("test@example.com");
    }

    @Test
    void testFindByUsername() {
        // Given
        User user = new User("testuser", "test@example.com", "password", "Test", "User");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        // When
        Optional<User> result = userService.findByUsername("testuser");

        // Then
        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
        verify(userRepository).findByUsername("testuser");
    }
}
