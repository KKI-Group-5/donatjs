package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.dto.SubscriptionResponse;
import java.util.List;

/**
 * Interface for Subscription management. 
 * To be fully implemented by the Saved Campaign & Subscription module.
 */
public interface SubscriptionService {
    List<SubscriptionResponse> getSubscriptionsByUser(String userId);
}
