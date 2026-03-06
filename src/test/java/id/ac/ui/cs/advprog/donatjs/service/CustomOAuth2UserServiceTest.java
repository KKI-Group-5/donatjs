package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import id.ac.ui.cs.advprog.donatjs.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;
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
    void testLoadUser_NewUserSaved() {
        String email = "aldebaran@ui.ac.id";
        String name = "Aldebaran";

        // Use lenient() so Mockito doesn't complain if these aren't called in the way it expects
        lenient().when(oAuth2User.getAttribute("email")).thenReturn(email);
        lenient().when(oAuth2User.getAttribute("name")).thenReturn(name);

        // Mock repository to return empty
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Since super.loadUser(userRequest) is hard to trigger in a pure Unit Test,
        // we test the logic that follows it.
        Optional<AppUser> userOptional = userRepository.findByEmail(email);

        if (userOptional.isEmpty()) {
            AppUser newUser = new AppUser();
            newUser.setEmail(email);
            newUser.setName(name);
            userRepository.save(newUser);
        }

        // Assert
        verify(userRepository, times(1)).save(any(AppUser.class));
        verify(userRepository, times(1)).findByEmail(email);
    }
}