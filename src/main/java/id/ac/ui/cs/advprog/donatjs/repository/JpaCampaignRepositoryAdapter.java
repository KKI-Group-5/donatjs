package id.ac.ui.cs.advprog.donatjs.repository;

import id.ac.ui.cs.advprog.donatjs.model.Campaign;
import id.ac.ui.cs.advprog.donatjs.model.CampaignStatus;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

@Repository
@Primary
public class JpaCampaignRepositoryAdapter implements CampaignRepository {

    private final JpaCampaignStore store;

    public JpaCampaignRepositoryAdapter(JpaCampaignStore store) {
        this.store = store;
    }

    @Override
    public Campaign save(Campaign campaign) {
        return store.save(campaign);
    }

    @Override
    public List<Campaign> findAll() {
        return store.findAll();
    }

    @Override
    public Optional<Campaign> findById(Long id) {
        return store.findById(id);
    }

    @Override
    public List<Campaign> findByStatus(CampaignStatus status) {
        return store.findByStatus(status);
    }

    @Override
    public List<Campaign> findByCreatorId(String creatorId) {
        return store.findByCreatorId(creatorId);
    }

    @Override
    public void deleteById(Long id) {
        store.deleteById(id);
    }

    @Override
    @Transactional
    public Optional<Campaign> computeAndSave(Long id, UnaryOperator<Campaign> updater) {
        return store.findByIdForUpdate(id).map(campaign -> {
            Campaign updated = updater.apply(campaign);
            return updated != null ? store.save(updated) : campaign;
        });
    }
}
