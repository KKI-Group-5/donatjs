package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.event.ProfileUpdatedEvent;
import org.junit.jupiter.api.Test;

class DonationProfileUpdateListenerTest {

    @Test
    void testHandleProfileUpdated() {
        DonationProfileUpdateListener listener = new DonationProfileUpdateListener();
        ProfileUpdatedEvent event = new ProfileUpdatedEvent("user-123", "New Name", "New Bio", null);
        
        // This is a void method that just logs, so we verify no exceptions are thrown
        listener.handleProfileUpdated(event);
    }
}
