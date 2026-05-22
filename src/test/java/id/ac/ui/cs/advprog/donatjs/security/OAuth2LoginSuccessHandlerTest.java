package id.ac.ui.cs.advprog.donatjs.security;

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
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class OAuth2LoginSuccessHandlerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    @Mock
    private OAuth2User oauthUser;

    @InjectMocks
    private OAuth2LoginSuccessHandler successHandler;

    @BeforeEach
    void setUp() {
        when(authentication.getPrincipal()).thenReturn(oauthUser);
    }

    @Test
    void onAuthenticationSuccess_IncompleteProfile_RedirectsToProfile() throws Exception {
        when(oauthUser.getAttribute("email")).thenReturn("test@gmail.com");
        
        AppUser user = new AppUser();
        user.setEmail("test@gmail.com");
        user.setBio(null); // Incomplete
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(user));

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect("/profile?incomplete=true");
    }

    @Test
    void onAuthenticationSuccess_CompleteProfile_RedirectsToHome() throws Exception {
        when(oauthUser.getAttribute("email")).thenReturn("test@gmail.com");

        AppUser user = new AppUser();
        user.setEmail("test@gmail.com");
        user.setBio("A bio");
        user.setDateOfBirth(java.time.LocalDate.now());
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(user));

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect("/");
    }

    @Test
    void onAuthenticationSuccess_UserNotFound_RedirectsToHome() throws Exception {
        when(oauthUser.getAttribute("email")).thenReturn("test@gmail.com");
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.empty());

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect("/");
    }
}
