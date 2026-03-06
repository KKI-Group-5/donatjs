package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.model.Campaign;
import id.ac.ui.cs.advprog.donatjs.repository.CampaignRepository;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SimpleCampaignService implements CampaignService {

    private final CampaignRepository campaignRepository;

    public SimpleCampaignService(CampaignRepository campaignRepository) {
        this.campaignRepository = campaignRepository;
    }

    @Override
    public Campaign createCampaign(Campaign campaign) {
        if (campaign.getCreatedAt() == null) {
            campaign.setCreatedAt(LocalDateTime.now());
        }
        if (campaign.getTotalRaised() == null) {
            campaign.setTotalRaised(java.math.BigDecimal.ZERO);
        }
        return campaignRepository.save(campaign);
    }

    @Override
    public List<Campaign> findOpenCampaigns() {
        return campaignRepository.findByStatus(id.ac.ui.cs.advprog.donatjs.model.CampaignStatus.OPEN);
    }

    @Override
    public Optional<Campaign> findById(Long id) {
        return campaignRepository.findById(id);
    }

    @Override
    public Campaign updateDescription(Long id, String description) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        campaign.setDescription(description);
        return campaignRepository.save(campaign);
    }

    @Override
    public void deleteIfNoDonations(Long id) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (campaign.getTotalRaised() != null
                && campaign.getTotalRaised().compareTo(java.math.BigDecimal.ZERO) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot delete campaign with donations"
            );
        }

        campaignRepository.deleteById(id);
    }
}

