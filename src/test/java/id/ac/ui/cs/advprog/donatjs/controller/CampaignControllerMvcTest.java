package id.ac.ui.cs.advprog.donatjs.controller;

import id.ac.ui.cs.advprog.donatjs.model.Campaign;
import id.ac.ui.cs.advprog.donatjs.model.CampaignStatus;
import id.ac.ui.cs.advprog.donatjs.service.CampaignService;
import id.ac.ui.cs.advprog.donatjs.repository.UserRepository;
import id.ac.ui.cs.advprog.donatjs.service.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(CampaignController.class)
@Import(CampaignControllerMvcTest.PermitAllSecurityConfig.class)
@SuppressWarnings("null")
class CampaignControllerMvcTest {

    @TestConfiguration
    static class PermitAllSecurityConfig {
        @Bean
        @Order(1)
        SecurityFilterChain testChain(HttpSecurity http) throws Exception {
            http.securityMatcher("/**")
                    .authorizeHttpRequests(a -> a.anyRequest().permitAll());
            return http.build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CampaignService campaignService;

    @MockitoBean
    private CurrentUserService currentUserService;

    @MockitoBean
    private UserRepository userRepository;

    // ── GET /campaigns ───────────────────────────────────────────────────────────

    @Test
    void getCampaigns_returnsOnlyOpenCampaignsInModel() throws Exception {
        Campaign open1 = buildCampaign(1L, "Open 1", CampaignStatus.OPEN);
        Campaign open2 = buildCampaign(2L, "Open 2", CampaignStatus.OPEN);
        Mockito.when(campaignService.findOpenCampaigns()).thenReturn(List.of(open1, open2));

        mockMvc.perform(get("/campaigns"))
                .andExpect(status().isOk())
                .andExpect(view().name("campaigns/list"))
                .andExpect(model().attribute("campaigns", notNullValue()))
                .andExpect(model().attribute("campaigns",
                        everyItem(hasProperty("status", is(CampaignStatus.OPEN)))));
    }

    // ── GET /campaigns/create ────────────────────────────────────────────────────

    @Test
    void getCreateForm_returnsCreateViewWithEmptyCampaign() throws Exception {
        mockMvc.perform(get("/campaigns/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("campaigns/create"))
                .andExpect(model().attributeExists("campaign"));
    }

    // ── POST /campaigns/create ───────────────────────────────────────────────────

    @Test
    @WithMockUser
    void postCreate_withBlankRequiredFields_returnsCreateViewWithErrors() throws Exception {
        mockMvc.perform(post("/campaigns/create")
                        .with(csrf())
                        .param("title", "")
                        .param("description", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("campaigns/create"))
                .andExpect(model().attributeHasFieldErrors("campaign", "title", "description"));
    }

    @Test
    @WithMockUser
    void postCreate_usesAuthenticatedUserId() throws Exception {
        Mockito.when(currentUserService.getCurrentUserId(Mockito.any())).thenReturn("user-123");

        mockMvc.perform(post("/campaigns/create")
                        .with(csrf())
                        .header("X-User-Id", "spoofed-user")
                        .param("title", "Build Library")
                        .param("description", "Support our village library")
                        .param("deadline", LocalDate.now().plusDays(7).toString())
                        .param("targetAmount", "100000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/campaigns"));

        Mockito.verify(campaignService).createCampaign(Mockito.any(Campaign.class), Mockito.eq("user-123"));
    }

    @Test
    void postCreate_withValidData_redirectsToCampaignList() throws Exception {
        Campaign created = buildCampaign(10L, "New Campaign", CampaignStatus.WAITING);
        Mockito.when(campaignService.createCampaign(any(Campaign.class), any()))
                .thenReturn(created);

        mockMvc.perform(post("/campaigns/create")
                        .with(csrf())
                        .header("X-User-Id", "user-123")
                        .param("title", "New Campaign")
                        .param("description", "A valid description")
                        .param("deadline", LocalDate.now().plusDays(10).toString())
                        .param("targetAmount", "1000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/campaigns*"));
    }

    @Test
    void postCreate_withPastDeadline_returnsCreateViewWithError() throws Exception {
        mockMvc.perform(post("/campaigns/create")
                        .with(csrf())
                        .param("title", "Campaign")
                        .param("description", "Desc")
                        .param("deadline", LocalDate.now().minusDays(1).toString())
                        .param("targetAmount", "1000"))
                .andExpect(status().isOk())
                .andExpect(view().name("campaigns/create"));
    }

    // ── GET /campaigns/{id} ──────────────────────────────────────────────────────

    @Test
    void getCampaign_found_returnsCampaignDetailView() throws Exception {
        Campaign campaign = buildCampaign(5L, "Detail Campaign", CampaignStatus.OPEN);
        Mockito.when(campaignService.findById(5L)).thenReturn(Optional.of(campaign));

        mockMvc.perform(get("/campaigns/5"))
                .andExpect(status().isOk())
                .andExpect(view().name("campaigns/detail"))
                .andExpect(model().attributeExists("campaign"));
    }

    @Test
    void getCampaign_notFound_returns404() throws Exception {
        Mockito.when(campaignService.findById(999999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/campaigns/999999"))
                .andExpect(status().isNotFound());
    }

    // ── GET /campaigns/{id}/edit ─────────────────────────────────────────────────

    @Test
    void getEditForm_found_returnsEditView() throws Exception {
        Campaign campaign = buildCampaign(3L, "Editable", CampaignStatus.OPEN);
        Mockito.when(campaignService.findById(3L)).thenReturn(Optional.of(campaign));

        mockMvc.perform(get("/campaigns/3/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("campaigns/edit"))
                .andExpect(model().attributeExists("campaign", "req"));
    }

    // ── POST /campaigns/{id}/edit ────────────────────────────────────────────────

    @Test
    void postEdit_withValidDescription_redirectsToDetail() throws Exception {
        Campaign updated = buildCampaign(3L, "Title", CampaignStatus.OPEN);
        updated.setDescription("Updated description");
        Mockito.when(campaignService.updateDescription(eq(3L), any(), eq(false), any()))
                .thenReturn(updated);

        mockMvc.perform(post("/campaigns/3/edit")
                        .with(csrf())
                        .header("X-User-Id", "user-123")
                        .param("description", "Updated description"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/campaigns/3*"));
    }

    @Test
    void postEdit_withBlankDescription_returnsEditView() throws Exception {
        Campaign campaign = buildCampaign(3L, "Title", CampaignStatus.OPEN);
        Mockito.when(campaignService.findById(3L)).thenReturn(Optional.of(campaign));

        mockMvc.perform(post("/campaigns/3/edit")
                        .with(csrf())
                        .param("description", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("campaigns/edit"));
    }

    // ── POST /campaigns/{id}/delete ──────────────────────────────────────────────

    @Test
    void postDelete_withNoDonations_redirectsToCampaignList() throws Exception {
        Mockito.doNothing()
                .when(campaignService).deleteIfNoDonations(anyLong(), any(), eq(false));

        mockMvc.perform(post("/campaigns/4/delete")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/campaigns/4/delete")
                        .with(csrf())
                        .header("X-User-Id", "user-123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/campaigns*"));
    }

    // ── POST /campaigns/{id}/moderate ────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void postModerate_withAdminHeader_returnsOk() throws Exception {
        Campaign campaign = buildCampaign(1L, "Waiting", CampaignStatus.OPEN);
        Mockito.when(campaignService.moderateCampaign(eq(1L), eq(true))).thenReturn(campaign);

        mockMvc.perform(post("/campaigns/1/moderate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approve\":true}"))
                .andExpect(status().isOk());
    }

    @Test
    void postModerate_withoutAdminHeader_returnsForbidden() throws Exception {
        mockMvc.perform(post("/campaigns/1/moderate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approve\":true}"))
                .andExpect(status().isForbidden());
    }

    // ── POST /campaigns/{id}/admin-edit ──────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void postAdminEdit_withAdminHeader_returnsOk() throws Exception {
        Campaign campaign = buildCampaign(2L, "Updated", CampaignStatus.OPEN);
        Mockito.when(campaignService.adminUpdateCampaign(eq(2L), any(), any(), any()))
                .thenReturn(campaign);

        mockMvc.perform(post("/campaigns/2/admin-edit")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void postAdminEdit_withoutAdminHeader_returnsForbidden() throws Exception {
        mockMvc.perform(post("/campaigns/2/admin-edit")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Hack\"}"))
                .andExpect(status().isForbidden());
    }

    // ── POST /campaigns/{id}/donations ────────────────────────────────────────────

    @Test
    void postDonation_validRequest_returnsUpdatedCampaign() throws Exception {
        Campaign campaign = buildCampaign(1L, "Open", CampaignStatus.OPEN);
        campaign.setTotalRaised(new BigDecimal("150"));
        Mockito.when(campaignService.recordSuccessfulDonation(eq(1L), any()))
                .thenReturn(campaign);

        mockMvc.perform(post("/campaigns/1/donations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":50}"))
                .andExpect(status().isOk());
    }

    // ── POST /campaigns/{id}/fraud ────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "USER")
    void postFraud_withoutAdminHeader_returnsForbidden() throws Exception {
        mockMvc.perform(post("/campaigns/1/fraud").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void postFraud_withAdminHeader_returnsOk() throws Exception {
        Campaign campaign = buildCampaign(1L, "Fraud", CampaignStatus.FRAUD);
        Mockito.when(campaignService.markAsFraud(1L)).thenReturn(campaign);

        mockMvc.perform(post("/campaigns/1/fraud")
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    // ── POST /campaigns/deadline-automation/run ───────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void postDeadlineAutomation_withAdminHeader_returnsOk() throws Exception {
        Mockito.when(campaignService.processExpiredCampaigns(any())).thenReturn(3);

        mockMvc.perform(post("/campaigns/deadline-automation/run")
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void postDeadlineAutomation_withoutAdminHeader_returnsForbidden() throws Exception {
        mockMvc.perform(post("/campaigns/deadline-automation/run")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ── helper ───────────────────────────────────────────────────────────────────

    private Campaign buildCampaign(Long id, String title, CampaignStatus status) {
        Campaign c = new Campaign();
        c.setId(id);
        c.setTitle(title);
        c.setDescription("Description for " + title);
        c.setDeadline(LocalDate.now().plusDays(10));
        c.setTargetAmount(new BigDecimal("1000"));
        c.setTotalRaised(BigDecimal.ZERO);
        c.setStatus(status);
        return c;
    }
}
