package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.dto.RegisterRequest;
import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import id.ac.ui.cs.advprog.donatjs.repository.UserRepository;
import id.ac.ui.cs.advprog.donatjs.repository.VerificationTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private VerificationTokenRepository tokenRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new RegisterRequest();
        validRequest.setEmail("aldebaran@ui.ac.id");
        validRequest.setPassword("securepassword");
        validRequest.setName("Aldebaran");
        validRequest.setBio("Software developer");
        validRequest.setDateOfBirth(LocalDate.of(2000, 1, 1));
    }

    @Test
    void registerUser_Success_SavesAndReturnsUser() {
        when(userRepository.findByEmail(validRequest.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(validRequest.getPassword())).thenReturn("encoded-password");

        AppUser savedUser = new AppUser();
        savedUser.setEmail(validRequest.getEmail());
        savedUser.setName(validRequest.getName());
        when(userRepository.save(any(AppUser.class))).thenReturn(savedUser);

        AppUser result = authService.registerUser(validRequest);

        assertNotNull(result);
        assertEquals(validRequest.getEmail(), result.getEmail());
        verify(passwordEncoder, times(1)).encode(validRequest.getPassword());
        verify(userRepository, times(1)).save(any(AppUser.class));
    }

    @Test
    void registerUser_DuplicateEmail_ThrowsRuntimeException() {
        AppUser existingUser = new AppUser();
        existingUser.setEmail(validRequest.getEmail());
        when(userRepository.findByEmail(validRequest.getEmail())).thenReturn(Optional.of(existingUser));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.registerUser(validRequest));

        assertEquals("Email is already registered", exception.getMessage());
        verify(userRepository, never()).save(any(AppUser.class));
    }

    @Test
    void registerUser_PasswordIsEncoded_NotStoredAsPlaintext() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("securepassword")).thenReturn("$2a$10$hashedvalue");

        AppUser savedUser = new AppUser();
        savedUser.setPassword("$2a$10$hashedvalue");
        when(userRepository.save(any(AppUser.class))).thenReturn(savedUser);

        AppUser result = authService.registerUser(validRequest);

        verify(passwordEncoder, times(1)).encode("securepassword");
        assertNotEquals("securepassword", result.getPassword());
    }
}
