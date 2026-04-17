package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.dto.SubscriptionResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Temporary STUB implementation of SubscriptionService.
 * This serves as a placeholder until the Subscription module is fully implemented.
 */
@Service
public class StubSubscriptionService implements SubscriptionService {

    @Override
    public List<SubscriptionResponse> getSubscriptionsByUser(String userId) {
        // Returns hardcoded data for demonstration if the demo user is used
        List<SubscriptionResponse> subs = new ArrayList<>();
        
        if ("user-demo-001".equals(userId)) {
            subs.add(SubscriptionResponse.builder()
                    .subscriptionId("SUB-001")
                    .campaignId(1L)
                    .campaignTitle("Help Build a School")
                    .amount(50000L)
                    .frequency("MONTHLY")
                    .status("ACTIVE")
                    .nextBillingDate(LocalDateTime.now().plusWeeks(2))
                    .build());
        }
        
        return subs;
    }
}
