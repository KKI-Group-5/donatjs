package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.model.Campaign;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingCampaignWalletGateway implements CampaignWalletGateway {

    private static final Logger log = LoggerFactory.getLogger(LoggingCampaignWalletGateway.class);

    @Override
    public void requestPayout(Campaign campaign) {
        log.info("Wallet payout requested for campaignId={}, amount={}",
                campaign.getId(), campaign.getTotalRaised());
    }

    @Override
    public void requestRefund(Campaign campaign) {
        log.info("Wallet refund requested for campaignId={}, amount={}",
                campaign.getId(), campaign.getTotalRaised());
    }
}
