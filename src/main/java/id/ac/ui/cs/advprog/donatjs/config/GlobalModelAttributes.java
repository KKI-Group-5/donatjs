package id.ac.ui.cs.advprog.donatjs.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    @ModelAttribute
    public void addAuthAttributes(Authentication authentication, Model model) {
        boolean loggedIn = authentication != null && authentication.isAuthenticated()
                && !(authentication.getPrincipal() instanceof String);
        model.addAttribute("isLoggedIn", loggedIn);
        if (loggedIn) {
            String name = null;
            if (authentication.getPrincipal() instanceof UserDetails ud) {
                name = ud.getUsername();
            } else if (authentication.getPrincipal() instanceof OAuth2User oauth) {
                name = oauth.getAttribute("name");
                if (name == null) name = oauth.getAttribute("email");
            }
            model.addAttribute("currentUserName", name != null ? name : "User");
        }
    }
}
