package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.event.CampaignFraudDetectedEvent;
import id.ac.ui.cs.advprog.donatjs.event.CampaignPayoutRequestedEvent;
import id.ac.ui.cs.advprog.donatjs.event.CampaignRefundRequestedEvent;
import id.ac.ui.cs.advprog.donatjs.event.RejectedCampaignEvent;
import id.ac.ui.cs.advprog.donatjs.model.Campaign;
import id.ac.ui.cs.advprog.donatjs.model.CampaignStatus;
import id.ac.ui.cs.advprog.donatjs.repository.InMemoryCampaignRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    // ── createCampaign ───────────────────────────────────────────────────────────

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
    void createCampaign_setsCreatorId() {
        Campaign campaign = new Campaign();
        campaign.setTitle("Title");
        campaign.setDescription("Desc");

        Campaign result = service.createCampaign(campaign, "user-42");

        assertThat(result.getCreatorId()).isEqualTo("user-42");
        assertThat(result.getStatus()).isEqualTo(CampaignStatus.WAITING);
    }

    @Test
    void createCampaign_preservesExistingCreatedAt() {
        LocalDateTime fixedTime = LocalDateTime.of(2025, 1, 1, 12, 0);
        Campaign campaign = new Campaign();
        campaign.setTitle("Title");
        campaign.setDescription("Desc");
        campaign.setCreatedAt(fixedTime);

        Campaign result = service.createCampaign(campaign);

        assertThat(result.getCreatedAt()).isEqualTo(fixedTime);
    }

    // ── findOpenCampaigns / findById ─────────────────────────────────────────────

    @Test
    void findOpenCampaigns_returnsOnlyOpenCampaigns() {
        Campaign open = new Campaign();
        open.setTitle("Open");
        open.setDescription("Open desc");
        open.setStatus(CampaignStatus.OPEN);
        repository.save(open);

        Campaign waiting = new Campaign();
        waiting.setTitle("Waiting");
        waiting.setDescription("Waiting desc");
        waiting.setStatus(CampaignStatus.WAITING);
        repository.save(waiting);

        Campaign rejected = new Campaign();
        rejected.setTitle("Rejected");
        rejected.setDescription("Rejected desc");
        rejected.setStatus(CampaignStatus.REJECTED);
        repository.save(rejected);

        List<Campaign> result = service.findOpenCampaigns();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(CampaignStatus.OPEN);
    }

    @Test
    void findById_returnsPresent_forExistingCampaign() {
        Campaign campaign = new Campaign();
        campaign.setTitle("Find me");
        campaign.setDescription("Desc");
        Campaign saved = repository.save(campaign);

        Optional<Campaign> result = service.findById(saved.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    void findById_returnsEmpty_forNonExistentId() {
        Optional<Campaign> result = service.findById(999L);

        assertThat(result).isEmpty();
    }

    // ── updateDescription ────────────────────────────────────────────────────────

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
    void updateDescription_succeeds_whenActorIsCreator() {
        Campaign campaign = new Campaign();
        campaign.setTitle("Title");
        campaign.setDescription("Old");
        campaign.setStatus(CampaignStatus.OPEN);
        campaign.setCreatorId("creator-1");
        Campaign saved = repository.save(campaign);

        Campaign updated = service.updateDescription(saved.getId(), "creator-1", false, "Updated");

        assertThat(updated.getDescription()).isEqualTo("Updated");
    }

    @Test
    void updateDescription_throwsForbidden_whenActorIsNotCreator() {
        Campaign campaign = new Campaign();
        campaign.setTitle("Title");
        campaign.setDescription("Desc");
        campaign.setStatus(CampaignStatus.OPEN);
        campaign.setCreatorId("owner");
        Campaign saved = repository.save(campaign);

        assertThatThrownBy(() ->
                service.updateDescription(saved.getId(), "other-user", false, "Hack"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void updateDescription_succeeds_whenAdminEditsAnyCreatorCampaign() {
        Campaign campaign = new Campaign();
        campaign.setTitle("Title");
        campaign.setDescription("Desc");
        campaign.setStatus(CampaignStatus.OPEN);
        campaign.setCreatorId("some-creator");
        Campaign saved = repository.save(campaign);

        Campaign updated = service.updateDescription(saved.getId(), "admin-id", true, "Admin override");

        assertThat(updated.getDescription()).isEqualTo("Admin override");
    }

    @Test
    void updateDescription_throwsBadRequest_whenCampaignIsRejected() {
        Campaign campaign = new Campaign();
        campaign.setTitle("Title");
        campaign.setDescription("Desc");
        campaign.setStatus(CampaignStatus.REJECTED);
        Campaign saved = repository.save(campaign);

        assertThatThrownBy(() -> service.updateDescription(saved.getId(), "New desc"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    // ── deleteIfNoDonations ──────────────────────────────────────────────────────

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
    void deleteIfNoDonations_deletesWhenTotalRaisedIsNull() {
        Campaign campaign = new Campaign();
        campaign.setTitle("Null raised");
        campaign.setDescription("Desc");
        campaign.setTotalRaised(null);
        Campaign saved = repository.save(campaign);

        service.deleteIfNoDonations(saved.getId());

        assertThat(repository.findById(saved.getId()).orElseThrow().getStatus())
                .isEqualTo(CampaignStatus.DELETED);
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
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void deleteIfNoDonations_throwsForbidden_whenActorIsNotCreator() {
        Campaign campaign = new Campaign();
        campaign.setTitle("Title");
        campaign.setDescription("Desc");
        campaign.setTotalRaised(BigDecimal.ZERO);
        campaign.setCreatorId("owner");
        Campaign saved = repository.save(campaign);

        assertThatThrownBy(() -> service.deleteIfNoDonations(saved.getId(), "intruder", false))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }

    // ── moderateCampaign ─────────────────────────────────────────────────────────

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
    void moderateCampaign_throwsBadRequest_whenCampaignIsNotWaiting() {
        Campaign campaign = new Campaign();
        campaign.setTitle("Already open");
        campaign.setDescription("Desc");
        campaign.setStatus(CampaignStatus.OPEN);
        Campaign saved = repository.save(campaign);

        assertThatThrownBy(() -> service.moderateCampaign(saved.getId(), true))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void moderateCampaign_approve_doesNotPublishEvent() {
        Campaign campaign = new Campaign();
        campaign.setTitle("Waiting");
        campaign.setDescription("Desc");
        campaign.setStatus(CampaignStatus.WAITING);
        Campaign saved = repository.save(campaign);

        service.moderateCampaign(saved.getId(), true);

        assertThat(eventPublisher.publishedEvents).isEmpty();
    }

    // ── adminUpdateCampaign ──────────────────────────────────────────────────────

    @Test
    void adminUpdateCampaign_updatesTitle() {
        Campaign campaign = new Campaign();
        campaign.setTitle("Old title");
        campaign.setDescription("Desc");
        Campaign saved = repository.save(campaign);

        Campaign updated = service.adminUpdateCampaign(saved.getId(), "New title", null, null);

        assertThat(updated.getTitle()).isEqualTo("New title");
    }

    @Test
    void adminUpdateCampaign_updatesDeadline() {
        Campaign campaign = new Campaign();
        campaign.setTitle("Title");
        campaign.setDescription("Desc");
        Campaign saved = repository.save(campaign);

        LocalDate future = LocalDate.now().plusDays(30);
        Campaign updated = service.adminUpdateCampaign(saved.getId(), null, future, null);

        assertThat(updated.getDeadline()).isEqualTo(future);
    }

    @Test
    void adminUpdateCampaign_throwsBadRequest_whenDeadlineIsInPast() {
        Campaign campaign = new Campaign();
        campaign.setTitle("Title");
        campaign.setDescription("Desc");
        Campaign saved = repository.save(campaign);

        LocalDate past = LocalDate.now().minusDays(1);

        assertThatThrownBy(() -> service.adminUpdateCampaign(saved.getId(), null, past, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void adminUpdateCampaign_throwsBadRequest_whenTargetAmountIsZero() {
        Campaign campaign = new Campaign();
        campaign.setTitle("Title");
        campaign.setDescription("Desc");
        Campaign saved = repository.save(campaign);

        assertThatThrownBy(() -> service.adminUpdateCampaign(saved.getId(), null, null, BigDecimal.ZERO))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void adminUpdateCampaign_autoClosesOpenCampaignWhenTargetAlreadyReached() {
        Campaign campaign = new Campaign();
        campaign.setTitle("Title");
        campaign.setDescription("Desc");
        campaign.setStatus(CampaignStatus.OPEN);
        campaign.setTotalRaised(new BigDecimal("500"));
        campaign.setTargetAmount(new BigDecimal("1000"));
        Campaign saved = repository.save(campaign);

        // Lower the target so that totalRaised >= new target
        Campaign updated = service.adminUpdateCampaign(saved.getId(), null, null, new BigDecimal("400"));

        assertThat(updated.getStatus()).isEqualTo(CampaignStatus.CLOSED);
    }

    // ── recordSuccessfulDonation ─────────────────────────────────────────────────

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
    void recordSuccessfulDonation_throwsBadRequest_whenCampaignNotOpen() {
        Campaign campaign = new Campaign();
        campaign.setTitle("Closed campaign");
        campaign.setDescription("Desc");
        campaign.setStatus(CampaignStatus.CLOSED);
        campaign.setTotalRaised(BigDecimal.ZERO);
        Campaign saved = repository.save(campaign);

        assertThatThrownBy(() -> service.recordSuccessfulDonation(saved.getId(), BigDecimal.ONE))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void recordSuccessfulDonation_throwsBadRequest_whenCampaignIsCancelled() {
        Campaign campaign = new Campaign();
        campaign.setTitle("Cancelled");
        campaign.setDescription("Desc");
        campaign.setStatus(CampaignStatus.CANCELLED);
        campaign.setTotalRaised(BigDecimal.ZERO);
        Campaign saved = repository.save(campaign);

        assertThatThrownBy(() -> service.recordSuccessfulDonation(saved.getId(), BigDecimal.ONE))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void recordSuccessfulDonation_throwsBadRequest_whenAmountIsNegative() {
        Campaign campaign = new Campaign();
        campaign.setTitle("Title");
        campaign.setDescription("Desc");
        campaign.setStatus(CampaignStatus.OPEN);
        campaign.setTotalRaised(BigDecimal.ZERO);
        Campaign saved = repository.save(campaign);

        assertThatThrownBy(() ->
                service.recordSuccessfulDonation(saved.getId(), new BigDecimal("-5")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void recordSuccessfulDonation_throwsBadRequest_whenAmountIsNull() {
        Campaign campaign = new Campaign();
        campaign.setTitle("Title");
        campaign.setDescription("Desc");
        campaign.setStatus(CampaignStatus.OPEN);
        campaign.setTotalRaised(BigDecimal.ZERO);
        Campaign saved = repository.save(campaign);

        assertThatThrownBy(() -> service.recordSuccessfulDonation(saved.getId(), null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void recordSuccessfulDonation_throwsNotFound_whenCampaignDoesNotExist() {
        assertThatThrownBy(() -> service.recordSuccessfulDonation(999L, BigDecimal.ONE))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ── markAsFraud ──────────────────────────────────────────────────────────────

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

    @Test
    void markAsFraud_publishesFraudEventOnly_whenNoRaisedAmount() {
        Campaign campaign = new Campaign();
        campaign.setTitle("Suspicious");
        campaign.setDescription("Flagged but no donations");
        campaign.setStatus(CampaignStatus.OPEN);
        campaign.setTotalRaised(BigDecimal.ZERO);
        Campaign saved = repository.save(campaign);

        service.markAsFraud(saved.getId());

        assertThat(walletGateway.refundRequestedCount).isZero();
        assertThat(eventPublisher.publishedCount).isEqualTo(1);
        assertThat(eventPublisher.publishedEvents.get(0))
                .isInstanceOf(CampaignFraudDetectedEvent.class);
    }

    @Test
    void markAsFraud_throwsBadRequest_whenCampaignIsDeleted() {
        Campaign campaign = new Campaign();
        campaign.setTitle("Deleted");
        campaign.setDescription("Desc");
        campaign.setStatus(CampaignStatus.DELETED);
        Campaign saved = repository.save(campaign);

        assertThatThrownBy(() -> service.markAsFraud(saved.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void markAsFraud_throwsBadRequest_whenCampaignIsCancelled() {
        Campaign campaign = new Campaign();
        campaign.setTitle("Cancelled");
        campaign.setDescription("Desc");
        campaign.setStatus(CampaignStatus.CANCELLED);
        Campaign saved = repository.save(campaign);

        assertThatThrownBy(() -> service.markAsFraud(saved.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    // ── processExpiredCampaigns ──────────────────────────────────────────────────

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
        assertThat(eventPublisher.publishedEvents.get(0))
                .isInstanceOf(CampaignPayoutRequestedEvent.class);
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
        assertThat(eventPublisher.publishedEvents.get(0))
                .isInstanceOf(CampaignRefundRequestedEvent.class);
    }

    @Test
    void processExpiredCampaigns_cancelsExpiredCampaignWithNoDonations_noRefundRequested() {
        Campaign campaign = new Campaign();
        campaign.setTitle("Zero donations");
        campaign.setDescription("No one donated");
        campaign.setStatus(CampaignStatus.OPEN);
        campaign.setDeadline(LocalDate.now().minusDays(1));
        campaign.setTargetAmount(new BigDecimal("500"));
        campaign.setTotalRaised(BigDecimal.ZERO);
        Campaign saved = repository.save(campaign);

        int processed = service.processExpiredCampaigns(LocalDate.now());

        assertThat(processed).isEqualTo(1);
        assertThat(repository.findById(saved.getId()).orElseThrow().getStatus()).isEqualTo(CampaignStatus.CANCELLED);
        assertThat(walletGateway.refundRequestedCount).isZero();
        assertThat(eventPublisher.publishedCount).isZero();
    }

    @Test
    void processExpiredCampaigns_processesWaitingExpiredCampaign() {
        Campaign campaign = new Campaign();
        campaign.setTitle("Waiting expired");
        campaign.setDescription("Never approved but passed deadline");
        campaign.setStatus(CampaignStatus.WAITING);
        campaign.setDeadline(LocalDate.now().minusDays(1));
        campaign.setTargetAmount(new BigDecimal("100"));
        campaign.setTotalRaised(BigDecimal.ZERO);
        Campaign saved = repository.save(campaign);

        int processed = service.processExpiredCampaigns(LocalDate.now());

        assertThat(processed).isEqualTo(1);
        assertThat(repository.findById(saved.getId()).orElseThrow().getStatus()).isEqualTo(CampaignStatus.CANCELLED);
    }

    @Test
    void processExpiredCampaigns_skipsNonExpiredCampaigns() {
        Campaign campaign = new Campaign();
        campaign.setTitle("Active");
        campaign.setDescription("Still running");
        campaign.setStatus(CampaignStatus.OPEN);
        campaign.setDeadline(LocalDate.now().plusDays(5));
        campaign.setTargetAmount(new BigDecimal("100"));
        campaign.setTotalRaised(BigDecimal.ZERO);
        repository.save(campaign);

        int processed = service.processExpiredCampaigns(LocalDate.now());

        assertThat(processed).isZero();
    }

    @Test
    void processExpiredCampaigns_skipsAlreadyClosedCampaign() {
        Campaign campaign = new Campaign();
        campaign.setTitle("Already closed");
        campaign.setDescription("Closed before deadline run");
        campaign.setStatus(CampaignStatus.CLOSED);
        campaign.setDeadline(LocalDate.now().minusDays(1));
        campaign.setTargetAmount(new BigDecimal("100"));
        campaign.setTotalRaised(new BigDecimal("150"));
        Campaign saved = repository.save(campaign);

        int processed = service.processExpiredCampaigns(LocalDate.now());

        assertThat(processed).isZero();
        assertThat(repository.findById(saved.getId()).orElseThrow().getStatus()).isEqualTo(CampaignStatus.CLOSED);
        assertThat(walletGateway.payoutRequestedCount).isZero();
    }

    @Test
    void processExpiredCampaigns_returnsZero_whenNoCampaigns() {
        int processed = service.processExpiredCampaigns(LocalDate.now());
        assertThat(processed).isZero();
    }

    // ── helpers ──────────────────────────────────────────────────────────────────

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

        @Override
        public void publishEvent(Object event) {
            publishedCount++;
            publishedEvents.add(event);
        }
    }
}
