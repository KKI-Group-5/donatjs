package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import id.ac.ui.cs.advprog.donatjs.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CustomOAuth2UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OAuth2User oAuth2User;

    @InjectMocks
    private CustomOAuth2UserService customOAuth2UserService;

    @Test
    void testProcessOAuth2User_NewUserSaved() {
        String email = "aldebaran@ui.ac.id";
        String name = "Aldebaran";

        when(oAuth2User.getAttribute("email")).thenReturn(email);
        when(oAuth2User.getAttribute("name")).thenReturn(name);
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act
        customOAuth2UserService.processOAuth2User(oAuth2User);

        // Assert
        verify(userRepository, times(1)).save(any(AppUser.class));
        verify(userRepository, times(1)).findByEmail(email);
    }

    @Test
    void testProcessOAuth2User_ExistingUserNotSaved() {
        String email = "existing@ui.ac.id";
        when(oAuth2User.getAttribute("email")).thenReturn(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(new AppUser()));

        // Act
        customOAuth2UserService.processOAuth2User(oAuth2User);

        // Assert
        verify(userRepository, never()).save(any(AppUser.class));
    }

    @Test
    void testProcessOAuth2User_MissingEmailThrowsException() {
        when(oAuth2User.getAttribute("email")).thenReturn(null);

        // Act & Assert
        assertThrows(OAuth2AuthenticationException.class, () -> {
            customOAuth2UserService.processOAuth2User(oAuth2User);
        });
    }
}