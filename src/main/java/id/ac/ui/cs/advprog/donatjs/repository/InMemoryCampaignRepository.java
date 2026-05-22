package id.ac.ui.cs.advprog.donatjs.repository;

import id.ac.ui.cs.advprog.donatjs.model.Campaign;
import id.ac.ui.cs.advprog.donatjs.model.CampaignStatus;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.UnaryOperator;

@Repository
public class InMemoryCampaignRepository implements CampaignRepository {

    private final Map<Long, Campaign> storage = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(1);

    @Override
    public Campaign save(Campaign campaign) {
        if (campaign.getId() == null) {
            campaign.setId(idSequence.getAndIncrement());
        }
        storage.put(campaign.getId(), campaign);
        return campaign;
    }

    @Override
    public List<Campaign> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public Optional<Campaign> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Campaign> findByStatus(CampaignStatus status) {
        List<Campaign> result = new ArrayList<>();
        for (Campaign campaign : storage.values()) {
            if (status.equals(campaign.getStatus())) {
                result.add(campaign);
            }
        }
        return result;
    }

    @Override
    public List<Campaign> findByCreatorId(String creatorId) {
        List<Campaign> result = new ArrayList<>();
        for (Campaign campaign : storage.values()) {
            if (creatorId != null && creatorId.equals(campaign.getCreatorId())) {
                result.add(campaign);
            }
        }
        return result;
    }

    @Override
    public void deleteById(Long id) {
        storage.remove(id);
    }

    @Override
    public Optional<Campaign> computeAndSave(Long id, UnaryOperator<Campaign> updater) {
        Campaign[] holder = {null};
        storage.compute(id, (key, existing) -> {
            if (existing == null) return null;
            Campaign updated = updater.apply(existing);
            holder[0] = updated;
            return updated != null ? updated : existing;
        });
        return Optional.ofNullable(holder[0]);
    }
}

