package id.ac.ui.cs.advprog.donatjs.controller;

import id.ac.ui.cs.advprog.donatjs.exception.InsufficientBalanceException;
import id.ac.ui.cs.advprog.donatjs.model.Wallet;
import id.ac.ui.cs.advprog.donatjs.service.WalletService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Internal REST API for inter-module communication.
 * Used by the Donation Module to deduct wallet balances when a user makes a donation.
 */
@RestController
@RequestMapping("/api/internal/wallet")
public class WalletApiController {

    private final WalletService walletService;

    public WalletApiController(WalletService walletService) {
        this.walletService = walletService;
    }

    /**
     * Deducts the wallet balance for a donation.
     * Called internally by the Donation Module — not exposed to end users.
     *
     * Request body:
     * {
     *   "userId": "user-demo-001",
     *   "amount": 50000,
     *   "campaignName": "Build a School"
     * }
     */
    @PostMapping("/deduct")
    public ResponseEntity<Map<String, Object>> deductForDonation(@RequestBody Map<String, Object> request) {
        String userId = (String) request.get("userId");
        Number amountRaw = (Number) request.get("amount");
        String campaignName = (String) request.getOrDefault("campaignName", "Unknown Campaign");

        if (userId == null || amountRaw == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "userId and amount are required."));
        }

        double amount = amountRaw.doubleValue();

        try {
            Wallet updated = walletService.deductForDonation(userId, amount, campaignName);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Donation deducted successfully.",
                    "newBalance", updated.getBalance()
            ));
        } catch (InsufficientBalanceException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Returns the current wallet balance for a user.
     * Can be used by other modules to check balance before attempting a donation.
     */
    @GetMapping("/balance/{userId}")
    public ResponseEntity<Map<String, Object>> getBalance(@PathVariable String userId) {
        try {
            Wallet wallet = walletService.getWalletByUserId(userId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "userId", userId,
                    "balance", wallet.getBalance()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Refunds all wallet donations for a cancelled/closed campaign.
     * Called internally by the Campaign Module when a campaign is cancelled.
     *
     * Request body:
     * {
     *   "campaignId": 42,
     *   "campaignName": "Help Build a School"
     * }
     */
    @PostMapping("/bulk-refund")
    public ResponseEntity<Map<String, Object>> bulkRefund(@RequestBody Map<String, Object> request) {
        Number campaignIdRaw = (Number) request.get("campaignId");
        String campaignName = (String) request.get("campaignName");

        if (campaignIdRaw == null || campaignName == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "campaignId and campaignName are required."));
        }

        try {
            int refunded = walletService.bulkRefundForCampaign(campaignIdRaw.longValue(), campaignName);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "refunded", refunded,
                    "message", refunded + " wallet donation(s) refunded for campaign " + campaignIdRaw.longValue() + "."
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
