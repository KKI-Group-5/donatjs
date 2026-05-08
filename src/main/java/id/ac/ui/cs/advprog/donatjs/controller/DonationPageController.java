package id.ac.ui.cs.advprog.donatjs.controller;

import id.ac.ui.cs.advprog.donatjs.dto.DonationResponse;
import id.ac.ui.cs.advprog.donatjs.service.CurrentUserService;
import id.ac.ui.cs.advprog.donatjs.service.DonationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

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
}
