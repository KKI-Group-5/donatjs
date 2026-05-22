package id.ac.ui.cs.advprog.donatjs.config;

import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import id.ac.ui.cs.advprog.donatjs.model.Campaign;
import id.ac.ui.cs.advprog.donatjs.model.CampaignStatus;
import id.ac.ui.cs.advprog.donatjs.model.Donation;
import id.ac.ui.cs.advprog.donatjs.model.SavedCampaign;
import id.ac.ui.cs.advprog.donatjs.model.Transaction;
import id.ac.ui.cs.advprog.donatjs.model.TransactionType;
import id.ac.ui.cs.advprog.donatjs.model.Wallet;
import id.ac.ui.cs.advprog.donatjs.repository.CampaignRepository;
import id.ac.ui.cs.advprog.donatjs.repository.DonationRepository;
import id.ac.ui.cs.advprog.donatjs.repository.SavedCampaignRepository;
import id.ac.ui.cs.advprog.donatjs.repository.TransactionRepository;
import id.ac.ui.cs.advprog.donatjs.repository.UserRepository;
import id.ac.ui.cs.advprog.donatjs.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Seeds a fully-usable demo dataset when the application starts under the
 * "local" profile (the default for {@code ./gradlew bootRun}). Every
 * insertion is idempotent so restarting the app never produces duplicates.
 */
