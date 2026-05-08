package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.dto.CreateSubscriptionRequest;
import id.ac.ui.cs.advprog.donatjs.dto.SubscriptionResponse;
import id.ac.ui.cs.advprog.donatjs.model.Subscription.SubscriptionFrequency;

import java.util.List;

public interface SubscriptionService {

    SubscriptionResponse createSubscription(CreateSubscriptionRequest request);

    SubscriptionResponse cancelSubscription(Long subscriptionId, String userId);

    SubscriptionResponse updateFrequency(Long subscriptionId, String userId, SubscriptionFrequency frequency);

    List<SubscriptionResponse> getSubscriptionsByUser(String userId);

    /**
     * Mark every ACTIVE subscription tied to the given campaign as TERMINATED.
     * Called when a campaign moves to DELETED, CANCELLED, FRAUD, or REJECTED.
     * Returns the number of subscriptions terminated.
     */
    int terminateActiveSubscriptionsForCampaign(Long campaignId, String reason);
}
