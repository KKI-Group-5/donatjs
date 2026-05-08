package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.model.Campaign;
import id.ac.ui.cs.advprog.donatjs.model.CampaignStatus;
import id.ac.ui.cs.advprog.donatjs.event.RejectedCampaignEvent;
import id.ac.ui.cs.advprog.donatjs.repository.InMemoryCampaignRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimpleCampaignServiceTest {

    private InMemoryCampaignRepository repository;
    private SimpleCampaignService service;
    private RecordingCampaignWalletGateway walletGateway;
    private RecordingEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        repository = new InMemoryCampaignRepository();
        walletGateway = new RecordingCampaignWalletGateway();
        eventPublisher = new RecordingEventPublisher();
        service = new SimpleCampaignService(repository, walletGateway, eventPublisher);
    }

    @Test
    void createCampaign_setsCreatedAtIfNull() {
        Campaign campaign = new Campaign();
        campaign.setTitle("Test");
        campaign.setDescription("Desc");
        campaign.setTotalRaised(BigDecimal.ZERO);

        assertThat(campaign.getCreatedAt()).isNull();

        Campaign result = service.createCampaign(campaign);

        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getCreatedAt())
                .isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void createCampaign_setsTotalRaisedToZeroIfNull() {
        Campaign campaign = new Campaign();
        campaign.setTitle("Test");
        campaign.setDescription("Desc");
        campaign.setTotalRaised(null);

        Campaign result = service.createCampaign(campaign);

        assertThat(result.getTotalRaised()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getStatus()).isEqualTo(CampaignStatus.WAITING);
    }

    @Test
    void updateDescription_updatesOnlyDescription() {
        Campaign existing = new Campaign();
        existing.setTitle("Original title");
        existing.setDescription("Old description");
        existing.setTotalRaised(BigDecimal.ZERO);

        Campaign saved = repository.save(existing);
        Long id = saved.getId();

        Campaign updated = service.updateDescription(id, "New description");

        assertThat(updated.getId()).isEqualTo(id);
        assertThat(updated.getTitle()).isEqualTo("Original title");
        assertThat(updated.getDescription()).isEqualTo("New description");
    }

    @Test
    void deleteIfNoDonations_deletesWhenTotalRaisedZero() {
        Campaign campaign = new Campaign();
        campaign.setTitle("Deletable");
        campaign.setDescription("No donations yet");
        campaign.setTotalRaised(BigDecimal.ZERO);

        Campaign saved = repository.save(campaign);
        Long id = saved.getId();

        service.deleteIfNoDonations(id);

        assertThat(repository.findById(id)).isPresent();
        assertThat(repository.findById(id).orElseThrow().getStatus()).isEqualTo(CampaignStatus.DELETED);
    }

    @Test
    void deleteIfNoDonations_throwsBadRequestWhenTotalRaisedGreaterThanZero() {
        Campaign campaign = new Campaign();
        campaign.setTitle("Has donations");
        campaign.setDescription("Cannot delete");
        campaign.setTotalRaised(BigDecimal.ONE);

        Campaign saved = repository.save(campaign);
        Long id = saved.getId();

        assertThatThrownBy(() -> service.deleteIfNoDonations(id))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    void moderateCampaign_approve_waitingCampaignBecomesOpen() {
        Campaign campaign = new Campaign();
        campaign.setTitle("Waiting campaign");
        campaign.setDescription("To review");
        campaign.setStatus(CampaignStatus.WAITING);
        Campaign saved = repository.save(campaign);

        Campaign moderated = service.moderateCampaign(saved.getId(), true);

        assertThat(moderated.getStatus()).isEqualTo(CampaignStatus.OPEN);
    }

    @Test
    void moderateCampaign_reject_waitingCampaignPublishesRejectedCampaignEventWithCreatorId() {
        Campaign campaign = new Campaign();
        campaign.setTitle("Waiting campaign");
        campaign.setDescription("To review");
        campaign.setStatus(CampaignStatus.WAITING);
        campaign.setCreatorId("creator-123");
        Campaign saved = repository.save(campaign);

        Campaign moderated = service.moderateCampaign(saved.getId(), false);

        assertThat(moderated.getStatus()).isEqualTo(CampaignStatus.REJECTED);
        assertThat(eventPublisher.publishedEvents)
                .hasSize(1)
                .first()
                .isInstanceOfSatisfying(RejectedCampaignEvent.class, event -> {
                    assertThat(event.getCreatorId()).isEqualTo("creator-123");
                    assertThat(event.getCampaign().getId()).isEqualTo(saved.getId());
                });
    }

    @Test
    void recordSuccessfulDonation_closesCampaignWhenTargetReached() {
        Campaign campaign = new Campaign();
        campaign.setTitle("Donation target");
        campaign.setDescription("Desc");
        campaign.setStatus(CampaignStatus.OPEN);
        campaign.setTargetAmount(new BigDecimal("100"));
        campaign.setTotalRaised(new BigDecimal("90"));
        Campaign saved = repository.save(campaign);

        Campaign updated = service.recordSuccessfulDonation(saved.getId(), new BigDecimal("15"));

        assertThat(updated.getTotalRaised()).isEqualByComparingTo(new BigDecimal("105"));
        assertThat(updated.getStatus()).isEqualTo(CampaignStatus.CLOSED);
    }

    @Test
    void processExpiredCampaigns_closesSuccessfulCampaignAndRequestsPayout() {
        Campaign campaign = new Campaign();
        campaign.setTitle("Success");
        campaign.setDescription("Reached target");
        campaign.setStatus(CampaignStatus.OPEN);
        campaign.setDeadline(LocalDate.now().minusDays(1));
        campaign.setTargetAmount(new BigDecimal("100"));
        campaign.setTotalRaised(new BigDecimal("120"));
        Campaign saved = repository.save(campaign);

        int processed = service.processExpiredCampaigns(LocalDate.now());

        assertThat(processed).isEqualTo(1);
        assertThat(repository.findById(saved.getId()).orElseThrow().getStatus()).isEqualTo(CampaignStatus.CLOSED);
        assertThat(walletGateway.payoutRequestedCount).isEqualTo(1);
        assertThat(walletGateway.refundRequestedCount).isZero();
        assertThat(eventPublisher.publishedCount).isEqualTo(1);
    }

    @Test
    void processExpiredCampaigns_cancelsFailedCampaignAndRequestsRefund() {
        Campaign campaign = new Campaign();
        campaign.setTitle("Failed");
        campaign.setDescription("Did not hit target");
        campaign.setStatus(CampaignStatus.OPEN);
        campaign.setDeadline(LocalDate.now().minusDays(1));
        campaign.setTargetAmount(new BigDecimal("100"));
        campaign.setTotalRaised(new BigDecimal("10"));
        Campaign saved = repository.save(campaign);

        int processed = service.processExpiredCampaigns(LocalDate.now());

        assertThat(processed).isEqualTo(1);
        assertThat(repository.findById(saved.getId()).orElseThrow().getStatus()).isEqualTo(CampaignStatus.CANCELLED);
        assertThat(walletGateway.refundRequestedCount).isEqualTo(1);
        assertThat(walletGateway.payoutRequestedCount).isZero();
        assertThat(eventPublisher.publishedCount).isEqualTo(1);
    }

    @Test
    void markAsFraud_updatesStatusAndRequestsRefund() {
        Campaign campaign = new Campaign();
        campaign.setTitle("Suspicious");
        campaign.setDescription("Flagged");
        campaign.setStatus(CampaignStatus.OPEN);
        campaign.setTotalRaised(new BigDecimal("20"));
        Campaign saved = repository.save(campaign);

        Campaign updated = service.markAsFraud(saved.getId());

        assertThat(updated.getStatus()).isEqualTo(CampaignStatus.FRAUD);
        assertThat(walletGateway.refundRequestedCount).isEqualTo(1);
        assertThat(eventPublisher.publishedCount).isEqualTo(2);
    }

    private static class RecordingCampaignWalletGateway implements CampaignWalletGateway {
        private int payoutRequestedCount;
        private int refundRequestedCount;

        @Override
        public void requestPayout(Campaign campaign) {
            payoutRequestedCount++;
        }

        @Override
        public void requestRefund(Campaign campaign) {
            refundRequestedCount++;
        }
    }

    private static class RecordingEventPublisher implements ApplicationEventPublisher {
        private int publishedCount;
        private final java.util.List<Object> publishedEvents = new java.util.ArrayList<>();

        @Override
        public void publishEvent(Object event) {
            publishedCount++;
            publishedEvents.add(event);
        }
    }
}