@Configuration
@Profile("local")
public class LocalDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(LocalDataInitializer.class);

    private static final String TEST_EMAIL  = "test@donatjs.com";
    private static final String ADMIN_EMAIL = "admin@donatjs.com";

    @Value("${donatjs.local.test-user.password}")
    private String testUserPassword;

    @Value("${donatjs.local.admin-user.password}")
    private String adminUserPassword;

    @Bean
    public CommandLineRunner seedLocalData(UserRepository userRepository,
                                           PasswordEncoder passwordEncoder,
                                           WalletRepository walletRepository,
                                           TransactionRepository transactionRepository,
                                           CampaignRepository campaignRepository,
                                           DonationRepository donationRepository,
                                           SavedCampaignRepository savedCampaignRepository) {
        return args -> {
            AppUser testUser  = ensureUser(userRepository, passwordEncoder,
                    TEST_EMAIL,  testUserPassword,
                    "Test User",  "Local dev account — log in with the button up top.");
            AppUser adminUser = ensureUser(userRepository, passwordEncoder,
                    ADMIN_EMAIL, adminUserPassword,
                    "Admin User", "Administrator demo account.");

            ensureWallet(walletRepository, transactionRepository, testUser.getId().toString(), 1_500_000.0);
            ensureWallet(walletRepository, transactionRepository, adminUser.getId().toString(), 5_000_000.0);

            Campaign schoolCampaign = ensureCampaign(campaignRepository,
                    "Help Build a School in Sulawesi",
                    "Support primary-school construction for 120 children in a remote village. "
                            + "Every rupiah goes to materials and local labour.",
                    LocalDate.now().plusDays(45),
                    new BigDecimal("50000000"),
                    new BigDecimal("12500000"),
                    CampaignStatus.OPEN,
                    adminUser.getId().toString());

            Campaign medicalCampaign = ensureCampaign(campaignRepository,
                    "Emergency Medical Aid Flood Victims",
                    "Rapid-response medical kits, clean water and temporary shelter for families affected "
                            + "by recent flash-flooding in Jakarta.",
                    LocalDate.now().plusDays(14),
                    new BigDecimal("25000000"),
                    new BigDecimal("8750000"),
                    CampaignStatus.OPEN,
                    adminUser.getId().toString());

            Campaign foodCampaign = ensureCampaign(campaignRepository,
                    "Monthly Food Packages for Orphanages",
                    "Recurring donations fund monthly food packages for three Jakarta-area orphanages. "
                            + "Perfect target for a subscription donation.",
                    LocalDate.now().plusDays(120),
                    new BigDecimal("15000000"),
                    new BigDecimal("2100000"),
                    CampaignStatus.OPEN,
                    testUser.getId().toString());

            ensureCampaign(campaignRepository,
                    "Awaiting Review: Reforestation Project",
                    "Proposal to reforest 5 hectares in West Java awaiting admin approval.",
                    LocalDate.now().plusDays(60),
                    new BigDecimal("40000000"),
                    BigDecimal.ZERO,
                    CampaignStatus.WAITING,
                    testUser.getId().toString());

            // Pre-loaded at 97% so a single small donation crosses the 98%
            // notification threshold and fires CampaignNearTargetEvent. The
            // test user has it saved, so the listener has a recipient.
            Campaign almostFundedCampaign = ensureCampaign(campaignRepository,
                    "Almost There: Animal Shelter Renovation",
                    "We're 97% funded — just a final push to renovate the kennel block "
                            + "before the rainy season. A small donation here will trigger "
                            + "the 98% near-target email to everyone who saved this campaign.",
                    LocalDate.now().plusDays(7),
                    new BigDecimal("1000000"),
                    new BigDecimal("970000"),
                    CampaignStatus.OPEN,
                    adminUser.getId().toString());

            ensureDonation(donationRepository, testUser.getId().toString(), schoolCampaign.getId(),
                    250_000L, Donation.PaymentMethod.WALLET);
            ensureDonation(donationRepository, testUser.getId().toString(), medicalCampaign.getId(),
                    100_000L, Donation.PaymentMethod.BANK_BCA);

            ensureSavedCampaign(savedCampaignRepository, testUser.getId().toString(), foodCampaign);
            ensureSavedCampaign(savedCampaignRepository, testUser.getId().toString(), almostFundedCampaign);

            log.info("Local seed complete — login with {} / {} or {} / {}",
                    TEST_EMAIL, testUserPassword, ADMIN_EMAIL, adminUserPassword);
        };
    }

    private AppUser ensureUser(UserRepository repo, PasswordEncoder encoder,
                                String email, String rawPassword,
                                String name, String bio) {
        return repo.findByEmail(email).orElseGet(() -> {
            AppUser user = new AppUser();
            user.setEmail(email);
            user.setPassword(encoder.encode(rawPassword));
            user.setName(name);
            user.setBio(bio);
            user.setDateOfBirth(LocalDate.of(2000, 1, 1));
            user.setAdmin(email.equals(ADMIN_EMAIL));
            return repo.save(user);
        });
    }

    @SuppressWarnings("null")
    private void ensureWallet(WalletRepository walletRepo,
                              TransactionRepository transactionRepo,
                              String userId, double initialBalance) {
        if (walletRepo.findByUserId(userId).isPresent()) {
            return;
        }
        Wallet wallet = walletRepo.save(Wallet.builder()
                .userId(userId)
                .balance(initialBalance)
                .build());
        transactionRepo.save(Transaction.builder()
                .wallet(wallet)
                .amount(initialBalance)
                .type(TransactionType.DEPOSIT)
                .description("Opening balance (seed)")
                .timestamp(LocalDateTime.now().minusDays(3))
                .build());
    }

    private Campaign ensureCampaign(CampaignRepository repo,
                                    String title, String description,
                                    LocalDate deadline,
                                    BigDecimal targetAmount, BigDecimal totalRaised,
                                    CampaignStatus status, String creatorId) {
        Campaign existing = repo.findAll().stream()
                .filter(c -> title.equals(c.getTitle()))
                .findFirst().orElse(null);
        if (existing != null) {
            return existing;
        }
        Campaign campaign = new Campaign();
        campaign.setTitle(title);
        campaign.setDescription(description);
        campaign.setDeadline(deadline);
        campaign.setTargetAmount(targetAmount);
        campaign.setTotalRaised(totalRaised);
        campaign.setStatus(status);
        campaign.setCreatorId(creatorId);
        campaign.setCreatedAt(LocalDateTime.now().minusDays(5));
        return repo.save(campaign);
    }

    @SuppressWarnings("null")
    private void ensureDonation(DonationRepository repo, String userId, Long campaignId,
                                long amount, Donation.PaymentMethod method) {
        boolean exists = repo.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .anyMatch(d -> campaignId.equals(d.getCampaignId()) && amount == d.getAmount());
        if (exists) {
            return;
        }
        repo.save(Donation.builder()
                .userId(userId)
                .campaignId(campaignId)
                .type(Donation.DonationType.ONE_TIME)
                .amount(amount)
                .paymentMethod(method)
                .fee(method == Donation.PaymentMethod.WALLET ? 0L
                        : method.name().startsWith("BANK") ? 1_500L : 2_000L)
                .totalAmount(amount)
                .status(Donation.DonationStatus.SUCCESS)
                .notes("Seed donation")
                .build());
    }

    @SuppressWarnings("null")
    private void ensureSavedCampaign(SavedCampaignRepository repo, String userId, Campaign campaign) {
        String campaignId = String.valueOf(campaign.getId());
        if (repo.existsByUserIdAndCampaignId(userId, campaignId)) {
            return;
        }
        repo.save(SavedCampaign.builder()
                .userId(userId)
                .campaignId(campaignId)
                .campaignTitle(campaign.getTitle())
                .campaignOrganizer("DonatJS Team")
                .campaignImageUrl("https://images.unsplash.com/photo-1488521787991-ed7bbaae773c")
                .savedAt(LocalDateTime.now().minusDays(1))
                .build());
    }
}
