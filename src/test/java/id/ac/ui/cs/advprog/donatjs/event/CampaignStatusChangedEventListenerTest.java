package id.ac.ui.cs.advprog.donatjs.event;

import id.ac.ui.cs.advprog.donatjs.model.CampaignStatus;
import id.ac.ui.cs.advprog.donatjs.service.SubscriptionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampaignStatusChangedEventListenerTest {

    @Mock private SubscriptionService subscriptionService;

    @InjectMocks
    private CampaignStatusChangedEventListener listener;

    @Test
    void deleted_terminatesActiveSubscriptions() {
        when(subscriptionService.terminateActiveSubscriptionsForCampaign(eq(7L), anyString())).thenReturn(2);

        listener.handleStatusChange(new CampaignStatusChangedEvent(
                this, 7L, CampaignStatus.OPEN, CampaignStatus.DELETED));

        verify(subscriptionService).terminateActiveSubscriptionsForCampaign(eq(7L), anyString());
    }

    @Test
    void rejected_terminatesActiveSubscriptions() {
        listener.handleStatusChange(new CampaignStatusChangedEvent(
                this, 7L, CampaignStatus.WAITING, CampaignStatus.REJECTED));

        verify(subscriptionService).terminateActiveSubscriptionsForCampaign(eq(7L), anyString());
    }

    @Test
    void cancelled_terminatesActiveSubscriptions() {
        listener.handleStatusChange(new CampaignStatusChangedEvent(
                this, 7L, CampaignStatus.OPEN, CampaignStatus.CANCELLED));

        verify(subscriptionService).terminateActiveSubscriptionsForCampaign(eq(7L), anyString());
    }

    @Test
    void fraud_terminatesActiveSubscriptions() {
        listener.handleStatusChange(new CampaignStatusChangedEvent(
                this, 7L, CampaignStatus.OPEN, CampaignStatus.FRAUD));

        verify(subscriptionService).terminateActiveSubscriptionsForCampaign(eq(7L), anyString());
    }

    @Test
    void closed_doesNotTerminate_healthyEndOfLife() {
        listener.handleStatusChange(new CampaignStatusChangedEvent(
                this, 7L, CampaignStatus.OPEN, CampaignStatus.CLOSED));

        verify(subscriptionService, never()).terminateActiveSubscriptionsForCampaign(anyLong(), anyString());
    }

    @Test
    void open_doesNotTerminate_normalApprovalTransition() {
        listener.handleStatusChange(new CampaignStatusChangedEvent(
                this, 7L, CampaignStatus.WAITING, CampaignStatus.OPEN));

        verify(subscriptionService, never()).terminateActiveSubscriptionsForCampaign(anyLong(), anyString());
    }
}
