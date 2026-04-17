package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import id.ac.ui.cs.advprog.donatjs.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    @Test
    void loadUserByUsername_found_returnsCorrectEmailAndPassword() {
        AppUser user = new AppUser();
        user.setEmail("user@test.com");
        user.setPassword("encodedPassword");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("user@test.com");

        assertEquals("user@test.com", details.getUsername());
        assertEquals("encodedPassword", details.getPassword());
    }

    @Test
    void loadUserByUsername_nullPassword_usesEmptyString() {
        AppUser user = new AppUser();
        user.setEmail("oauth@test.com");
        user.setPassword(null);
        when(userRepository.findByEmail("oauth@test.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("oauth@test.com");

        assertEquals("", details.getPassword());
    }

    @Test
    void loadUserByUsername_notFound_throwsUsernameNotFoundException() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("missing@test.com"));
    }
}
