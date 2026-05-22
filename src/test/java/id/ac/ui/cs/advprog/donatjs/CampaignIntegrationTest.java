package id.ac.ui.cs.advprog.donatjs;

import id.ac.ui.cs.advprog.donatjs.model.Campaign;
import id.ac.ui.cs.advprog.donatjs.model.CampaignStatus;
import id.ac.ui.cs.advprog.donatjs.repository.InMemoryCampaignRepository;
import id.ac.ui.cs.advprog.donatjs.service.CampaignService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * Integration tests that boot the full Spring context and exercise the
 * campaign management flow end-to-end through the HTTP layer.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class CampaignIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private InMemoryCampaignRepository campaignRepository;

    @BeforeEach
    void setUp() {
        // Nothing to clean up; @DirtiesContext resets the Spring context (and thus
        // the in-memory repository) before each test method.
    }

    // ── Campaign list page ────────────────────────────────────────────────────────

    @Test
    void getCampaignList_returnsListView() throws Exception {
        mockMvc.perform(get("/campaigns"))
                .andExpect(status().isOk())
                .andExpect(view().name("campaigns/list"));
    }

    // ── Campaign creation form ────────────────────────────────────────────────────

    @Test
    void getCreateForm_returnsCreateView() throws Exception {
        mockMvc.perform(get("/campaigns/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("campaigns/create"));
    }

    // ── Campaign creation → detail ────────────────────────────────────────────────

    @Test
    void postCreate_validData_createsWaitingCampaign_andRedirects() throws Exception {
        mockMvc.perform(post("/campaigns/create")
                        .header("X-User-Id", "user-1")
                        .param("title", "Integration Test Campaign")
                        .param("description", "Created by integration test")
                        .param("deadline", LocalDate.now().plusDays(30).toString())
                        .param("targetAmount", "5000"))
                .andExpect(status().is3xxRedirection());

        assertThat(campaignRepository.findAll())
                .anyMatch(c -> "Integration Test Campaign".equals(c.getTitle())
                        && c.getStatus() == CampaignStatus.WAITING);
    }

    // ── Approve flow: WAITING → OPEN ─────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void moderate_approve_makesWaitingCampaignOpen() throws Exception {
        Campaign campaign = createWaitingCampaign("Campaign to approve");
        Long id = campaign.getId();

        mockMvc.perform(post("/campaigns/" + id + "/moderate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approve\":true}"))
                .andExpect(status().isOk());

        assertThat(campaignRepository.findById(id).orElseThrow().getStatus())
                .isEqualTo(CampaignStatus.OPEN);
    }

    // ── Reject flow: WAITING → REJECTED ──────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void moderate_reject_makesWaitingCampaignRejected() throws Exception {
        Campaign campaign = createWaitingCampaign("Campaign to reject");
        Long id = campaign.getId();

        mockMvc.perform(post("/campaigns/" + id + "/moderate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approve\":false}"))
                .andExpect(status().isOk());

        assertThat(campaignRepository.findById(id).orElseThrow().getStatus())
                .isEqualTo(CampaignStatus.REJECTED);
    }

    // ── Donation recording ────────────────────────────────────────────────────────

    @Test
    void donation_updatesTotalRaised() throws Exception {
        Campaign campaign = createOpenCampaign("Donation test campaign", new BigDecimal("1000"));
        Long id = campaign.getId();

        mockMvc.perform(post("/campaigns/" + id + "/donations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":200}"))
                .andExpect(status().isOk());

        assertThat(campaignRepository.findById(id).orElseThrow().getTotalRaised())
                .isEqualByComparingTo(new BigDecimal("200"));
    }

    @Test
    void donation_autoClosesCampaign_whenTargetReached() throws Exception {
        Campaign campaign = createOpenCampaign("Near-target campaign", new BigDecimal("500"));
        campaign.setTotalRaised(new BigDecimal("490"));
        campaignRepository.save(campaign);
        Long id = campaign.getId();

        mockMvc.perform(post("/campaigns/" + id + "/donations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":20}"))
                .andExpect(status().isOk());

        assertThat(campaignRepository.findById(id).orElseThrow().getStatus())
                .isEqualTo(CampaignStatus.CLOSED);
    }

    // ── Deadline automation ───────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void deadlineAutomation_cancelsExpiredOpenCampaign() throws Exception {
        Campaign campaign = new Campaign();
        campaign.setTitle("Expired campaign");
        campaign.setDescription("Should be cancelled");
        campaign.setStatus(CampaignStatus.OPEN);
        campaign.setDeadline(LocalDate.now().minusDays(1));
        campaign.setTargetAmount(new BigDecimal("1000"));
        campaign.setTotalRaised(BigDecimal.ZERO);
        Campaign saved = campaignRepository.save(campaign);

        mockMvc.perform(post("/campaigns/deadline-automation/run"))
                .andExpect(status().isOk());

        assertThat(campaignRepository.findById(saved.getId()).orElseThrow().getStatus())
                .isEqualTo(CampaignStatus.CANCELLED);
    }

    @Test
    void deadlineAutomation_withoutAdminHeader_returnsForbidden() throws Exception {
        mockMvc.perform(post("/campaigns/deadline-automation/run"))
                .andExpect(status().isForbidden());
    }

    // ── Fraud marking ─────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void markFraud_makesOpenCampaignFraud() throws Exception {
        Campaign campaign = createOpenCampaign("Fraudulent campaign", new BigDecimal("1000"));
        campaign.setTotalRaised(new BigDecimal("100"));
        campaignRepository.save(campaign);
        Long id = campaign.getId();

        mockMvc.perform(post("/campaigns/" + id + "/fraud"))
                .andExpect(status().isOk());

        assertThat(campaignRepository.findById(id).orElseThrow().getStatus())
                .isEqualTo(CampaignStatus.FRAUD);
    }

    // ── Delete campaign ───────────────────────────────────────────────────────────

    @Test
    void deleteCampaign_withNoDonations_marksDeleted() throws Exception {
        Campaign campaign = createWaitingCampaign("Deletable campaign");
        campaign.setCreatorId("user-99");
        campaign.setTotalRaised(BigDecimal.ZERO);
        campaignRepository.save(campaign);
        Long id = campaign.getId();

        mockMvc.perform(post("/campaigns/" + id + "/delete")
                        .header("X-User-Id", "user-99"))
                .andExpect(status().is3xxRedirection());

        assertThat(campaignRepository.findById(id).orElseThrow().getStatus())
                .isEqualTo(CampaignStatus.DELETED);
    }

    // ── helpers ───────────────────────────────────────────────────────────────────

    private Campaign createWaitingCampaign(String title) {
        Campaign c = new Campaign();
        c.setTitle(title);
        c.setDescription("Description for " + title);
        c.setStatus(CampaignStatus.WAITING);
        c.setDeadline(LocalDate.now().plusDays(30));
        c.setTargetAmount(new BigDecimal("1000"));
        c.setTotalRaised(BigDecimal.ZERO);
        return campaignRepository.save(c);
    }

    private Campaign createOpenCampaign(String title, BigDecimal targetAmount) {
        Campaign c = new Campaign();
        c.setTitle(title);
        c.setDescription("Description for " + title);
        c.setStatus(CampaignStatus.OPEN);
        c.setDeadline(LocalDate.now().plusDays(30));
        c.setTargetAmount(targetAmount);
        c.setTotalRaised(BigDecimal.ZERO);
        return campaignRepository.save(c);
    }
}
