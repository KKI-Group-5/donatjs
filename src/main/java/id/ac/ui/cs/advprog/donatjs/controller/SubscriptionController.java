package id.ac.ui.cs.advprog.donatjs.controller;

import id.ac.ui.cs.advprog.donatjs.dto.CreateSubscriptionRequest;
import id.ac.ui.cs.advprog.donatjs.dto.UpdateSubscriptionRequest;
import id.ac.ui.cs.advprog.donatjs.exception.InsufficientBalanceException;
import id.ac.ui.cs.advprog.donatjs.model.Subscription;
import id.ac.ui.cs.advprog.donatjs.service.CurrentUserService;
import id.ac.ui.cs.advprog.donatjs.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final CurrentUserService currentUserService;

    public SubscriptionController(SubscriptionService subscriptionService,
                                  CurrentUserService currentUserService) {
        this.subscriptionService = subscriptionService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ResponseEntity<List<Subscription>> list() {
        String userId = currentUserService.requireCurrentUserId();
        return ResponseEntity.ok(subscriptionService.listByUser(userId));
    }

    @PostMapping
    public ResponseEntity<Subscription> subscribe(@Valid @RequestBody CreateSubscriptionRequest request) {
        String userId = currentUserService.requireCurrentUserId();
        Subscription created = subscriptionService.subscribe(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Subscription> update(@PathVariable("id") Long id,
                                               @RequestBody UpdateSubscriptionRequest request) {
        String userId = currentUserService.requireCurrentUserId();
        return ResponseEntity.ok(subscriptionService.updateSubscription(userId, id, request));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Subscription> cancel(@PathVariable("id") Long id) {
        String userId = currentUserService.requireCurrentUserId();
        return ResponseEntity.ok(subscriptionService.cancel(userId, id));
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<Subscription> resume(@PathVariable("id") Long id) {
        String userId = currentUserService.requireCurrentUserId();
        return ResponseEntity.ok(subscriptionService.resume(userId, id));
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<Map<String, String>> handleInsufficient(InsufficientBalanceException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("error", ex.getMessage()));
    }
}
