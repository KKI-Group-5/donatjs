package id.ac.ui.cs.advprog.donatjs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.donatjs.dto.AdminUpdateCampaignRequest;
import id.ac.ui.cs.advprog.donatjs.dto.CampaignModerationRequest;
import id.ac.ui.cs.advprog.donatjs.dto.DonationUpdateRequest;
import id.ac.ui.cs.advprog.donatjs.model.Campaign;
import id.ac.ui.cs.advprog.donatjs.service.CampaignService;
import id.ac.ui.cs.advprog.donatjs.service.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
public class CampaignControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CampaignService campaignService;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private CampaignController campaignController;

    private ObjectMapper objectMapper = new ObjectMapper();

    private Campaign campaign;

    private static RequestPostProcessor asAdmin() {
        return request -> {
            request.setUserPrincipal(new UsernamePasswordAuthenticationToken(
                    "admin", null,
                    List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
            return request;
        };
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(campaignController).build();
        campaign = new Campaign();
        campaign.setId(1L);
        campaign.setTitle("Test Campaign");
        campaign.setDescription("Test Description");
        campaign.setDeadline(LocalDate.now().plusDays(10));
    }

    @Test
    void listCampaigns() throws Exception {
        when(campaignService.findOpenCampaigns()).thenReturn(Arrays.asList(campaign));

        mockMvc.perform(get("/campaigns"))
                .andExpect(status().isOk())
                .andExpect(view().name("campaigns/list"))
                .andExpect(model().attributeExists("campaigns"));
    }

    @Test
    void showCreateForm() throws Exception {
        mockMvc.perform(get("/campaigns/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("campaigns/create"))
                .andExpect(model().attributeExists("campaign"));
    }

    @Test
    void createCampaignValid() throws Exception {
        mockMvc.perform(post("/campaigns/create")
                        .with(csrf())
                        .header("X-User-Id", "user123")
                        .param("title", "Title")
                        .param("description", "Desc")
                        .param("targetAmount", "1000")
                        .param("deadline", LocalDate.now().plusDays(5).toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/campaigns"));

        verify(campaignService, times(1)).createCampaign(any(Campaign.class), eq("user123"));
    }

    @Test
    void createCampaignInvalidDeadline() throws Exception {
        mockMvc.perform(post("/campaigns/create")
                        .with(csrf())
                        .param("title", "Title")
                        .param("description", "Desc")
                        .param("targetAmount", "1000")
                        .param("deadline", LocalDate.now().minusDays(5).toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("campaigns/create"))
                .andExpect(model().hasErrors());

        verify(campaignService, never()).createCampaign(any(), any());
    }

    @Test
    void campaignDetailFound() throws Exception {
        when(campaignService.findById(1L)).thenReturn(Optional.of(campaign));

        mockMvc.perform(get("/campaigns/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("campaigns/detail"))
                .andExpect(model().attributeExists("campaign"));
    }

    @Test
    void campaignDetailNotFound() throws Exception {
        when(campaignService.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/campaigns/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void showEditDescriptionFormFound() throws Exception {
        when(campaignService.findById(1L)).thenReturn(Optional.of(campaign));

        mockMvc.perform(get("/campaigns/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("campaigns/edit"))
                .andExpect(model().attributeExists("campaign", "req"));
    }

    @Test
    void updateDescriptionValid() throws Exception {
        when(campaignService.updateDescription(eq(1L), anyString(), anyBoolean(), anyString())).thenReturn(campaign);

        mockMvc.perform(post("/campaigns/1/edit")
                        .with(csrf())
                        .header("X-User-Id", "user123")
                        .param("description", "New Desc"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/campaigns/1"));
    }

    @Test
    void deleteCampaignSuccess() throws Exception {
        mockMvc.perform(post("/campaigns/1/delete")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/campaigns"));
    }

    @Test
    void deleteCampaignWithDonations() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete")).when(campaignService).deleteIfNoDonations(anyLong(), any(), anyBoolean());

        mockMvc.perform(post("/campaigns/1/delete")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/campaigns/1"));
    }

    @Test
    void moderateCampaignAsAdmin() throws Exception {
        CampaignModerationRequest req = new CampaignModerationRequest();
        req.setApprove(true);
        when(campaignService.moderateCampaign(1L, true)).thenReturn(campaign);

        mockMvc.perform(post("/campaigns/1/moderate")
                        .with(asAdmin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void moderateCampaignForbidden() throws Exception {
        CampaignModerationRequest req = new CampaignModerationRequest();
        req.setApprove(true);

        mockMvc.perform(post("/campaigns/1/moderate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminEditCampaignAsAdmin() throws Exception {
        AdminUpdateCampaignRequest req = new AdminUpdateCampaignRequest();
        req.setTitle("New Title");
        when(campaignService.adminUpdateCampaign(eq(1L), any(), any(), any())).thenReturn(campaign);

        mockMvc.perform(post("/campaigns/1/admin-edit")
                        .with(asAdmin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void recordDonation() throws Exception {
        DonationUpdateRequest req = new DonationUpdateRequest();
        req.setAmount(java.math.BigDecimal.valueOf(100L));
        when(campaignService.recordSuccessfulDonation(1L, java.math.BigDecimal.valueOf(100L))).thenReturn(campaign);

        mockMvc.perform(post("/campaigns/1/donations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void markFraudAsAdmin() throws Exception {
        when(campaignService.markAsFraud(1L)).thenReturn(campaign);

        mockMvc.perform(post("/campaigns/1/fraud")
                        .with(asAdmin())
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void runDeadlineAutomationAsAdmin() throws Exception {
        when(campaignService.processExpiredCampaigns(any())).thenReturn(5);

        mockMvc.perform(post("/campaigns/deadline-automation/run")
                        .with(asAdmin())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Processed expired campaigns: 5"));
    }
}
