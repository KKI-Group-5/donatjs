package id.ac.ui.cs.advprog.donatjs.controller;

import id.ac.ui.cs.advprog.donatjs.dto.DonationResponse;
import id.ac.ui.cs.advprog.donatjs.dto.LeaderboardEntry;
import id.ac.ui.cs.advprog.donatjs.service.CurrentUserService;
import id.ac.ui.cs.advprog.donatjs.service.DonationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class DonationPageController {

    private final DonationService donationService;
    private final CurrentUserService currentUserService;

    @GetMapping("/donations")
    public String myDonationsPage(Model model) {
        String userId = currentUserService.requireCurrentUserId();
        List<DonationResponse> donations = donationService.getDonationsByUser(userId);
        model.addAttribute("donations", donations);
        return "donations";
    }

    @GetMapping("/leaderboard")
    public String leaderboardPage(@RequestParam(defaultValue = "20") int limit, Model model) {
        List<LeaderboardEntry> leaderboard = donationService.getOverallLeaderboard(limit);
        model.addAttribute("leaderboard", leaderboard);
        return "leaderboard";
    }
}
