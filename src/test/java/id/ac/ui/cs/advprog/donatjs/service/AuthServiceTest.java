package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.dto.RegisterRequest;
import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import id.ac.ui.cs.advprog.donatjs.repository.UserRepository;
import id.ac.ui.cs.advprog.donatjs.repository.VerificationTokenRepository;
import id.ac.ui.cs.advprog.donatjs.model.VerificationToken;
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
        savedUser.setVerified(true);
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppUser result = authService.registerUser(validRequest);

        assertNotNull(result);
        assertEquals(validRequest.getEmail(), result.getEmail());
        assertTrue(result.isVerified());
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

    @Test
    void verifyEmail_ValidToken_ReturnsTrueAndSetsVerified() {
        AppUser user = new AppUser();
        user.setVerified(false);
        VerificationToken token = new VerificationToken("valid-token", user);
        when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));

        boolean result = authService.verifyEmail("valid-token");

        assertTrue(result);
        assertTrue(user.isVerified());
        verify(userRepository, times(1)).save(user);
        verify(tokenRepository, times(1)).delete(token);
    }

    @Test
    void verifyEmail_ExpiredToken_ReturnsFalseAndDoesNotVerify() {
        AppUser user = new AppUser();
        user.setVerified(false);
        VerificationToken token = new VerificationToken("expired-token", user);
        token.setExpiryDate(java.time.LocalDateTime.now().minusHours(1));
        when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        boolean result = authService.verifyEmail("expired-token");

        assertFalse(result);
        assertFalse(user.isVerified());
        verify(userRepository, never()).save(any());
        verify(tokenRepository, never()).delete(any());
    }

    @Test
    void verifyEmail_TokenNotFound_ReturnsFalse() {
        when(tokenRepository.findByToken("non-existent")).thenReturn(Optional.empty());

        boolean result = authService.verifyEmail("non-existent");

        assertFalse(result);
        verify(userRepository, never()).save(any());
        verify(tokenRepository, never()).delete(any());
    }
}
