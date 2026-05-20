package id.ac.ui.cs.advprog.donatjs.event;

import id.ac.ui.cs.advprog.donatjs.model.Campaign;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EventTest {

    @Test
    void testCampaignRefundRequestedEvent() {
        Campaign campaign = new Campaign();
        campaign.setId(1L);
        CampaignRefundRequestedEvent event = new CampaignRefundRequestedEvent(this, campaign, BigDecimal.TEN);
        
        assertEquals(campaign, event.getCampaign());
        assertEquals(BigDecimal.TEN, event.getAmount());
    }

    @Test
    void testCampaignPayoutRequestedEvent() {
        Campaign campaign = new Campaign();
        campaign.setId(1L);
        CampaignPayoutRequestedEvent event = new CampaignPayoutRequestedEvent(this, campaign, BigDecimal.TEN);
        
        assertEquals(campaign, event.getCampaign());
        assertEquals(BigDecimal.TEN, event.getAmount());
    }

    @Test
    void testCampaignFraudDetectedEvent() {
        Campaign campaign = new Campaign();
        campaign.setId(1L);
        CampaignFraudDetectedEvent event = new CampaignFraudDetectedEvent(this, campaign);
        
        assertEquals(campaign, event.getCampaign());
    }

    @Test
    void testProfileUpdatedEvent() {
        LocalDate dob = LocalDate.of(1990, 1, 1);
        ProfileUpdatedEvent event = new ProfileUpdatedEvent("user1", "John", "Bio", dob);
        
        assertEquals("user1", event.getUserId());
        assertEquals("John", event.getName());
        assertEquals("Bio", event.getBio());
        assertEquals(dob, event.getDateOfBirth());
    }

    @Test
    void testCampaignNotificationEventListener() {
        CampaignNotificationEventListener listener = new CampaignNotificationEventListener();
        Campaign campaign = new Campaign();
        campaign.setId(1L);

        listener.handlePayout(new CampaignPayoutRequestedEvent(this, campaign, BigDecimal.TEN));
        listener.handleRefund(new CampaignRefundRequestedEvent(this, campaign, BigDecimal.TEN));
        listener.handleFraud(new CampaignFraudDetectedEvent(this, campaign));
        
        // Verifying that it doesn't throw exceptions
    }

    @Test
    void testProfileUpdatedEventListener() {
        ProfileUpdatedEventListener listener = new ProfileUpdatedEventListener();
        ProfileUpdatedEvent event = new ProfileUpdatedEvent("user1", "John", "Bio", LocalDate.of(1990, 1, 1));

        listener.syncCampaignModule(event);
        listener.syncDonationModule(event);
        
        // Verifying that it doesn't throw exceptions
    }
}
