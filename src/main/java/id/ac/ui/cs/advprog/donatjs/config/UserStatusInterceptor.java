package id.ac.ui.cs.advprog.donatjs.config;

import id.ac.ui.cs.advprog.donatjs.exception.ProfileIncompleteException;
import id.ac.ui.cs.advprog.donatjs.exception.UserStatusException;
import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import id.ac.ui.cs.advprog.donatjs.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
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
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        
        // Skip static assets
        if (uri.startsWith("/css/") || uri.startsWith("/js/") || uri.startsWith("/images/")) {
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

        boolean isStateChanging = "POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method);

        // Block suspended users from state-changing actions
        if (user.isSuspended() && isStateChanging) {
            throw new UserStatusException("Action blocked: Your account is suspended. Please contact the administrator.");
        }

        return handleIncompleteProfile(user, uri, method, response, isStateChanging);
    }

    private boolean handleIncompleteProfile(AppUser user, String uri, String method, HttpServletResponse response, boolean isStateChanging) throws java.io.IOException {
        if (user.getBio() == null || user.getDateOfBirth() == null || user.getBio().isBlank()) {
            if (isExemptUri(uri)) {
                return true;
            }
            if ("GET".equalsIgnoreCase(method) && !uri.startsWith("/api/")) {
                response.sendRedirect("/profile?incomplete=true");
                return false;
            }
            if (isStateChanging) {
                throw new ProfileIncompleteException("Action blocked: Please complete your profile (Bio and Date of Birth) before performing this action.");
            }
        }
        return true;
    }

    private boolean isExemptUri(String uri) {
        return uri.equals("/profile") || uri.startsWith("/api/profile") || uri.startsWith("/api/auth") || uri.equals("/login") || uri.equals("/logout") || uri.equals("/error") || uri.equals("/register");
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
