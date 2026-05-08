package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.event.CampaignNearTargetEvent;
import id.ac.ui.cs.advprog.donatjs.event.CampaignStatusChangedEvent;
import id.ac.ui.cs.advprog.donatjs.event.RejectedCampaignEvent;
import id.ac.ui.cs.advprog.donatjs.model.Campaign;
import id.ac.ui.cs.advprog.donatjs.model.CampaignStatus;
import id.ac.ui.cs.advprog.donatjs.repository.InMemoryCampaignRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SuppressWarnings("null")
class SimpleCampaignServiceTest {

    private InMemoryCampaignRepository repository;
    private ApplicationEventPublisher publisher;
    private SimpleCampaignService service;
    private RecordingCampaignWalletGateway walletGateway;
    private RecordingEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        repository = new InMemoryCampaignRepository();
        walletGateway = new RecordingCampaignWalletGateway();
        publisher = mock(ApplicationEventPublisher.class);
        eventPublisher = new RecordingEventPublisher(publisher);
        service = new SimpleCampaignService(repository, walletGateway, eventPublisher, new BigDecimal("0.98"));
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
        assertThat(result.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
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
    void deleteIfNoDonations_deletesAndPublishesStatusEvent() {
        Campaign campaign = new Campaign();
        campaign.setTitle("Deletable");
        campaign.setDescription("No donations yet");
        campaign.setTotalRaised(BigDecimal.ZERO);

        Campaign saved = repository.save(campaign);
        Long id = saved.getId();

        service.deleteIfNoDonations(id);

        assertThat(repository.findById(id)).isPresent();
        assertThat(repository.findById(id).orElseThrow().getStatus()).isEqualTo(CampaignStatus.DELETED);
        verify(publisher).publishEvent(any(CampaignStatusChangedEvent.class));
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
        verify(publisher, never()).publishEvent(any());
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
        verify(publisher).publishEvent(any(CampaignStatusChangedEvent.class));
    }

    @Test
    void moderateCampaign_reject_publishesRejectionStatusEvent() {
        Campaign campaign = new Campaign();
        campaign.setTitle("Waiting campaign");
        campaign.setDescription("To review");
        campaign.setStatus(CampaignStatus.WAITING);
        Campaign saved = repository.save(campaign);

        Campaign moderated = service.moderateCampaign(saved.getId(), false);

        assertThat(moderated.getStatus()).isEqualTo(CampaignStatus.REJECTED);

        org.mockito.ArgumentCaptor<CampaignStatusChangedEvent> captor =
                org.mockito.ArgumentCaptor.forClass(CampaignStatusChangedEvent.class);
        verify(publisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getNewStatus()).isEqualTo(CampaignStatus.REJECTED);
        assertThat(captor.getValue().shouldTerminateSubscriptions()).isTrue();
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
                .filteredOn(e -> e instanceof RejectedCampaignEvent)
                .hasSize(1)
                .first()
                .isInstanceOfSatisfying(RejectedCampaignEvent.class, event -> {
                    assertThat(event.getCreatorId()).isEqualTo("creator-123");
                    assertThat(event.getCampaign().getId()).isEqualTo(saved.getId());
                });
    }

    @Test
    void recordSuccessfulDonation_closesCampaignWhenTargetReached() {
        Campaign campaign = openCampaign(new BigDecimal("100"), new BigDecimal("90"));

        Campaign updated = service.recordSuccessfulDonation(campaign.getId(), new BigDecimal("15"));

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
        verify(publisher, times(1)).publishEvent(any());
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
        verify(publisher, times(1)).publishEvent(any());
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
        verify(publisher, times(2)).publishEvent(any());
    }

    // ── 98%-threshold trigger ────────────────────────────────────────────

    @Test
    void recordSuccessfulDonation_doesNotPublishNearTargetEvent_whenStillBelowThreshold() {
        Campaign campaign = openCampaign(new BigDecimal("1000"), new BigDecimal("950")); // 95%

        Campaign updated = service.recordSuccessfulDonation(campaign.getId(), new BigDecimal("20")); // 970 = 97%

        assertThat(updated.isNearTargetNotified()).isFalse();
        verify(publisher, never()).publishEvent(any(CampaignNearTargetEvent.class));
    }

    @Test
    void recordSuccessfulDonation_publishesNearTargetEvent_whenCrossingThreshold() {
        Campaign campaign = openCampaign(new BigDecimal("1000"), new BigDecimal("970")); // 97%

        Campaign updated = service.recordSuccessfulDonation(campaign.getId(), new BigDecimal("10")); // 980 = 98%

        assertThat(updated.isNearTargetNotified()).isTrue();
        org.mockito.ArgumentCaptor<CampaignNearTargetEvent> captor =
                org.mockito.ArgumentCaptor.forClass(CampaignNearTargetEvent.class);
        verify(publisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getCampaignId()).isEqualTo(campaign.getId());
        assertThat(captor.getValue().getTotalRaised()).isEqualByComparingTo(new BigDecimal("980"));
        assertThat(captor.getValue().getTargetAmount()).isEqualByComparingTo(new BigDecimal("1000"));
    }

    @Test
    void recordSuccessfulDonation_doesNotRepublishNearTargetEvent_whenAlreadyNotified() {
        Campaign campaign = openCampaign(new BigDecimal("1000"), new BigDecimal("980")); // already 98%
        campaign.setNearTargetNotified(true);
        repository.save(campaign);

        service.recordSuccessfulDonation(campaign.getId(), new BigDecimal("5")); // 985

        verify(publisher, never()).publishEvent(any(CampaignNearTargetEvent.class));
    }

    @Test
    void recordSuccessfulDonation_publishesBothNearTargetAndClosedEvents_whenCrossingDirectlyToTarget() {
        Campaign campaign = openCampaign(new BigDecimal("1000"), new BigDecimal("100")); // 10%

        service.recordSuccessfulDonation(campaign.getId(), new BigDecimal("950")); // jumps to 1050 = 105%

        // both events fire
        verify(publisher).publishEvent(any(CampaignNearTargetEvent.class));
        verify(publisher, times(2)).publishEvent(any()); // total of 2 events
    }

    private Campaign openCampaign(BigDecimal target, BigDecimal raised) {
        Campaign c = new Campaign();
        c.setTitle("c");
        c.setDescription("d");
        c.setStatus(CampaignStatus.OPEN);
        c.setTargetAmount(target);
        c.setTotalRaised(raised);
        return repository.save(c);
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
        private final List<Object> publishedEvents = new ArrayList<>();
        private final ApplicationEventPublisher delegate;

        RecordingEventPublisher(ApplicationEventPublisher delegate) {
            this.delegate = delegate;
        }

        @Override
        public void publishEvent(Object event) {
            publishedCount++;
            publishedEvents.add(event);
            delegate.publishEvent(event);
        }
    }
}
