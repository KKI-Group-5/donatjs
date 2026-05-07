package id.ac.ui.cs.advprog.donatjs.controller;

import id.ac.ui.cs.advprog.donatjs.dto.SubscriptionResponse;
import id.ac.ui.cs.advprog.donatjs.model.SavedCampaign;
import id.ac.ui.cs.advprog.donatjs.service.CurrentUserService;
import id.ac.ui.cs.advprog.donatjs.service.ProfileService;
import id.ac.ui.cs.advprog.donatjs.service.SavedCampaignService;
import id.ac.ui.cs.advprog.donatjs.service.SubscriptionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.security.access.prepost.PreAuthorize;
import id.ac.ui.cs.advprog.donatjs.repository.UserRepository;
import id.ac.ui.cs.advprog.donatjs.model.AppUser;

import java.util.List;

@Controller
public class PageController {

    private final SavedCampaignService savedCampaignService;
    private final SubscriptionService subscriptionService;
    private final CurrentUserService currentUserService;
    private final ProfileService profileService;
    private final UserRepository userRepository;

    public PageController(SavedCampaignService savedCampaignService,
            SubscriptionService subscriptionService,
            CurrentUserService currentUserService,
            ProfileService profileService,
            UserRepository userRepository) {
        this.savedCampaignService = savedCampaignService;
        this.subscriptionService = subscriptionService;
        this.currentUserService = currentUserService;
        this.profileService = profileService;
        this.userRepository = userRepository;
    }

    @GetMapping("/saved-campaigns/{userId}")
    public String savedCampaignsPage(@PathVariable String userId, Model model) {
        List<SavedCampaign> savedCampaigns = savedCampaignService.getSavedCampaigns(userId);
        model.addAttribute("savedCampaigns", savedCampaigns);
        model.addAttribute("userId", userId);
        return "saved-campaigns";
    }

    @GetMapping("/saved-campaigns")
    public String mySavedCampaignsPage(Model model) {
        String userId = currentUserService.requireCurrentUserId();
        return savedCampaignsPage(userId, model);
    }

    @GetMapping("/subscriptions/{userId}")
    public String subscriptionsPage(@PathVariable String userId, Model model) {
        List<SubscriptionResponse> subscriptions = subscriptionService.getSubscriptionsByUser(userId);
        model.addAttribute("subscriptions", subscriptions);
        model.addAttribute("userId", userId);
        return "subscriptions";
    }

    @GetMapping("/subscriptions")
    public String mySubscriptionsPage(Model model) {
        String userId = currentUserService.requireCurrentUserId();
        return subscriptionsPage(userId, model);
    }

    @GetMapping("/profile")
    public String profileDashboard(Model model) {
        String email = currentUserService.getCurrentUserEmail(
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication());
        model.addAttribute("profile", profileService.getUserProfile(email));
        return "profile";
    }

    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminDashboard(Model model) {
        List<AppUser> flaggedUsers = ((List<AppUser>) userRepository.findAll())
                .stream()
                .filter(u -> u.isFlaggedForReview() || u.isSuspended())
                .toList();
        model.addAttribute("flaggedUsers", flaggedUsers);
        return "admin-dashboard";
    }
}
