package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.model.Campaign;
import id.ac.ui.cs.advprog.donatjs.repository.InMemoryCampaignRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimpleCampaignServiceTest {

    private InMemoryCampaignRepository repository;
    private SimpleCampaignService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryCampaignRepository();
        service = new SimpleCampaignService(repository);
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

        assertThat(repository.findById(id)).isEmpty();
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
}

