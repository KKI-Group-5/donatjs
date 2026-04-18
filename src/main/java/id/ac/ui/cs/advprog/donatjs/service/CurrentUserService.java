package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import id.ac.ui.cs.advprog.donatjs.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

/**
 * Central place for resolving the currently-authenticated {@link AppUser}.
 *
 * <p>Every controller that needs the "who am I?" user used to re-implement
 * this; now they can inject this service instead and stay DRY.</p>
 */
@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<AppUser> findCurrentUser() {
        return findCurrentUser(SecurityContextHolder.getContext().getAuthentication());
    }

    public Optional<AppUser> findCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof String) {
            return Optional.empty();
        }
        String email = null;
        if (principal instanceof OAuth2User oauth) {
            email = oauth.getAttribute("email");
        } else if (principal instanceof UserDetails ud) {
            email = ud.getUsername();
        } else {
            email = authentication.getName();
        }
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByEmail(email);
    }

    public AppUser requireCurrentUser() {
        return findCurrentUser()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
    }

    public String requireCurrentUserId() {
        return requireCurrentUser().getId().toString();
    }
}
