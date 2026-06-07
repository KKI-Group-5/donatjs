package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import id.ac.ui.cs.advprog.donatjs.model.Campaign;
import id.ac.ui.cs.advprog.donatjs.model.CampaignStatus;
import id.ac.ui.cs.advprog.donatjs.model.Donation;
import id.ac.ui.cs.advprog.donatjs.model.Subscription;
import id.ac.ui.cs.advprog.donatjs.model.Subscription.SubscriptionFrequency;
import id.ac.ui.cs.advprog.donatjs.model.Subscription.SubscriptionStatus;
import id.ac.ui.cs.advprog.donatjs.model.Wallet;
import id.ac.ui.cs.advprog.donatjs.repository.DonationRepository;
import id.ac.ui.cs.advprog.donatjs.repository.SavedCampaignRepository;
import id.ac.ui.cs.advprog.donatjs.repository.SpringDataCampaignRepository;
import id.ac.ui.cs.advprog.donatjs.repository.SubscriptionRepository;
import id.ac.ui.cs.advprog.donatjs.repository.TransactionRepository;
import id.ac.ui.cs.advprog.donatjs.repository.UserRepository;
import id.ac.ui.cs.advprog.donatjs.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
@ActiveProfiles("test")
@SuppressWarnings("null")
class SubscriptionSchedulerIntegrationTest {

    @MockitoBean private EmailService emailService;

    @Autowired private SubscriptionScheduler scheduler;
    @Autowired private DonationRepository donationRepository;
    @Autowired private SavedCampaignRepository savedCampaignRepository;
    @Autowired private SpringDataCampaignRepository campaignRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private WalletRepository walletRepository;

    @BeforeEach
    void setUp() {
        donationRepository.deleteAll();
        subscriptionRepository.deleteAll();
        transactionRepository.deleteAll();
        savedCampaignRepository.deleteAll();
        walletRepository.deleteAll();
        campaignRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void processSubscriptions_insufficientBalanceDoesNotThrowUnexpectedRollback() {
        AppUser donor = saveUser("insufficient-subscription@example.com");
        Campaign campaign = saveOpenCampaign(donor.getId().toString());
        Subscription subscription = saveDueSubscription(donor.getId().toString(), campaign.getId(), 50_000L);

        assertDoesNotThrow(() -> scheduler.processSubscriptions());

        Subscription reloaded = subscriptionRepository.findById(subscription.getId()).orElseThrow();
        assertThat(reloaded.getNextDebitDate()).isEqualTo(LocalDate.now());
        assertThat(donationRepository.findAll()).isEmpty();
        assertThat(transactionRepository.findAll()).isEmpty();
    }

    @Test
    void processSubscriptions_missingCampaignRollsBackDebitAndDoesNotThrow() {
        AppUser donor = saveUser("missing-campaign-subscription@example.com");
        walletRepository.save(Wallet.builder()
                .userId(donor.getId().toString())
                .balance(100_000.0)
                .build());
        Subscription subscription = saveDueSubscription(donor.getId().toString(), 999_999L, 25_000L);

        assertDoesNotThrow(() -> scheduler.processSubscriptions());

        Subscription reloaded = subscriptionRepository.findById(subscription.getId()).orElseThrow();
        assertThat(reloaded.getNextDebitDate()).isEqualTo(LocalDate.now());
        assertThat(walletRepository.findByUserId(donor.getId().toString()).orElseThrow().getBalance())
                .isEqualTo(100_000.0);
        assertThat(donationRepository.findAll()).isEmpty();
        assertThat(transactionRepository.findAll()).isEmpty();
    }

    @Test
    void processSubscriptions_successDebitsWalletRecordsDonationAndAdvancesDate() {
        AppUser donor = saveUser("successful-subscription@example.com");
        Campaign campaign = saveOpenCampaign(donor.getId().toString());
        walletRepository.save(Wallet.builder()
                .userId(donor.getId().toString())
                .balance(100_000.0)
                .build());
        Subscription subscription = saveDueSubscription(donor.getId().toString(), campaign.getId(), 25_000L);

        assertDoesNotThrow(() -> scheduler.processSubscriptions());

        Subscription reloaded = subscriptionRepository.findById(subscription.getId()).orElseThrow();
        assertThat(reloaded.getNextDebitDate()).isEqualTo(LocalDate.now().plusMonths(1));
        assertThat(walletRepository.findByUserId(donor.getId().toString()).orElseThrow().getBalance())
                .isEqualTo(75_000.0);
        assertThat(donationRepository.findAll())
                .singleElement()
                .satisfies(donation -> {
                    assertThat(donation.getUserId()).isEqualTo(donor.getId().toString());
                    assertThat(donation.getCampaignId()).isEqualTo(campaign.getId());
                    assertThat(donation.getAmount()).isEqualTo(25_000L);
                    assertThat(donation.getType()).isEqualTo(Donation.DonationType.SUBSCRIPTION);
                    assertThat(donation.getStatus()).isEqualTo(Donation.DonationStatus.SUCCESS);
                });
        assertThat(transactionRepository.findAll()).hasSize(1);
    }

    private AppUser saveUser(String email) {
        AppUser donor = new AppUser();
        donor.setEmail(email);
        donor.setPassword("ignored");
        donor.setName("Subscription Tester");
        donor.setDateOfBirth(LocalDate.of(2000, 1, 1));
        return userRepository.save(donor);
    }

    private Campaign saveOpenCampaign(String creatorId) {
        Campaign campaign = new Campaign();
        campaign.setTitle("Subscription Integration Campaign");
        campaign.setDescription("Campaign used by subscription scheduler integration tests.");
        campaign.setDeadline(LocalDate.now().plusDays(30));
        campaign.setTargetAmount(new BigDecimal("1000000"));
        campaign.setTotalRaised(BigDecimal.ZERO);
        campaign.setStatus(CampaignStatus.OPEN);
        campaign.setCreatorId(creatorId);
        return campaignRepository.save(campaign);
    }

    private Subscription saveDueSubscription(String userId, Long campaignId, Long amount) {
        return subscriptionRepository.save(Subscription.builder()
                .userId(userId)
                .campaignId(campaignId)
                .amount(amount)
                .frequency(SubscriptionFrequency.MONTHLY)
                .status(SubscriptionStatus.ACTIVE)
                .nextDebitDate(LocalDate.now())
                .build());
    }
}
