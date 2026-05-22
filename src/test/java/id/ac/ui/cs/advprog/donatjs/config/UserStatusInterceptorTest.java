package id.ac.ui.cs.advprog.donatjs.config;

import id.ac.ui.cs.advprog.donatjs.exception.ProfileIncompleteException;
import id.ac.ui.cs.advprog.donatjs.exception.UserStatusException;
import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import id.ac.ui.cs.advprog.donatjs.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class UserStatusInterceptorTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserStatusInterceptor interceptor;

    private AppUser appUser;

    @BeforeEach
    void setUp() {
        appUser = new AppUser();
        appUser.setEmail("test@example.com");
        appUser.setBio("A valid bio");
        appUser.setDateOfBirth(java.time.LocalDate.of(2000, 1, 1));
        appUser.setSuspended(false);
    }

    private void mockAuthentication(String email, boolean isOAuth) {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        
        if (isOAuth) {
            OAuth2User oauth2User = mock(OAuth2User.class);
            when(oauth2User.getAttribute("email")).thenReturn(email);
            when(authentication.getPrincipal()).thenReturn(oauth2User);
        } else {
            User userDetails = new User(email, "password", java.util.Collections.emptyList());
            when(authentication.getPrincipal()).thenReturn(userDetails);
        }
    }

    @Test
    void preHandle_StaticAssets_ReturnsTrue() throws Exception {
        when(request.getRequestURI()).thenReturn("/css/style.css");
        assertTrue(interceptor.preHandle(request, response, new Object()));
        verifyNoInteractions(securityContext);
    }

    @Test
    void preHandle_NotAuthenticated_ReturnsTrue() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/campaigns");
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(null);
        assertTrue(interceptor.preHandle(request, response, new Object()));
    }

    @Test
    void preHandle_AnonymousUser_ReturnsTrue() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/campaigns");
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("anonymousUser");
        assertTrue(interceptor.preHandle(request, response, new Object()));
    }

    @Test
    void preHandle_ValidUser_OAuth_ReturnsTrue() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/campaigns");
        when(request.getMethod()).thenReturn("GET");
        mockAuthentication("test@example.com", true);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(appUser));

        assertTrue(interceptor.preHandle(request, response, new Object()));
    }

    @Test
    void preHandle_ValidUser_UserDetails_ReturnsTrue() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/campaigns");
        when(request.getMethod()).thenReturn("GET");
        mockAuthentication("test@example.com", false);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(appUser));

        assertTrue(interceptor.preHandle(request, response, new Object()));
    }

    @Test
    void preHandle_SuspendedUser_StateChangingMethod_ThrowsException() throws Exception {
        appUser.setSuspended(true);
        when(request.getRequestURI()).thenReturn("/api/campaigns");
        when(request.getMethod()).thenReturn("POST");
        mockAuthentication("test@example.com", false);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(appUser));

        assertThrows(UserStatusException.class, () -> interceptor.preHandle(request, response, new Object()));
    }

    @Test
    void preHandle_SuspendedUser_NonStateChangingMethod_ReturnsTrue() throws Exception {
        appUser.setSuspended(true);
        when(request.getRequestURI()).thenReturn("/api/campaigns");
        when(request.getMethod()).thenReturn("GET");
        mockAuthentication("test@example.com", false);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(appUser));

        assertTrue(interceptor.preHandle(request, response, new Object()));
    }

    @Test
    void preHandle_IncompleteProfile_ExemptUri_ReturnsTrue() throws Exception {
        appUser.setBio(null);
        when(request.getRequestURI()).thenReturn("/profile");
        when(request.getMethod()).thenReturn("GET");
        mockAuthentication("test@example.com", false);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(appUser));

        assertTrue(interceptor.preHandle(request, response, new Object()));
    }

    @Test
    void preHandle_IncompleteProfile_NonApiGetRequest_Redirects() throws Exception {
        appUser.setBio(null);
        when(request.getRequestURI()).thenReturn("/campaigns");
        when(request.getMethod()).thenReturn("GET");
        mockAuthentication("test@example.com", false);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(appUser));

        assertFalse(interceptor.preHandle(request, response, new Object()));
        verify(response).sendRedirect("/profile?incomplete=true");
    }

    @Test
    void preHandle_IncompleteProfile_StateChangingRequest_ThrowsException() throws Exception {
        appUser.setBio("");
        when(request.getRequestURI()).thenReturn("/api/campaigns");
        when(request.getMethod()).thenReturn("POST");
        mockAuthentication("test@example.com", false);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(appUser));

        assertThrows(ProfileIncompleteException.class, () -> interceptor.preHandle(request, response, new Object()));
    }
}
