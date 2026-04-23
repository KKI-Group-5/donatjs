package id.ac.ui.cs.advprog.donatjs.controller;

import id.ac.ui.cs.advprog.donatjs.dto.UpdateProfileRequest;
import id.ac.ui.cs.advprog.donatjs.dto.UserProfileDTO;
import id.ac.ui.cs.advprog.donatjs.service.ProfileService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    private String getEmail(Object principal) {
        if (principal instanceof OAuth2User oauth) {
            return oauth.getAttribute("email");
        } else if (principal instanceof UserDetails user) {
            return user.getUsername();
        }
        return principal.toString();
    }

    @GetMapping("/me")
    public UserProfileDTO getMyProfile(@AuthenticationPrincipal Object principal) {
        String email = getEmail(principal);
        return profileService.getUserProfile(email);
    }

    @PutMapping("/update")
    public UserProfileDTO updateMyProfile(
            @AuthenticationPrincipal Object principal,
            @RequestBody UpdateProfileRequest request) {

        String email = getEmail(principal);
        return profileService.updateUserProfile(email, request);
    }
}