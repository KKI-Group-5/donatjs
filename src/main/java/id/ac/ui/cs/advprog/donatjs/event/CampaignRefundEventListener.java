package id.ac.ui.cs.advprog.donatjs.event;

import id.ac.ui.cs.advprog.donatjs.model.Campaign;
import id.ac.ui.cs.advprog.donatjs.model.CampaignStatus;
import id.ac.ui.cs.advprog.donatjs.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class CampaignRefundEventListener {

    private final WalletService walletService;

    @EventListener
    public void handleRefundRequest(CampaignRefundRequestedEvent event) {
        Campaign campaign = event.getCampaign();

        if (campaign.getStatus() == CampaignStatus.FRAUD) {
            log.info("Skipping automatic refund for FRAUD campaign id={}", campaign.getId());
            return;
        }

        if (event.getAmount() == null || event.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        int count = walletService.bulkRefundForCampaign(campaign.getId(), campaign.getTitle());
        log.info("Bulk refund complete: {} wallet donation(s) refunded for campaign id={}",
                count, campaign.getId());
    }
}
