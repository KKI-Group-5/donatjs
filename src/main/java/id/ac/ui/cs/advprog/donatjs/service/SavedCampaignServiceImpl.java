package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.model.SavedCampaign;
import id.ac.ui.cs.advprog.donatjs.repository.SavedCampaignRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SavedCampaignServiceImpl implements SavedCampaignService {

    private final SavedCampaignRepository savedCampaignRepository;

    public SavedCampaignServiceImpl(SavedCampaignRepository savedCampaignRepository) {
        this.savedCampaignRepository = savedCampaignRepository;
    }

    @Override
    @Transactional
    public SavedCampaign saveCampaign(String userId, String campaignId, String campaignTitle,
                                      String campaignOrganizer, String campaignImageUrl) {
        if (savedCampaignRepository.existsByUserIdAndCampaignId(userId, campaignId)) {
            throw new IllegalStateException("Campaign is already saved");
        }

        SavedCampaign savedCampaign = SavedCampaign.builder()
                .userId(userId)
                .campaignId(campaignId)
                .campaignTitle(campaignTitle)
                .campaignOrganizer(campaignOrganizer)
                .campaignImageUrl(campaignImageUrl)
                .build();

        return savedCampaignRepository.save(savedCampaign);
    }

    @Override
    @Transactional
    public void removeSavedCampaign(String userId, String campaignId) {
        SavedCampaign savedCampaign = savedCampaignRepository
                .findByUserIdAndCampaignId(userId, campaignId)
                .orElseThrow(() -> new IllegalStateException("Saved campaign not found"));

        savedCampaignRepository.delete(savedCampaign);
    }

    @Override
    public List<SavedCampaign> getSavedCampaigns(String userId) {
        return savedCampaignRepository.findByUserIdOrderBySavedAtDesc(userId);
    }

    @Override
    public boolean isCampaignSaved(String userId, String campaignId) {
        return savedCampaignRepository.existsByUserIdAndCampaignId(userId, campaignId);
    }
}
