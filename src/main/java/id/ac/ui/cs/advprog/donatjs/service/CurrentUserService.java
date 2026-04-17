package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import id.ac.ui.cs.advprog.donatjs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public AppUser getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null
                || authentication.getPrincipal() instanceof String principal && "anonymousUser".equals(principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        String email = extractEmail(authentication);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    public String getCurrentUserId(Authentication authentication) {
        return getCurrentUser(authentication).getId().toString();
    }

    public String getCurrentUserEmail(Authentication authentication) {
        return getCurrentUser(authentication).getEmail();
    }

    private String extractEmail(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof OAuth2User oauth2User) {
            String email = oauth2User.getAttribute("email");
            if (email != null) {
                return email;
            }
        }
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return authentication.getName();
    }
}
