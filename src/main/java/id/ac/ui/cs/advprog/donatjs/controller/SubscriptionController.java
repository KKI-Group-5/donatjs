package id.ac.ui.cs.advprog.donatjs.controller;

import id.ac.ui.cs.advprog.donatjs.dto.CreateSubscriptionRequest;
import id.ac.ui.cs.advprog.donatjs.dto.SubscriptionResponse;
import id.ac.ui.cs.advprog.donatjs.dto.UpdateSubscriptionRequest;
import id.ac.ui.cs.advprog.donatjs.service.CurrentUserService;
import id.ac.ui.cs.advprog.donatjs.service.SubscriptionService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final CurrentUserService currentUserService;

    @PostMapping
    public ResponseEntity<Object> createSubscription(@Valid @RequestBody CreateSubscriptionRequest request,
                                                     Authentication authentication) {
        request.setUserId(currentUserService.getCurrentUserId(authentication));
        try {
            SubscriptionResponse response = subscriptionService.createSubscription(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalStateException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("already exists")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", msg));
            }
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("error", msg != null ? msg : "Unknown error"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> cancelSubscription(
            @PathVariable Long id,
            Authentication authentication) {
        String userId = currentUserService.getCurrentUserId(authentication);
        try {
            SubscriptionResponse response = subscriptionService.cancelSubscription(id, userId);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/frequency")
    public ResponseEntity<Object> updateFrequency(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSubscriptionRequest request,
            Authentication authentication) {
        String userId = currentUserService.getCurrentUserId(authentication);
        try {
            SubscriptionResponse response = subscriptionService.updateFrequency(id, userId, request.getFrequency());
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SubscriptionResponse>> getSubscriptionsByUser(@PathVariable String userId,
                                                                             Authentication authentication) {
        String currentUserId = currentUserService.getCurrentUserId(authentication);
        if (!currentUserId.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only access your own subscriptions");
        }
        return ResponseEntity.ok(subscriptionService.getSubscriptionsByUser(currentUserId));
    }

    @GetMapping("/me")
    public ResponseEntity<List<SubscriptionResponse>> getMySubscriptions(Authentication authentication) {
        return ResponseEntity.ok(subscriptionService.getSubscriptionsByUser(
                currentUserService.getCurrentUserId(authentication)));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleEntityNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }
}
