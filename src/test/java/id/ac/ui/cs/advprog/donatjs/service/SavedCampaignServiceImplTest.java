package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.model.SavedCampaign;
import id.ac.ui.cs.advprog.donatjs.repository.SavedCampaignRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SavedCampaignServiceImplTest {

    @Mock
    private SavedCampaignRepository savedCampaignRepository;

    @InjectMocks
    private SavedCampaignServiceImpl savedCampaignService;

    private static final String USER_ID = "user-001";
    private static final String CAMPAIGN_ID = "camp-001";
    private static final String CAMPAIGN_TITLE = "Help Build a School";
    private static final String CAMPAIGN_ORGANIZER = "Sarah Johnson";
    private static final String CAMPAIGN_IMAGE_URL = "https://example.com/image.jpg";

    private SavedCampaign sampleSavedCampaign;

    @BeforeEach
    void setUp() {
        sampleSavedCampaign = SavedCampaign.builder()
                .id(1L)
                .userId(USER_ID)
                .campaignId(CAMPAIGN_ID)
                .campaignTitle(CAMPAIGN_TITLE)
                .campaignOrganizer(CAMPAIGN_ORGANIZER)
                .campaignImageUrl(CAMPAIGN_IMAGE_URL)
                .savedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void saveCampaign_success() {
        when(savedCampaignRepository.existsByUserIdAndCampaignId(USER_ID, CAMPAIGN_ID))
                .thenReturn(false);
        when(savedCampaignRepository.save(any(SavedCampaign.class)))
                .thenReturn(sampleSavedCampaign);

        SavedCampaign result = savedCampaignService.saveCampaign(
                USER_ID, CAMPAIGN_ID, CAMPAIGN_TITLE, CAMPAIGN_ORGANIZER, CAMPAIGN_IMAGE_URL);

        assertNotNull(result);
        assertEquals(USER_ID, result.getUserId());
        assertEquals(CAMPAIGN_ID, result.getCampaignId());
        assertEquals(CAMPAIGN_TITLE, result.getCampaignTitle());
        verify(savedCampaignRepository).save(any(SavedCampaign.class));
    }

    @Test
    void saveCampaign_alreadySaved_throwsException() {
        when(savedCampaignRepository.existsByUserIdAndCampaignId(USER_ID, CAMPAIGN_ID))
                .thenReturn(true);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                savedCampaignService.saveCampaign(
                        USER_ID, CAMPAIGN_ID, CAMPAIGN_TITLE, CAMPAIGN_ORGANIZER, CAMPAIGN_IMAGE_URL));

        assertEquals("Campaign is already saved", exception.getMessage());
        verify(savedCampaignRepository, never()).save(any());
    }

    @Test
    void removeSavedCampaign_success() {
        when(savedCampaignRepository.findByUserIdAndCampaignId(USER_ID, CAMPAIGN_ID))
                .thenReturn(Optional.of(sampleSavedCampaign));

        assertDoesNotThrow(() -> savedCampaignService.removeSavedCampaign(USER_ID, CAMPAIGN_ID));
        verify(savedCampaignRepository).delete(sampleSavedCampaign);
    }

    @Test
    void removeSavedCampaign_notFound_throwsException() {
        when(savedCampaignRepository.findByUserIdAndCampaignId(USER_ID, CAMPAIGN_ID))
                .thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                savedCampaignService.removeSavedCampaign(USER_ID, CAMPAIGN_ID));

        assertEquals("Saved campaign not found", exception.getMessage());
        verify(savedCampaignRepository, never()).delete(any());
    }

    @Test
    void getSavedCampaigns_returnsListForUser() {
        SavedCampaign campaign2 = SavedCampaign.builder()
                .id(2L)
                .userId(USER_ID)
                .campaignId("camp-002")
                .campaignTitle("Another Campaign")
                .campaignOrganizer("Another Org")
                .savedAt(LocalDateTime.now())
                .build();

        when(savedCampaignRepository.findByUserIdOrderBySavedAtDesc(USER_ID))
                .thenReturn(Arrays.asList(sampleSavedCampaign, campaign2));

        List<SavedCampaign> result = savedCampaignService.getSavedCampaigns(USER_ID);

        assertEquals(2, result.size());
        verify(savedCampaignRepository).findByUserIdOrderBySavedAtDesc(USER_ID);
    }

    @Test
    void getSavedCampaigns_emptyList() {
        when(savedCampaignRepository.findByUserIdOrderBySavedAtDesc(USER_ID))
                .thenReturn(List.of());

        List<SavedCampaign> result = savedCampaignService.getSavedCampaigns(USER_ID);

        assertTrue(result.isEmpty());
    }

    @Test
    void isCampaignSaved_returnsTrue() {
        when(savedCampaignRepository.existsByUserIdAndCampaignId(USER_ID, CAMPAIGN_ID))
                .thenReturn(true);

        assertTrue(savedCampaignService.isCampaignSaved(USER_ID, CAMPAIGN_ID));
    }

    @Test
    void isCampaignSaved_returnsFalse() {
        when(savedCampaignRepository.existsByUserIdAndCampaignId(USER_ID, CAMPAIGN_ID))
                .thenReturn(false);

        assertFalse(savedCampaignService.isCampaignSaved(USER_ID, CAMPAIGN_ID));
    }
}
