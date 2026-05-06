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
}
