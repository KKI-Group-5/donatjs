package id.ac.ui.cs.advprog.donatjs.repository;

import id.ac.ui.cs.advprog.donatjs.model.Campaign;
import id.ac.ui.cs.advprog.donatjs.model.CampaignStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryCampaignRepositoryTest {

    private InMemoryCampaignRepository repository;
    private Campaign activeCampaign;
    private Campaign closedCampaign;
    private Campaign deletedCampaign;

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

        activeCampaign = new Campaign();
        activeCampaign.setId(1L);
        activeCampaign.setTitle("Save the forest");
        activeCampaign.setStatus(CampaignStatus.OPEN);
        activeCampaign.setCreatorId("user-1");
        activeCampaign.setDeadline(LocalDate.now().plusDays(10));

        closedCampaign = new Campaign();
        closedCampaign.setId(2L);
        closedCampaign.setTitle("Clean the ocean");
        closedCampaign.setStatus(CampaignStatus.CLOSED);
        closedCampaign.setCreatorId("user-1");
        closedCampaign.setDeadline(LocalDate.now().minusDays(1));

        deletedCampaign = new Campaign();
        deletedCampaign.setId(3L);
        deletedCampaign.setTitle("Deleted project");
        deletedCampaign.setStatus(CampaignStatus.DELETED);
        deletedCampaign.setCreatorId("user-2");
        deletedCampaign.setDeadline(LocalDate.now().plusDays(10));

        repository.save(activeCampaign);
        repository.save(closedCampaign);
        repository.save(deletedCampaign);
    }

    @Test
    void findById_ReturnsCampaign() {
        Optional<Campaign> result = repository.findById(1L);
        assertThat(result).isPresent().contains(activeCampaign);
    }

    @Test
    void save_UpdatesExistingCampaign() {
        activeCampaign.setTitle("Updated Title");
        repository.save(activeCampaign);
        
        Optional<Campaign> result = repository.findById(1L);
        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("Updated Title");
    }

    @Test
    void deleteById_RemovesCampaign() {
        repository.deleteById(1L);
        assertThat(repository.findById(1L)).isEmpty();
    }

    @Test
    void findAll_ReturnsAllActiveAndClosedCampaigns() {
        List<Campaign> result = repository.findAll();
        assertThat(result).hasSize(3).contains(activeCampaign, closedCampaign, deletedCampaign);
    }

    @Test
    void findByStatus_ReturnsMatchingCampaigns() {
        List<Campaign> result = repository.findByStatus(CampaignStatus.OPEN);
        assertThat(result).hasSize(1).contains(activeCampaign);
    }

    @Test
    void findByCreatorId_ReturnsMatchingCampaigns() {
        List<Campaign> result = repository.findByCreatorId("user-1");
        assertThat(result).hasSize(2).contains(activeCampaign, closedCampaign);
    }

    @Test
    void computeAndSave_UpdatesCampaign() {
        repository.computeAndSave(1L, campaign -> {
            campaign.setTitle("Updated via compute");
            return campaign;
        });

        Optional<Campaign> result = repository.findById(1L);
        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("Updated via compute");
    }
}
