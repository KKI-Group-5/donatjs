package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.event.ProfileUpdatedEvent;
import org.junit.jupiter.api.Test;

class CampaignProfileUpdateListenerTest {

    @Test
    void testHandleProfileUpdated() {
        CampaignProfileUpdateListener listener = new CampaignProfileUpdateListener();
        ProfileUpdatedEvent event = new ProfileUpdatedEvent("user-123", "New Name", "New Bio", null);
        
        // This is a void method that just logs, so we verify no exceptions are thrown
        listener.handleProfileUpdated(event);
    }
}
