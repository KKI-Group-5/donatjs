package id.ac.ui.cs.advprog.donatjs.controller;

import id.ac.ui.cs.advprog.donatjs.dto.SaveCampaignRequest;
import id.ac.ui.cs.advprog.donatjs.model.SavedCampaign;
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

    public SavedCampaignController(SavedCampaignService savedCampaignService) {
        this.savedCampaignService = savedCampaignService;
    }

    @PostMapping
    public ResponseEntity<Object> saveCampaign(@RequestBody SaveCampaignRequest request) {
        try {
            SavedCampaign saved = savedCampaignService.saveCampaign(
                    request.getUserId(),
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
            savedCampaignService.removeSavedCampaign(userId, campaignId);
            return ResponseEntity.ok(Map.of("message", "Campaign removed from saved list"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<SavedCampaign>> getSavedCampaigns(@PathVariable String userId) {
        List<SavedCampaign> campaigns = savedCampaignService.getSavedCampaigns(userId);
        return ResponseEntity.ok(campaigns);
    }

    @GetMapping("/{userId}/check/{campaignId}")
    public ResponseEntity<Map<String, Boolean>> isCampaignSaved(
            @PathVariable String userId,
            @PathVariable String campaignId) {
        boolean saved = savedCampaignService.isCampaignSaved(userId, campaignId);
        return ResponseEntity.ok(Map.of("saved", saved));
    }
}
