package id.ac.ui.cs.advprog.donatjs.controller;

import id.ac.ui.cs.advprog.donatjs.model.SavedCampaign;
import id.ac.ui.cs.advprog.donatjs.service.SavedCampaignService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class PageController {

    private final SavedCampaignService savedCampaignService;

    public PageController(SavedCampaignService savedCampaignService) {
        this.savedCampaignService = savedCampaignService;
    }

    @GetMapping("/saved-campaigns/{userId}")
    public String savedCampaignsPage(@PathVariable String userId, Model model) {
        List<SavedCampaign> savedCampaigns = savedCampaignService.getSavedCampaigns(userId);
        model.addAttribute("savedCampaigns", savedCampaigns);
        model.addAttribute("userId", userId);
        return "saved-campaigns";
    }
}
