package id.ac.ui.cs.advprog.donatjs.controller;

import id.ac.ui.cs.advprog.donatjs.dto.CreateDonationRequest;
import id.ac.ui.cs.advprog.donatjs.dto.DonationResponse;
import id.ac.ui.cs.advprog.donatjs.model.Donation.DonationStatus;
import id.ac.ui.cs.advprog.donatjs.repository.UserRepository;
import id.ac.ui.cs.advprog.donatjs.service.DonationService;
import id.ac.ui.cs.advprog.donatjs.service.UserModerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/donations")
@RequiredArgsConstructor
public class DonationController {

    private final DonationService donationService;
    private final UserRepository userRepository;
    private final UserModerationService moderationService;

    private String resolveEmail(Authentication auth) {
        if (auth.getPrincipal() instanceof OAuth2User oauth) {
            return oauth.getAttribute("email");
        } else if (auth.getPrincipal() instanceof UserDetails ud) {
            return ud.getUsername();
        }
        return auth.getName();
    }

    private String resolveUserId(Authentication auth) {
        String email = resolveEmail(auth);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"))
                .getId().toString();
    }

    @PostMapping
    public ResponseEntity<DonationResponse> createDonation(
            @Valid @RequestBody CreateDonationRequest request,
            Authentication auth) {

        String email = resolveEmail(auth);
        if (moderationService.isUserSuspended(email)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        request.setUserId(resolveUserId(auth));

        DonationResponse response = donationService.createDonation(request);

        HttpStatus status = response.getStatus() == DonationStatus.REJECTED
                ? HttpStatus.UNPROCESSABLE_ENTITY
                : HttpStatus.CREATED;

        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DonationResponse> getDonationById(@PathVariable Long id) {
        return ResponseEntity.ok(donationService.getDonationById(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<DonationResponse>> getDonationsByUser(
            @PathVariable String userId) {
        return ResponseEntity.ok(donationService.getDonationsByUser(userId));
    }

    @GetMapping("/campaign/{campaignId}/total")
    public ResponseEntity<Long> getTotalDonationsByCampaign(
            @PathVariable Long campaignId) {
        return ResponseEntity.ok(donationService.getTotalDonationsByCampaign(campaignId));
    }

    @GetMapping("/campaign/{campaignId}")
    public ResponseEntity<List<DonationResponse>> getSuccessfulDonationsByCampaign(
            @PathVariable Long campaignId) {
        return ResponseEntity.ok(donationService.getSuccessfulDonationsByCampaign(campaignId));
    }

    @PatchMapping("/{id}/notes")
    public ResponseEntity<DonationResponse> updateDonationNotes(
            @PathVariable Long id,
            @RequestParam String userId,
            @RequestParam String notes) {
        return ResponseEntity.ok(donationService.updateDonationNotes(id, userId, notes));
    }

    @GetMapping("/user/{userId}/rejected-count")
    public ResponseEntity<Long> getRejectedDonationCount(@PathVariable String userId) {
        return ResponseEntity.ok(donationService.countRejectedDonationsByUser(userId));
    }

    @PostMapping("/campaign/{campaignId}/refund")
    public ResponseEntity<Void> processCampaignRefund(@PathVariable Long campaignId) {
        donationService.processRefundForCampaign(campaignId);
        return ResponseEntity.ok().build();
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }
}
