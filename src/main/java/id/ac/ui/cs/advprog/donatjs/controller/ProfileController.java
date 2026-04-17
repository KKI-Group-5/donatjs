package id.ac.ui.cs.advprog.donatjs.controller;

import id.ac.ui.cs.advprog.donatjs.dto.UpdateProfileRequest;
import id.ac.ui.cs.advprog.donatjs.dto.UserProfileDTO;
import id.ac.ui.cs.advprog.donatjs.service.ProfileService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@Tag(name = "User Profile", description = "Endpoints for viewing and updating user profile information")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    private String getEmailFromPrincipal(Authentication authentication) {
        if (authentication.getPrincipal() instanceof OAuth2User) {
            return ((OAuth2User) authentication.getPrincipal()).getAttribute("email");
        } else if (authentication.getPrincipal() instanceof UserDetails) {
            return ((UserDetails) authentication.getPrincipal()).getUsername();
        }
        return authentication.getName();
    }

    @GetMapping("/me")
    public UserProfileDTO getMyProfile(Authentication authentication) {
        String email = getEmailFromPrincipal(authentication);
        return profileService.getUserProfile(email);
    }

    @PutMapping("/update")
    public UserProfileDTO updateMyProfile(
            Authentication authentication,
            @RequestBody UpdateProfileRequest request) {

        String email = getEmailFromPrincipal(authentication);
        return profileService.updateUserProfile(email, request);
    }
}