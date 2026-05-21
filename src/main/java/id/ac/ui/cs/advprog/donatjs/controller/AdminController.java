package id.ac.ui.cs.advprog.donatjs.controller;

import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import id.ac.ui.cs.advprog.donatjs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@SuppressWarnings("null")
public class AdminController {

    private final UserRepository userRepository;

    @GetMapping("/users/flagged")
    public ResponseEntity<List<AppUser>> getFlaggedUsers() {
        List<AppUser> flaggedUsers = userRepository.findAll().stream()
                .filter(AppUser::isFlaggedForReview)
                .collect(Collectors.toList());
        return ResponseEntity.ok(flaggedUsers);
    }

    @PostMapping("/users/{id}/suspend")
    public ResponseEntity<String> suspendUser(@PathVariable UUID id) {
        Optional<AppUser> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        AppUser user = userOpt.get();
        user.setSuspended(true);
        user.setFlaggedForReview(false); // Clear the flag since action is taken
        userRepository.save(user);

        return ResponseEntity.ok("User suspended successfully");
    }

    @PostMapping("/users/{id}/unsuspend")
    public ResponseEntity<String> unsuspendUser(@PathVariable UUID id) {
        Optional<AppUser> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        AppUser user = userOpt.get();
        user.setSuspended(false);
        user.setFlaggedForReview(false); // Clear the flag as the admin deemed them okay
        user.setRejectedCampaignCount(0); // Reset counters to give a clean slate
        user.setRejectedDonationCount(0);
        userRepository.save(user);

        return ResponseEntity.ok("User unsuspended and flags cleared");
    }
}
