package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.model.Campaign;
import id.ac.ui.cs.advprog.donatjs.model.CampaignStatus;
import id.ac.ui.cs.advprog.donatjs.repository.InMemoryCampaignRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import org.springframework.context.ApplicationEventPublisher;

class SimpleCampaignServiceTest {

    private InMemoryCampaignRepository repository;
    private ApplicationEventPublisher eventPublisher;
    private SimpleCampaignService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryCampaignRepository();
        eventPublisher = mock(ApplicationEventPublisher.class);
        service = new SimpleCampaignService(repository, eventPublisher);
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
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void moderateCampaign_reject_publishesEvent() {
        Campaign campaign = new Campaign();
        campaign.setTitle("Waiting campaign");
        campaign.setDescription("To review");
        campaign.setStatus(CampaignStatus.WAITING);
        Campaign saved = repository.save(campaign);

        Campaign moderated = service.moderateCampaign(saved.getId(), false);

        assertThat(moderated.getStatus()).isEqualTo(CampaignStatus.REJECTED);
        verify(eventPublisher).publishEvent(any(id.ac.ui.cs.advprog.donatjs.event.RejectedCampaignEvent.class));
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
}

