package id.ac.ui.cs.advprog.donatjs.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CampaignDeadlineSchedulerTest {

    @Test
    void finalizeExpiredCampaigns_delegatesToCampaignService() {
        CampaignService campaignService = mock(CampaignService.class);
        CampaignDeadlineScheduler scheduler = new CampaignDeadlineScheduler(campaignService);

        scheduler.finalizeExpiredCampaigns();

        verify(campaignService).processExpiredCampaigns(LocalDate.now());
    }
}