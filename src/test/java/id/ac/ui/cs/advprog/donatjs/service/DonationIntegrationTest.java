package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.dto.CreateDonationRequest;
import id.ac.ui.cs.advprog.donatjs.dto.DonationResponse;
import id.ac.ui.cs.advprog.donatjs.dto.UserActivityUpdate;
import id.ac.ui.cs.advprog.donatjs.dto.WithdrawalRequestResponse;
import id.ac.ui.cs.advprog.donatjs.model.Campaign;
import id.ac.ui.cs.advprog.donatjs.model.CampaignStatus;
import id.ac.ui.cs.advprog.donatjs.model.Donation;
import id.ac.ui.cs.advprog.donatjs.model.WithdrawalRequestStatus;
import id.ac.ui.cs.advprog.donatjs.repository.CampaignRepository;
import id.ac.ui.cs.advprog.donatjs.repository.DonationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DonationIntegrationTest {

    @Autowired private DonationService donationService;
    @Autowired private WithdrawalRequestService withdrawalRequestService;
    @Autowired private DonationRepository donationRepository;
    @Autowired private CampaignRepository campaignRepository;
    @Autowired private UserActivityService userActivityService;

    @MockitoBean
    private WalletService walletService;

    // ── helpers ───────────────────────────────────────────────────────────────

    private Campaign openCampaign(String title) {
        Campaign c = new Campaign();
        c.setTitle(title);
        c.setDescription("Integration test campaign");
        c.setStatus(CampaignStatus.OPEN);
        c.setTotalRaised(BigDecimal.ZERO);
        c.setTargetAmount(new BigDecimal("10000000"));
        return campaignRepository.save(c);
    }

    private CreateDonationRequest req(String userId, Long campaignId, long amount,
                                      Donation.PaymentMethod method) {
        CreateDonationRequest r = new CreateDonationRequest();
        r.setUserId(userId);
        r.setCampaignId(campaignId);
        r.setAmount(amount);
        r.setPaymentMethod(method);
        r.setType(Donation.DonationType.ONE_TIME);
        return r;
    }

    // ── Campaign integration ──────────────────────────────────────────────────

    @Test
    @DisplayName("SUCCESS donation updates campaign totalRaised")
    void createDonation_success_updatesCampaignTotalRaised() {
        Campaign campaign = openCampaign("Flood Relief");

        donationService.createDonation(req("user-i-01", campaign.getId(), 500_000L,
                Donation.PaymentMethod.GOPAY));

        Campaign updated = campaignRepository.findById(campaign.getId()).orElseThrow();
        assertThat(updated.getTotalRaised()).isEqualByComparingTo(new BigDecimal("500000"));
    }

    @Test
    @DisplayName("REJECTED donation does NOT update campaign totalRaised")
    void createDonation_rejected_doesNotUpdateCampaignTotal() {
        Campaign campaign = openCampaign("School Rebuild");

        donationService.createDonation(req("user-i-02", campaign.getId(), 6_000_000L,
                Donation.PaymentMethod.GOPAY));

        Campaign unchanged = campaignRepository.findById(campaign.getId()).orElseThrow();
        assertThat(unchanged.getTotalRaised()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Donation to non-OPEN campaign throws IllegalStateException")
    void createDonation_nonOpenCampaign_throws() {
        Campaign campaign = openCampaign("Closed Campaign");
        campaign.setStatus(CampaignStatus.CLOSED);
        campaignRepository.save(campaign);

        assertThatThrownBy(() -> donationService.createDonation(
                req("user-i-03", campaign.getId(), 100_000L, Donation.PaymentMethod.GOPAY)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not open");
    }

    // ── Wallet integration ────────────────────────────────────────────────────

    @Test
    @DisplayName("WALLET donation debits the wallet with correct totalAmount")
    void createDonation_walletPayment_debitsWallet() {
        Campaign campaign = openCampaign("Water Well");

        donationService.createDonation(req("user-i-04", campaign.getId(), 200_000L,
                Donation.PaymentMethod.WALLET));

        verify(walletService).deductForDonation("user-i-04", 200_000L, "Water Well");
    }

    @Test
    @DisplayName("WALLET donation with fee includes fee in deducted amount")
    void createDonation_gopayPayment_doesNotDebitWallet() {
        Campaign campaign = openCampaign("Tree Planting");

        donationService.createDonation(req("user-i-05", campaign.getId(), 150_000L,
                Donation.PaymentMethod.GOPAY));

        org.mockito.Mockito.verifyNoInteractions(walletService);
    }

    // ── User Profile / Activity integration ──────────────────────────────────

    @Test
    @DisplayName("REJECTED donation records user activity with REJECTED status")
    void createDonation_rejected_recordsUserActivity() {
        Campaign campaign = openCampaign("Suspicious Campaign");

        donationService.createDonation(req("user-i-06", campaign.getId(), 6_000_000L,
                Donation.PaymentMethod.BANK_BCA));

        List<UserActivityUpdate> activities = userActivityService.getUserActivities("user-i-06");
        assertThat(activities).hasSize(1);
        assertThat(activities.get(0).getStatus()).isEqualTo(UserActivityUpdate.ActivityStatus.REJECTED);
        assertThat(activities.get(0).getActivityType()).isEqualTo(UserActivityUpdate.ActivityType.DONATION);
    }

    // ── Database persistence ──────────────────────────────────────────────────

    @Test
    @DisplayName("SUCCESS donation is persisted to the database with correct fields")
    void createDonation_success_persistedToDatabase() {
        Campaign campaign = openCampaign("Medical Aid");

        DonationResponse response = donationService.createDonation(
                req("user-i-07", campaign.getId(), 150_000L, Donation.PaymentMethod.GOPAY));

        Donation persisted = donationRepository.findById(response.getId()).orElseThrow();
        assertThat(persisted.getUserId()).isEqualTo("user-i-07");
        assertThat(persisted.getStatus()).isEqualTo(Donation.DonationStatus.SUCCESS);
        assertThat(persisted.getAmount()).isEqualTo(150_000L);
        assertThat(persisted.getFee()).isEqualTo(2_000L);
        assertThat(persisted.getTotalAmount()).isEqualTo(152_000L);
    }

    @Test
    @DisplayName("REJECTED donation is still persisted to the database")
    void createDonation_rejected_persistedToDatabase() {
        Campaign campaign = openCampaign("Donation Limit Test");

        DonationResponse response = donationService.createDonation(
                req("user-i-08", campaign.getId(), 7_000_000L, Donation.PaymentMethod.BANK_BCA));

        Donation persisted = donationRepository.findById(response.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(Donation.DonationStatus.REJECTED);
    }

    // ── Withdrawal Request integration ────────────────────────────────────────

    @Test
    @DisplayName("User can request withdrawal on a SUCCESS donation")
    void requestWithdrawal_success_createsPendingRequest() {
        Campaign campaign = openCampaign("Withdrawal Test");
        DonationResponse donation = donationService.createDonation(
                req("user-i-09", campaign.getId(), 100_000L, Donation.PaymentMethod.GOPAY));

        WithdrawalRequestResponse result = withdrawalRequestService
                .requestWithdrawal(donation.getId(), "user-i-09", "Changed my mind");

        assertThat(result.getStatus()).isEqualTo(WithdrawalRequestStatus.PENDING);
        assertThat(result.getDonationId()).isEqualTo(donation.getId());
    }

    @Test
    @DisplayName("Admin approving a withdrawal marks donation as REFUNDED")
    void approveWithdrawal_markesDonationRefunded() {
        Campaign campaign = openCampaign("Refund Test");
        DonationResponse donation = donationService.createDonation(
                req("user-i-10", campaign.getId(), 100_000L, Donation.PaymentMethod.GOPAY));

        WithdrawalRequestResponse request = withdrawalRequestService
                .requestWithdrawal(donation.getId(), "user-i-10", "Need refund");

        withdrawalRequestService.approveWithdrawal(request.getId());

        Donation updated = donationRepository.findById(donation.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(Donation.DonationStatus.REFUNDED);
    }

    @Test
    @DisplayName("Cannot request withdrawal on a REJECTED donation")
    void requestWithdrawal_onRejectedDonation_throws() {
        Campaign campaign = openCampaign("Rejected Withdrawal Test");
        DonationResponse rejected = donationService.createDonation(
                req("user-i-11", campaign.getId(), 6_000_000L, Donation.PaymentMethod.GOPAY));

        assertThatThrownBy(() -> withdrawalRequestService
                .requestWithdrawal(rejected.getId(), "user-i-11", "want money back"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SUCCESS");
    }
}
