package id.ac.ui.cs.advprog.donatjs.controller;

import id.ac.ui.cs.advprog.donatjs.dto.CreateDonationRequest;
import id.ac.ui.cs.advprog.donatjs.dto.DonationResponse;
import id.ac.ui.cs.advprog.donatjs.service.DonationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/donations")
@RequiredArgsConstructor
public class DonationController {

    private final DonationService donationService;

    @PostMapping
    public ResponseEntity<DonationResponse> createDonation(
            @Valid @RequestBody CreateDonationRequest request) {

        DonationResponse response = donationService.createDonation(request);

        HttpStatus status = response.getStatus() == id.ac.ui.cs.advprog.donatjs.model.Donation.DonationStatus.REJECTED
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
}