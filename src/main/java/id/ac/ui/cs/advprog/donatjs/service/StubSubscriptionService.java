package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.dto.SubscriptionResponse;
// import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Temporary STUB implementation of SubscriptionService.
 * This serves as a placeholder until the Subscription module is fully implemented.
 */
public class StubSubscriptionService implements SubscriptionService {

    @Override
    public id.ac.ui.cs.advprog.donatjs.dto.SubscriptionResponse createSubscription(id.ac.ui.cs.advprog.donatjs.dto.CreateSubscriptionRequest request) {
        return null;
    }

    @Override
    public id.ac.ui.cs.advprog.donatjs.dto.SubscriptionResponse cancelSubscription(Long subscriptionId, String userId) {
        return null;
    }

    @Override
    public id.ac.ui.cs.advprog.donatjs.dto.SubscriptionResponse updateFrequency(Long subscriptionId, String userId, id.ac.ui.cs.advprog.donatjs.model.Subscription.SubscriptionFrequency frequency) {
        return null;
    }

    @Override
    public List<SubscriptionResponse> getSubscriptionsByUser(String userId) {
        // Returns hardcoded data for demonstration if the demo user is used
        List<SubscriptionResponse> subs = new ArrayList<>();
        
        if ("user-demo-001".equals(userId)) {
            subs.add(SubscriptionResponse.builder()
                    .id(1L)
                    .userId(userId)
                    .campaignId(1L)
                    .amount(50000L)
                    .frequency(id.ac.ui.cs.advprog.donatjs.model.Subscription.SubscriptionFrequency.MONTHLY)
                    .status(id.ac.ui.cs.advprog.donatjs.model.Subscription.SubscriptionStatus.ACTIVE)
                    .nextDebitDate(java.time.LocalDate.now().plusWeeks(2))
                    .build());
        }
        
        return subs;
    }

    @Override
    public int terminateActiveSubscriptionsForCampaign(Long campaignId, String reason) {
        return 0;
    }
}
