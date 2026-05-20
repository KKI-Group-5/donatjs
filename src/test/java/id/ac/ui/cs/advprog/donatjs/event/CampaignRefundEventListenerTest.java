package id.ac.ui.cs.advprog.donatjs.event;

import id.ac.ui.cs.advprog.donatjs.model.Campaign;
import id.ac.ui.cs.advprog.donatjs.model.CampaignStatus;
import id.ac.ui.cs.advprog.donatjs.service.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CampaignRefundEventListenerTest {

    @Mock
    private WalletService walletService;

    @InjectMocks
    private CampaignRefundEventListener listener;

    private Campaign campaign(long id, String title, CampaignStatus status) {
        Campaign c = new Campaign();
        c.setId(id);
        c.setTitle(title);
        c.setStatus(status);
        return c;
    }

    @Test
    void fraudCampaign_skipsRefund() {
        Campaign fraud = campaign(1L, "Scam", CampaignStatus.FRAUD);

        listener.handleRefundRequest(
                new CampaignRefundRequestedEvent(this, fraud, new BigDecimal("500000")));

        verify(walletService, never()).bulkRefundForCampaign(anyLong(), anyString());
    }

    @Test
    void cancelledCampaign_triggersRefund() {
        Campaign cancelled = campaign(2L, "Failed Campaign", CampaignStatus.CANCELLED);
        when(walletService.bulkRefundForCampaign(2L, "Failed Campaign")).thenReturn(3);

        listener.handleRefundRequest(
                new CampaignRefundRequestedEvent(this, cancelled, new BigDecimal("300000")));

        verify(walletService).bulkRefundForCampaign(2L, "Failed Campaign");
    }

    @Test
    void zeroAmount_skipsRefund() {
        Campaign cancelled = campaign(3L, "Zero Campaign", CampaignStatus.CANCELLED);

        listener.handleRefundRequest(
                new CampaignRefundRequestedEvent(this, cancelled, BigDecimal.ZERO));

        verify(walletService, never()).bulkRefundForCampaign(anyLong(), anyString());
    }
}
