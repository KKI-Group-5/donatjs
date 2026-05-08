package id.ac.ui.cs.advprog.donatjs.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class CampaignDeadlineScheduler {

    private static final Logger log = LoggerFactory.getLogger(CampaignDeadlineScheduler.class);
    private final CampaignService campaignService;

    public CampaignDeadlineScheduler(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void finalizeExpiredCampaigns() {
        int processed = campaignService.processExpiredCampaigns(LocalDate.now());
        if (processed > 0) {
            log.info("Campaign deadline automation processed {} campaign(s)", processed);
        }
    }
}
