package id.ac.ui.cs.advprog.donatjs.controller;

import id.ac.ui.cs.advprog.donatjs.model.SavedCampaign;
import id.ac.ui.cs.advprog.donatjs.model.Subscription;
import id.ac.ui.cs.advprog.donatjs.service.CurrentUserService;
import id.ac.ui.cs.advprog.donatjs.service.SavedCampaignService;
import id.ac.ui.cs.advprog.donatjs.service.SubscriptionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class PageController {

    private final SavedCampaignService savedCampaignService;
    private final SubscriptionService subscriptionService;
    private final CurrentUserService currentUserService;

    public PageController(SavedCampaignService savedCampaignService,
                          SubscriptionService subscriptionService,
                          CurrentUserService currentUserService) {
        this.savedCampaignService = savedCampaignService;
        this.subscriptionService = subscriptionService;
        this.currentUserService = currentUserService;
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

    @GetMapping("/subscriptions")
    public String subscriptionsPage(Model model) {
        String userId = currentUserService.requireCurrentUserId();
        List<Subscription> subscriptions = subscriptionService.listByUser(userId);
        model.addAttribute("subscriptions", subscriptions);
        model.addAttribute("userId", userId);
        return "subscriptions";
    }
}
