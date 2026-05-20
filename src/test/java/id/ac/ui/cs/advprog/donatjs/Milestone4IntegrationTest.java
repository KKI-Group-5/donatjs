package id.ac.ui.cs.advprog.donatjs;

import id.ac.ui.cs.advprog.donatjs.dto.CreateDonationRequest;
import id.ac.ui.cs.advprog.donatjs.dto.CreateSubscriptionRequest;
import id.ac.ui.cs.advprog.donatjs.dto.SubscriptionResponse;
import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import id.ac.ui.cs.advprog.donatjs.model.Campaign;
import id.ac.ui.cs.advprog.donatjs.model.CampaignStatus;
import id.ac.ui.cs.advprog.donatjs.model.Donation.DonationType;
import id.ac.ui.cs.advprog.donatjs.model.Donation.PaymentMethod;
import id.ac.ui.cs.advprog.donatjs.model.SavedCampaign;
import id.ac.ui.cs.advprog.donatjs.model.Subscription.SubscriptionFrequency;
import id.ac.ui.cs.advprog.donatjs.model.Subscription.SubscriptionStatus;
import id.ac.ui.cs.advprog.donatjs.model.Wallet;
import id.ac.ui.cs.advprog.donatjs.repository.CampaignRepository;
import id.ac.ui.cs.advprog.donatjs.repository.DonationRepository;
import id.ac.ui.cs.advprog.donatjs.repository.SavedCampaignRepository;
import id.ac.ui.cs.advprog.donatjs.repository.SubscriptionRepository;
import id.ac.ui.cs.advprog.donatjs.repository.TransactionRepository;
import id.ac.ui.cs.advprog.donatjs.repository.UserRepository;
import id.ac.ui.cs.advprog.donatjs.repository.WalletRepository;
import id.ac.ui.cs.advprog.donatjs.service.CampaignService;
import id.ac.ui.cs.advprog.donatjs.service.DonationService;
import id.ac.ui.cs.advprog.donatjs.service.EmailService;
import id.ac.ui.cs.advprog.donatjs.service.SubscriptionService;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * End-to-end coverage for Milestone 4: donation crossing 98% triggers a single
 * email via the real DonationService → CampaignService → AFTER_COMMIT @Async
 * listener chain, and campaign rejection terminates active subscriptions.
 * Async listener completion is awaited with Awaitility.
 */
@SpringBootTest
@ActiveProfiles("test")
@SuppressWarnings("null")
class Milestone4IntegrationTest {

    @MockitoBean private EmailService emailService;

    @Autowired private DonationService donationService;
    @Autowired private CampaignService campaignService;
    @Autowired private SubscriptionService subscriptionService;
    @Autowired private CampaignRepository campaignRepository;
    @Autowired private SavedCampaignRepository savedCampaignRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private DonationRepository donationRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private WalletRepository walletRepository;
    @Autowired private UserRepository userRepository;

    private AppUser donor;

    @BeforeEach
    void setUp() {
        // Clean state in FK-dependency order (children before parents).
        donationRepository.deleteAll();
        subscriptionRepository.deleteAll();
        transactionRepository.deleteAll();
        savedCampaignRepository.deleteAll();
        walletRepository.deleteAll();
        userRepository.deleteAll();

        donor = new AppUser();
        donor.setEmail("donor@example.com");
        donor.setPassword("ignored");
        donor.setName("Test Donor");
        donor.setBio("");
        donor.setDateOfBirth(LocalDate.of(2000, 1, 1));
        donor = userRepository.save(donor);

        walletRepository.save(Wallet.builder()
                .userId(donor.getId().toString())
                .balance(500_000.0)
                .build());
    }

    @Test
    void donationCrossing98_triggersExactlyOneEmail() {
        Campaign campaign = seedNearTargetCampaign("Crossing 98%");
        seedSavedCampaign(campaign);

        CreateDonationRequest req = walletDonation(campaign.getId(), 20_000L); // 970k + 20k = 990k = 99%
        donationService.createDonation(req);

        Campaign reloaded = campaignRepository.findById(campaign.getId()).orElseThrow();
        assertThat(reloaded.getTotalRaised()).isEqualByComparingTo(new BigDecimal("990000"));
        assertThat(reloaded.isNearTargetNotified())
                .as("threshold detection should set nearTargetNotified")
                .isTrue();

        // Listener is @Async — wait briefly for it to land before asserting.
        Awaitility.await().atMost(Duration.ofSeconds(3)).untilAsserted(() ->
                verify(emailService, times(1)).sendPlainText(
                        eq("donor@example.com"),
                        contains("Crossing 98%"),
                        anyString()));
    }

