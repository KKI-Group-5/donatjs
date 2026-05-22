package id.ac.ui.cs.advprog.donatjs.repository;

import id.ac.ui.cs.advprog.donatjs.model.Campaign;
import id.ac.ui.cs.advprog.donatjs.model.CampaignStatus;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

@Repository
public class JpaCampaignRepository implements CampaignRepository {

    private final SpringDataCampaignRepository delegate;

    public JpaCampaignRepository(SpringDataCampaignRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public Campaign save(Campaign campaign) {
        return delegate.save(campaign);
    }

    @Override
    public List<Campaign> findAll() {
        return delegate.findAll();
    }

    @Override
    public Optional<Campaign> findById(Long id) {
        return delegate.findById(id);
    }

    @Override
    public List<Campaign> findByStatus(CampaignStatus status) {
        return delegate.findByStatus(status);
    }

    @Override
    public List<Campaign> findByCreatorId(String creatorId) {
        return delegate.findByCreatorId(creatorId);
    }

    @Override
    public void deleteById(Long id) {
        delegate.deleteById(id);
    }

    @Override
    @Transactional
    public Optional<Campaign> computeAndSave(Long id, UnaryOperator<Campaign> updater) {
        return delegate.findByIdForUpdate(id)
                .map(existing -> {
                    Campaign updated = updater.apply(existing);
                    return delegate.save(updated != null ? updated : existing);
                });
    }
}
