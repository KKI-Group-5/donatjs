package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.model.SavedCampaign;

import java.util.List;

public interface SavedCampaignService {

    SavedCampaign saveCampaign(String userId, String campaignId, String campaignTitle,
                               String campaignOrganizer, String campaignImageUrl);

    void removeSavedCampaign(String userId, String campaignId);

    List<SavedCampaign> getSavedCampaigns(String userId);

    boolean isCampaignSaved(String userId, String campaignId);
}
