package id.ac.ui.cs.advprog.donatjs.config;

import id.ac.ui.cs.advprog.donatjs.service.CurrentUserService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Publishes a small bag of auth-related attributes on every MVC view so
 * templates can render e.g. the logged-in user's id without peppering every
 * controller with the same lookup. The service is injected via
 * {@link ObjectProvider} so that MVC slice tests ({@code @WebMvcTest}) don't
 * have to wire a {@link CurrentUserService} bean just to render a template.
 */
@ControllerAdvice
public class GlobalModelAttributes {

    private final ObjectProvider<CurrentUserService> currentUserServiceProvider;

    public GlobalModelAttributes(ObjectProvider<CurrentUserService> currentUserServiceProvider) {
        this.currentUserServiceProvider = currentUserServiceProvider;
    }

    @ModelAttribute
    public void addAuthAttributes(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() instanceof String) {
            model.addAttribute("isLoggedIn", false);
            model.addAttribute("currentUserName", "User");
            model.addAttribute("currentUserId", null);
            return;
        }

        model.addAttribute("isLoggedIn", true);

        String name = null;
        String currentUserId = null;

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails ud) {
            name = ud.getUsername();
        } else if (principal instanceof OAuth2User oauth) {
            name = oauth.getAttribute("name");
            if (name == null) {
                name = oauth.getAttribute("email");
            }
        }

        CurrentUserService currentUserService = currentUserServiceProvider.getIfAvailable();
        if (currentUserService != null) {
            try {
                currentUserId = currentUserService.getCurrentUserId(authentication);
            } catch (Exception ignored) {
                currentUserId = null;
            }
        }

        model.addAttribute("currentUserName", name != null ? name : "User");
        model.addAttribute("currentUserId", currentUserId);
    }
}
