package id.ac.ui.cs.advprog.donatjs.controller;

import id.ac.ui.cs.advprog.donatjs.dto.DisputeDTO;
import id.ac.ui.cs.advprog.donatjs.dto.DisputeResolutionRequest;
import id.ac.ui.cs.advprog.donatjs.dto.DisputeSubmissionRequest;
import id.ac.ui.cs.advprog.donatjs.service.CurrentUserService;
import id.ac.ui.cs.advprog.donatjs.service.DisputeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/disputes")
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeService disputeService;
    private final CurrentUserService currentUserService;

    @PostMapping("/submit")
    public ResponseEntity<DisputeDTO> submitDispute(@Valid @RequestBody DisputeSubmissionRequest request, Authentication authentication) {
        UUID userId = UUID.fromString(currentUserService.getCurrentUserId(authentication));
        DisputeDTO disputeDTO = disputeService.submitDispute(Objects.requireNonNull(userId), Objects.requireNonNull(request.getReason()));
        return ResponseEntity.ok(disputeDTO);
    }

    @GetMapping("/me")
    public ResponseEntity<List<DisputeDTO>> getMyDisputes(Authentication authentication) {
        UUID userId = UUID.fromString(currentUserService.getCurrentUserId(authentication));
        List<DisputeDTO> disputes = disputeService.getDisputesByUser(Objects.requireNonNull(userId));
        return ResponseEntity.ok(disputes);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/pending")
    public ResponseEntity<List<DisputeDTO>> getAllPendingDisputes() {
        List<DisputeDTO> pendingDisputes = disputeService.getAllPendingDisputes();
        return ResponseEntity.ok(pendingDisputes);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{disputeId}/resolve")
    public ResponseEntity<DisputeDTO> resolveDispute(
            @PathVariable UUID disputeId,
            @RequestBody DisputeResolutionRequest request) {
        DisputeDTO resolved = disputeService.resolveDispute(Objects.requireNonNull(disputeId), request.isApprove(), request.getAdminNotes());
        return ResponseEntity.ok(resolved);
    }
}
