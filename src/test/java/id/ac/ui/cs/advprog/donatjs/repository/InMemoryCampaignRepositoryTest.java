package id.ac.ui.cs.advprog.donatjs.repository;

import id.ac.ui.cs.advprog.donatjs.model.Campaign;
import id.ac.ui.cs.advprog.donatjs.model.CampaignStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
