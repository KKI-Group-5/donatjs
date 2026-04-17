package id.ac.ui.cs.advprog.donatjs.config;

import id.ac.ui.cs.advprog.donatjs.exception.ProfileIncompleteException;
import id.ac.ui.cs.advprog.donatjs.exception.UserStatusException;
import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import id.ac.ui.cs.advprog.donatjs.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class UserStatusInterceptor implements HandlerInterceptor {

    private final UserRepository userRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Only intercept state-changing requests (POST, PUT, PATCH)
        String method = request.getMethod();
        if (!"POST".equalsIgnoreCase(method) && !"PUT".equalsIgnoreCase(method) && !"PATCH".equalsIgnoreCase(method)) {
            return true;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return true;
        }

        String email = getEmailFromPrincipal(auth.getPrincipal());
        if (email == null) return true;

        AppUser user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return true;

        // Block suspended users from all actions
        if (user.isSuspended()) {
            throw new UserStatusException("Action blocked: Your account is suspended. Please contact the administrator.");
        }

        // Block users with incomplete profiles from specific action modules
        if (user.getBio() == null || user.getDateOfBirth() == null) {
            // Allow exceptions for profile completion endpoints
            String uri = request.getRequestURI();
            if (uri.contains("/api/profile") || uri.contains("/api/auth")) {
                return true;
            }
            throw new ProfileIncompleteException("Action blocked: Please complete your profile (Bio and Date of Birth) before performing this action.");
        }

        return true;
    }

    private String getEmailFromPrincipal(Object principal) {
        if (principal instanceof OAuth2User oauth) {
            return oauth.getAttribute("email");
        } else if (principal instanceof UserDetails ud) {
            return ud.getUsername();
        }
        return null;
    }
}
