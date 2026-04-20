package id.ac.ui.cs.advprog.donatjs.controller;

import id.ac.ui.cs.advprog.donatjs.model.Campaign;
import id.ac.ui.cs.advprog.donatjs.service.CampaignService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class LandingController {

    private static final int HOMEPAGE_CAMPAIGN_LIMIT = 6;

    private final CampaignService campaignService;

    public LandingController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @GetMapping("/")
    public String landingPage(Model model) {
        List<Campaign> campaigns = campaignService.findOpenCampaigns().stream()
                .limit(HOMEPAGE_CAMPAIGN_LIMIT)
                .toList();
        model.addAttribute("campaigns", campaigns);
        return "index";
    }
}
