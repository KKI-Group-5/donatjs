package id.ac.ui.cs.advprog.donatjs.controller;

import id.ac.ui.cs.advprog.donatjs.dto.SaveCampaignRequest;
import id.ac.ui.cs.advprog.donatjs.model.SavedCampaign;
import id.ac.ui.cs.advprog.donatjs.service.CurrentUserService;
import id.ac.ui.cs.advprog.donatjs.service.SavedCampaignService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/saved-campaigns")
public class SavedCampaignController {

    private final SavedCampaignService savedCampaignService;
    private final CurrentUserService currentUserService;

    public SavedCampaignController(SavedCampaignService savedCampaignService,
                                   CurrentUserService currentUserService) {
        this.savedCampaignService = savedCampaignService;
        this.currentUserService = currentUserService;
    }

    /**
     * Resolve the effective user id for a request. If the caller is
     * authenticated (e.g. via Spring Security), always prefer that identity
     * and ignore whatever the client may have sent in the payload — this
     * prevents users from saving campaigns on someone else's behalf. Falls
     * back to the supplied value only when there is no authenticated user
     * (e.g. pure API tests without Security).
     */
    private String resolveUserId(String supplied) {
        try {
            return currentUserService.requireCurrentUserId();
        } catch (Exception ignored) {
            return supplied;
        }
    }

    @PostMapping
    public ResponseEntity<Object> saveCampaign(@RequestBody SaveCampaignRequest request) {
        String userId = resolveUserId(request.getUserId());
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication required"));
        }
        try {
            SavedCampaign saved = savedCampaignService.saveCampaign(
                    userId,
                    request.getCampaignId(),
                    request.getCampaignTitle(),
                    request.getCampaignOrganizer(),
                    request.getCampaignImageUrl()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{userId}/{campaignId}")
    public ResponseEntity<Object> removeSavedCampaign(
            @PathVariable String userId,
            @PathVariable String campaignId) {
        try {
            savedCampaignService.removeSavedCampaign(resolveUserId(userId), campaignId);
            return ResponseEntity.ok(Map.of("message", "Campaign removed from saved list"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<SavedCampaign>> getSavedCampaigns(@PathVariable String userId) {
        List<SavedCampaign> campaigns = savedCampaignService.getSavedCampaigns(resolveUserId(userId));
        return ResponseEntity.ok(campaigns);
    }

    @GetMapping("/{userId}/check/{campaignId}")
    public ResponseEntity<Map<String, Boolean>> isCampaignSaved(
            @PathVariable String userId,
            @PathVariable String campaignId) {
        boolean saved = savedCampaignService.isCampaignSaved(resolveUserId(userId), campaignId);
        return ResponseEntity.ok(Map.of("saved", saved));
    }
}
