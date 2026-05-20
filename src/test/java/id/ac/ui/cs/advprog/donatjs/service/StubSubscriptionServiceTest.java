package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.dto.SubscriptionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class StubSubscriptionServiceTest {

    private StubSubscriptionService stubSubscriptionService;

    @BeforeEach
    void setUp() {
        stubSubscriptionService = new StubSubscriptionService();
    }

    @Test
    void testCreateSubscriptionReturnsNull() {
        assertNull(stubSubscriptionService.createSubscription(null));
    }

    @Test
    void testCancelSubscriptionReturnsNull() {
        assertNull(stubSubscriptionService.cancelSubscription(1L, "user"));
    }

    @Test
    void testUpdateFrequencyReturnsNull() {
        assertNull(stubSubscriptionService.updateFrequency(1L, "user", null));
    }

    @Test
    void testTerminateActiveSubscriptionsForCampaignReturnsZero() {
        assertEquals(0, stubSubscriptionService.terminateActiveSubscriptionsForCampaign(1L, "reason"));
    }

    @Test
    void testGetSubscriptionsByUserDemo() {
        List<SubscriptionResponse> results = stubSubscriptionService.getSubscriptionsByUser("user-demo-001");
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(1L, results.get(0).getId());
    }

    @Test
    void testGetSubscriptionsByUserOther() {
        List<SubscriptionResponse> results = stubSubscriptionService.getSubscriptionsByUser("other-user");
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }
}