    @Test
    void secondDonationDoesNotReTriggerEmail_idempotency() {
        Campaign campaign = seedNearTargetCampaign("Idempotent");
        seedSavedCampaign(campaign);

        donationService.createDonation(walletDonation(campaign.getId(), 10_000L)); // 980k = 98%, fires
        donationService.createDonation(walletDonation(campaign.getId(),  5_000L)); // 985k = 98.5%, no fire

        Awaitility.await().atMost(Duration.ofSeconds(3)).untilAsserted(() ->
                verify(emailService, times(1)).sendPlainText(anyString(), anyString(), anyString()));
    }

    @Test
    void donationStayingBelowThreshold_doesNotTrigger() {
        Campaign campaign = openCampaign("Still 95%", new BigDecimal("1000000"), new BigDecimal("950000"));
        seedSavedCampaign(campaign);

        donationService.createDonation(walletDonation(campaign.getId(), 10_000L)); // 960k = 96%

        verify(emailService, times(0)).sendPlainText(anyString(), anyString(), anyString());
    }

    @Test
    void campaignRejection_terminatesActiveSubscription() {
        Campaign campaign = openCampaign("To reject",
                new BigDecimal("100000"), BigDecimal.ZERO);

        SubscriptionResponse sub = subscriptionService.createSubscription(
                CreateSubscriptionRequest.builder()
                        .userId(donor.getId().toString())
                        .campaignId(campaign.getId())
                        .amount(5_000L)
                        .frequency(SubscriptionFrequency.MONTHLY)
                        .build());
        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);

        // OPEN → REJECTED via admin moderation publishes CampaignStatusChangedEvent
        // which the listener routes through SubscriptionService.terminateActiveSubscriptionsForCampaign
        campaignService.moderateCampaign(campaign.getId(), false);

        SubscriptionResponse reloaded = subscriptionService
                .getSubscriptionsByUser(donor.getId().toString())
                .stream()
                .filter(s -> s.getId().equals(sub.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(SubscriptionStatus.TERMINATED);
    }

    @Test
    void campaignClosed_doesNotTerminateSubscription_healthyEndOfLife() {
        Campaign campaign = openCampaign("Closed-on-target",
                new BigDecimal("1000"), new BigDecimal("990"));

        SubscriptionResponse sub = subscriptionService.createSubscription(
                CreateSubscriptionRequest.builder()
                        .userId(donor.getId().toString())
                        .campaignId(campaign.getId())
                        .amount(5_000L)
                        .frequency(SubscriptionFrequency.MONTHLY)
                        .build());

        // The subscription itself adds 5,000 → already 5,990 ≥ 1,000 → CLOSED.
        // Status change CLOSED is *not* a terminating one.
        SubscriptionResponse reloaded = subscriptionService
                .getSubscriptionsByUser(donor.getId().toString())
                .stream()
                .filter(s -> s.getId().equals(sub.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private Campaign seedNearTargetCampaign(String title) {
        return openCampaign(title, new BigDecimal("1000000"), new BigDecimal("970000"));
    }

    private Campaign openCampaign(String title, BigDecimal target, BigDecimal raised) {
        Campaign c = new Campaign();
        c.setTitle(title);
        c.setDescription("integration-test campaign");
        c.setDeadline(LocalDate.now().plusDays(30));
        c.setTargetAmount(target);
        c.setTotalRaised(raised);
        c.setStatus(CampaignStatus.OPEN);
        c.setCreatorId(donor.getId().toString());
        c.setCreatedAt(LocalDateTime.now());
        return campaignRepository.save(c);
    }

    private void seedSavedCampaign(Campaign campaign) {
        savedCampaignRepository.save(SavedCampaign.builder()
                .userId(donor.getId().toString())
                .campaignId(String.valueOf(campaign.getId()))
                .campaignTitle(campaign.getTitle())
                .campaignOrganizer("Test")
                .campaignImageUrl(null)
                .build());
    }

    private CreateDonationRequest walletDonation(Long campaignId, long amount) {
        CreateDonationRequest req = new CreateDonationRequest();
        req.setUserId(donor.getId().toString());
        req.setCampaignId(campaignId);
        req.setAmount(amount);
        req.setPaymentMethod(PaymentMethod.WALLET);
        req.setType(DonationType.ONE_TIME);
        req.setNotes("integration-test");
        return req;
    }
}
