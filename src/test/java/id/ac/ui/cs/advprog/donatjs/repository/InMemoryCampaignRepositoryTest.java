package id.ac.ui.cs.advprog.donatjs.repository;

import id.ac.ui.cs.advprog.donatjs.model.Campaign;
import id.ac.ui.cs.advprog.donatjs.model.CampaignStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryCampaignRepositoryTest {

    private InMemoryCampaignRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryCampaignRepository();
    }

    private Campaign buildCampaign(String title, CampaignStatus status, String creatorId) {
        Campaign c = new Campaign();
        c.setTitle(title);
        c.setDescription("desc");
        c.setDeadline(LocalDate.now().plusDays(10));
        c.setTargetAmount(new BigDecimal("1000"));
        c.setTotalRaised(BigDecimal.ZERO);
        c.setStatus(status);
        c.setCreatorId(creatorId);
        return c;
    }

    @Test
    void save_assignsIdIfNull() {
        Campaign c = buildCampaign("Camp 1", CampaignStatus.OPEN, "user-1");
        Campaign saved = repository.save(c);
        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void save_keepsExistingId() {
        Campaign c = buildCampaign("Camp 1", CampaignStatus.OPEN, "user-1");
        c.setId(42L);
        Campaign saved = repository.save(c);
        assertThat(saved.getId()).isEqualTo(42L);
    }

    @Test
    void findAll_returnsAllSaved() {
        repository.save(buildCampaign("A", CampaignStatus.OPEN, "u1"));
        repository.save(buildCampaign("B", CampaignStatus.WAITING, "u2"));
        assertThat(repository.findAll()).hasSize(2);
    }

    @Test
    void findById_found_returnsOptional() {
        Campaign saved = repository.save(buildCampaign("Camp", CampaignStatus.OPEN, "u1"));
        Optional<Campaign> result = repository.findById(saved.getId());
        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("Camp");
    }

    @Test
    void findById_notFound_returnsEmpty() {
        assertThat(repository.findById(9999L)).isEmpty();
    }

    @Test
    void findByStatus_returnsMatchingCampaigns() {
        repository.save(buildCampaign("Open1", CampaignStatus.OPEN, "u1"));
        repository.save(buildCampaign("Open2", CampaignStatus.OPEN, "u1"));
        repository.save(buildCampaign("Waiting1", CampaignStatus.WAITING, "u2"));

        List<Campaign> open = repository.findByStatus(CampaignStatus.OPEN);
        assertThat(open).hasSize(2);
        assertThat(open).allMatch(c -> c.getStatus() == CampaignStatus.OPEN);
    }

    @Test
    void findByCreatorId_returnsOnlyMatchingCreator() {
        repository.save(buildCampaign("Mine", CampaignStatus.OPEN, "creator-1"));
        repository.save(buildCampaign("Theirs", CampaignStatus.OPEN, "creator-2"));

        List<Campaign> mine = repository.findByCreatorId("creator-1");
        assertThat(mine).hasSize(1);
        assertThat(mine.get(0).getTitle()).isEqualTo("Mine");
    }

    @Test
    void findByCreatorId_nullCreatorId_doesNotMatch() {
        Campaign c = buildCampaign("NoCreator", CampaignStatus.OPEN, null);
        repository.save(c);
        List<Campaign> result = repository.findByCreatorId("creator-1");
        assertThat(result).isEmpty();
    }

    @Test
    void findByCreatorId_nullArgument_returnsEmpty() {
        repository.save(buildCampaign("Camp", CampaignStatus.OPEN, "creator-1"));
        List<Campaign> result = repository.findByCreatorId(null);
        assertThat(result).isEmpty();
    }

    @Test
    void deleteById_removesEntry() {
        Campaign saved = repository.save(buildCampaign("ToDelete", CampaignStatus.OPEN, "u1"));
        Long id = saved.getId();
        repository.deleteById(id);
        assertThat(repository.findById(id)).isEmpty();
    }

    @Test
    void computeAndSave_existingEntry_updatesAndReturns() {
        Campaign saved = repository.save(buildCampaign("Original", CampaignStatus.WAITING, "u1"));
        Long id = saved.getId();

        Optional<Campaign> result = repository.computeAndSave(id, c -> {
            c.setStatus(CampaignStatus.OPEN);
            return c;
        });

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(CampaignStatus.OPEN);
        assertThat(repository.findById(id).get().getStatus()).isEqualTo(CampaignStatus.OPEN);
    }

    @Test
    void computeAndSave_notFound_returnsEmpty() {
        Optional<Campaign> result = repository.computeAndSave(9999L, c -> c);
        assertThat(result).isEmpty();
    }

    @Test
    void computeAndSave_updaterReturnsNull_doesNotOverwrite() {
        Campaign saved = repository.save(buildCampaign("Safe", CampaignStatus.OPEN, "u1"));
        Long id = saved.getId();

        Optional<Campaign> result = repository.computeAndSave(id, c -> null);

        assertThat(result).isEmpty();
        assertThat(repository.findById(id)).isPresent();
    }

    @Test
    void idsAreAutoIncrementedSequentially() {
        Campaign c1 = repository.save(buildCampaign("C1", CampaignStatus.OPEN, "u1"));
        Campaign c2 = repository.save(buildCampaign("C2", CampaignStatus.OPEN, "u1"));
        assertThat(c2.getId()).isGreaterThan(c1.getId());
    }
}
