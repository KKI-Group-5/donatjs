package id.ac.ui.cs.advprog.donatjs.controller;

import id.ac.ui.cs.advprog.donatjs.dto.CreateDonationRequest;
import id.ac.ui.cs.advprog.donatjs.dto.DonationResponse;
import id.ac.ui.cs.advprog.donatjs.dto.LeaderboardEntry;
import id.ac.ui.cs.advprog.donatjs.dto.WithdrawalRequestResponse;
import id.ac.ui.cs.advprog.donatjs.model.Donation.DonationStatus;
import id.ac.ui.cs.advprog.donatjs.service.CurrentUserService;
import id.ac.ui.cs.advprog.donatjs.service.DonationService;
import id.ac.ui.cs.advprog.donatjs.service.WithdrawalRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/donations")
@RequiredArgsConstructor
public class DonationController {

    private final DonationService donationService;
    private final CurrentUserService currentUserService;
    private final WithdrawalRequestService withdrawalRequestService;

    @PostMapping
    public ResponseEntity<DonationResponse> createDonation(
            @Valid @RequestBody CreateDonationRequest request,
            Authentication auth) {

        request.setUserId(currentUserService.getCurrentUserId(auth));

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

    @GetMapping("/leaderboard")
    public ResponseEntity<List<LeaderboardEntry>> getOverallLeaderboard(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(donationService.getOverallLeaderboard(limit));
    }

    @GetMapping("/campaign/{campaignId}/leaderboard")
    public ResponseEntity<List<LeaderboardEntry>> getCampaignLeaderboard(
            @PathVariable Long campaignId,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(donationService.getCampaignLeaderboard(campaignId, limit));
    }

    @PostMapping("/{donationId}/withdrawal-request")
    public ResponseEntity<WithdrawalRequestResponse> requestWithdrawal(
            @PathVariable Long donationId,
            @RequestParam(required = false) String reason,
            Authentication auth) {
        String userId = currentUserService.getCurrentUserId(auth);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(withdrawalRequestService.requestWithdrawal(donationId, userId, reason));
    }

    @GetMapping("/withdrawal-requests")
    public ResponseEntity<List<WithdrawalRequestResponse>> getPendingWithdrawalRequests() {
        return ResponseEntity.ok(withdrawalRequestService.getPendingRequests());
    }

    @PostMapping("/withdrawal-requests/{requestId}/approve")
    public ResponseEntity<WithdrawalRequestResponse> approveWithdrawal(@PathVariable Long requestId) {
        return ResponseEntity.ok(withdrawalRequestService.approveWithdrawal(requestId));
    }

    @PostMapping("/withdrawal-requests/{requestId}/reject")
    public ResponseEntity<WithdrawalRequestResponse> rejectWithdrawal(@PathVariable Long requestId) {
        return ResponseEntity.ok(withdrawalRequestService.rejectWithdrawal(requestId));
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