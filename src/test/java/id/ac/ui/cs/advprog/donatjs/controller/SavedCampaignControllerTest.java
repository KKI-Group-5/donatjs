package id.ac.ui.cs.advprog.donatjs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.donatjs.dto.SaveCampaignRequest;
import id.ac.ui.cs.advprog.donatjs.model.SavedCampaign;
import id.ac.ui.cs.advprog.donatjs.service.SavedCampaignService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SavedCampaignController.class)
@WithMockUser
class SavedCampaignControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SavedCampaignService savedCampaignService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String USER_ID = "user-001";
    private static final String CAMPAIGN_ID = "camp-001";

    @Test
    void saveCampaign_returnsCreated() throws Exception {
        SaveCampaignRequest request = SaveCampaignRequest.builder()
                .userId(USER_ID)
                .campaignId(CAMPAIGN_ID)
                .campaignTitle("Test Campaign")
                .campaignOrganizer("Test Organizer")
                .campaignImageUrl("https://example.com/img.jpg")
                .build();

        SavedCampaign saved = SavedCampaign.builder()
                .id(1L)
                .userId(USER_ID)
                .campaignId(CAMPAIGN_ID)
                .campaignTitle("Test Campaign")
                .campaignOrganizer("Test Organizer")
                .campaignImageUrl("https://example.com/img.jpg")
                .savedAt(LocalDateTime.now())
                .build();

        when(savedCampaignService.saveCampaign(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(saved);

        mockMvc.perform(post("/api/saved-campaigns")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.campaignId").value(CAMPAIGN_ID))
                .andExpect(jsonPath("$.campaignTitle").value("Test Campaign"));
    }

    @Test
    void saveCampaign_alreadySaved_returnsConflict() throws Exception {
        SaveCampaignRequest request = SaveCampaignRequest.builder()
                .userId(USER_ID)
                .campaignId(CAMPAIGN_ID)
                .campaignTitle("Test Campaign")
                .campaignOrganizer("Test Organizer")
                .campaignImageUrl("https://example.com/img.jpg")
                .build();

        when(savedCampaignService.saveCampaign(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("Campaign is already saved"));

        mockMvc.perform(post("/api/saved-campaigns")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Campaign is already saved"));
    }

    @Test
    void removeSavedCampaign_returnsOk() throws Exception {
        doNothing().when(savedCampaignService).removeSavedCampaign(USER_ID, CAMPAIGN_ID);

        mockMvc.perform(delete("/api/saved-campaigns/{userId}/{campaignId}", USER_ID, CAMPAIGN_ID)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Campaign removed from saved list"));
    }

    @Test
    void removeSavedCampaign_notFound_returns404() throws Exception {
        doThrow(new IllegalStateException("Saved campaign not found"))
                .when(savedCampaignService).removeSavedCampaign(USER_ID, CAMPAIGN_ID);

        mockMvc.perform(delete("/api/saved-campaigns/{userId}/{campaignId}", USER_ID, CAMPAIGN_ID)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Saved campaign not found"));
    }

    @Test
    void getSavedCampaigns_returnsList() throws Exception {
        List<SavedCampaign> campaigns = Arrays.asList(
                SavedCampaign.builder()
                        .id(1L).userId(USER_ID).campaignId("camp-001")
                        .campaignTitle("Campaign 1").campaignOrganizer("Org 1")
                        .savedAt(LocalDateTime.now()).build(),
                SavedCampaign.builder()
                        .id(2L).userId(USER_ID).campaignId("camp-002")
                        .campaignTitle("Campaign 2").campaignOrganizer("Org 2")
                        .savedAt(LocalDateTime.now()).build()
        );

        when(savedCampaignService.getSavedCampaigns(USER_ID)).thenReturn(campaigns);

        mockMvc.perform(get("/api/saved-campaigns/{userId}", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].campaignTitle").value("Campaign 1"))
                .andExpect(jsonPath("$[1].campaignTitle").value("Campaign 2"));
    }

    @Test
    void isCampaignSaved_returnsTrue() throws Exception {
        when(savedCampaignService.isCampaignSaved(USER_ID, CAMPAIGN_ID)).thenReturn(true);

        mockMvc.perform(get("/api/saved-campaigns/{userId}/check/{campaignId}", USER_ID, CAMPAIGN_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(true));
    }

    @Test
    void isCampaignSaved_returnsFalse() throws Exception {
        when(savedCampaignService.isCampaignSaved(USER_ID, CAMPAIGN_ID)).thenReturn(false);

        mockMvc.perform(get("/api/saved-campaigns/{userId}/check/{campaignId}", USER_ID, CAMPAIGN_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(false));
    }
}
