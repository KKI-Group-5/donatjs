package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import id.ac.ui.cs.advprog.donatjs.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CurrentUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CurrentUserService currentUserService;

    private AppUser mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new AppUser();
        mockUser.setId(java.util.UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        mockUser.setEmail("test@test.com");
    }

    @Test
    void testRequireCurrentUser() {
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(authentication.getPrincipal()).thenReturn("user");
        when(authentication.getName()).thenReturn("test@test.com");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(mockUser));

        AppUser result = currentUserService.requireCurrentUser();
        assertEquals(java.util.UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), result.getId());
    }

    @Test
    void testRequireCurrentUserId() {
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(authentication.getPrincipal()).thenReturn("user");
        when(authentication.getName()).thenReturn("test@test.com");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(mockUser));

        String id = currentUserService.requireCurrentUserId();
        assertEquals("123e4567-e89b-12d3-a456-426614174000", id);
    }

    @Test
    void testGetCurrentUserUnauthenticated() {
        assertThrows(ResponseStatusException.class, () -> currentUserService.getCurrentUser(null));
        when(authentication.getPrincipal()).thenReturn(null);
        assertThrows(ResponseStatusException.class, () -> currentUserService.getCurrentUser(authentication));
        when(authentication.getPrincipal()).thenReturn("anonymousUser");
        assertThrows(ResponseStatusException.class, () -> currentUserService.getCurrentUser(authentication));
    }

    @Test
    void testGetCurrentUserUserNotFound() {
        when(authentication.getPrincipal()).thenReturn("user");
        when(authentication.getName()).thenReturn("test@test.com");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> currentUserService.getCurrentUser(authentication));
    }

    @Test
    void testGetCurrentUserId() {
        when(authentication.getPrincipal()).thenReturn("user");
        when(authentication.getName()).thenReturn("test@test.com");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(mockUser));

        String id = currentUserService.getCurrentUserId(authentication);
        assertEquals("123e4567-e89b-12d3-a456-426614174000", id);
    }

    @Test
    void testGetCurrentUserEmail() {
        when(authentication.getPrincipal()).thenReturn("user");
        when(authentication.getName()).thenReturn("test@test.com");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(mockUser));

        String email = currentUserService.getCurrentUserEmail(authentication);
        assertEquals("test@test.com", email);
    }

    @Test
    void testExtractEmailFromOAuth2User() {
        OAuth2User oauth2User = mock(OAuth2User.class);
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttribute("email")).thenReturn("oauth@test.com");
        when(userRepository.findByEmail("oauth@test.com")).thenReturn(Optional.of(mockUser));

        AppUser result = currentUserService.getCurrentUser(authentication);
        assertNotNull(result);
    }

    @Test
    void testExtractEmailFromUserDetails() {
        UserDetails userDetails = mock(UserDetails.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("details@test.com");
        when(userRepository.findByEmail("details@test.com")).thenReturn(Optional.of(mockUser));

        AppUser result = currentUserService.getCurrentUser(authentication);
        assertNotNull(result);
    }
}
