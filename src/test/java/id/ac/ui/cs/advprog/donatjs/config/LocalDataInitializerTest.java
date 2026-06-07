package id.ac.ui.cs.advprog.donatjs.config;

import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import id.ac.ui.cs.advprog.donatjs.model.Campaign;
import id.ac.ui.cs.advprog.donatjs.model.CampaignStatus;
import id.ac.ui.cs.advprog.donatjs.model.Donation;
import id.ac.ui.cs.advprog.donatjs.model.SavedCampaign;
import id.ac.ui.cs.advprog.donatjs.model.Transaction;
import id.ac.ui.cs.advprog.donatjs.model.Wallet;
import id.ac.ui.cs.advprog.donatjs.repository.CampaignRepository;
import id.ac.ui.cs.advprog.donatjs.repository.DonationRepository;
import id.ac.ui.cs.advprog.donatjs.repository.SavedCampaignRepository;
import id.ac.ui.cs.advprog.donatjs.repository.TransactionRepository;
import id.ac.ui.cs.advprog.donatjs.repository.UserRepository;
import id.ac.ui.cs.advprog.donatjs.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalDataInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private DonationRepository donationRepository;

    @Mock
    private SavedCampaignRepository savedCampaignRepository;

    private LocalDataInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new LocalDataInitializer();
        ReflectionTestUtils.setField(initializer, "testUserPassword", "test-password");
        ReflectionTestUtils.setField(initializer, "adminUserPassword", "admin-password");
    }

    @Test
    void seedLocalData_createsCompleteDemoDatasetWithStockImages() throws Exception {
        AtomicLong campaignIds = new AtomicLong(1L);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> assignStableId(invocation.getArgument(0)));
        when(walletRepository.findByUserId(anyString())).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> {
            Wallet wallet = invocation.getArgument(0);
            wallet.setId("wallet-" + wallet.getUserId());
            return wallet;
        });
        when(campaignRepository.findAll()).thenReturn(List.of());
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(invocation -> {
            Campaign campaign = invocation.getArgument(0);
            campaign.setId(campaignIds.getAndIncrement());
            return campaign;
        });
        when(donationRepository.findByUserIdOrderByCreatedAtDesc(anyString())).thenReturn(List.of());
        when(savedCampaignRepository.findByUserIdAndCampaignId(anyString(), anyString())).thenReturn(Optional.empty());

        seedRunner().run();

        ArgumentCaptor<AppUser> users = ArgumentCaptor.forClass(AppUser.class);
        ArgumentCaptor<Wallet> wallets = ArgumentCaptor.forClass(Wallet.class);
        ArgumentCaptor<Transaction> transactions = ArgumentCaptor.forClass(Transaction.class);
        ArgumentCaptor<Campaign> campaigns = ArgumentCaptor.forClass(Campaign.class);
        ArgumentCaptor<Donation> donations = ArgumentCaptor.forClass(Donation.class);
        ArgumentCaptor<SavedCampaign> savedCampaigns = ArgumentCaptor.forClass(SavedCampaign.class);

        verify(userRepository, times(2)).save(users.capture());
        verify(walletRepository, times(2)).save(wallets.capture());
        verify(transactionRepository, times(2)).save(transactions.capture());
        verify(campaignRepository, times(5)).save(campaigns.capture());
        verify(donationRepository, times(2)).save(donations.capture());
        verify(savedCampaignRepository, times(2)).save(savedCampaigns.capture());

        assertThat(users.getAllValues())
                .extracting(AppUser::getEmail)
                .containsExactlyInAnyOrder("test@donatjs.com", "admin@donatjs.com");
        assertThat(users.getAllValues())
                .filteredOn(AppUser::isAdmin)
                .extracting(AppUser::getEmail)
                .containsExactly("admin@donatjs.com");
        assertThat(wallets.getAllValues())
                .extracting(Wallet::getBalance)
                .containsExactlyInAnyOrder(1_500_000.0, 5_000_000.0);
        assertThat(transactions.getAllValues())
                .allSatisfy(transaction -> assertThat(transaction.getDescription()).isEqualTo("Opening balance (seed)"));

        assertThat(campaigns.getAllValues())
                .extracting(Campaign::getTitle)
                .containsExactly(
                        "Help Build a School in Sulawesi",
                        "Emergency Medical Aid Flood Victims",
                        "Monthly Food Packages for Orphanages",
                        "Awaiting Review: Reforestation Project",
                        "Almost There: Animal Shelter Renovation");
        assertThat(campaigns.getAllValues())
                .extracting(Campaign::getStatus)
                .contains(CampaignStatus.OPEN, CampaignStatus.WAITING);
        assertThat(campaigns.getAllValues())
                .allSatisfy(campaign -> assertThat(campaign.getImageUrl())
                        .startsWith("https://images.unsplash.com/")
                        .contains("auto=format"));

        assertThat(donations.getAllValues())
                .extracting(Donation::getFee)
                .containsExactlyInAnyOrder(0L, 1_500L);
        assertThat(savedCampaigns.getAllValues())
                .extracting(SavedCampaign::getCampaignImageUrl)
                .allSatisfy(imageUrl -> assertThat(imageUrl)
                        .startsWith("https://images.unsplash.com/")
                        .contains("auto=format"));
    }

    @Test
    void seedLocalData_refreshesExistingCampaignAndSavedImagesWithoutDuplicatingData() throws Exception {
        AppUser testUser = user("test@donatjs.com");
        AppUser adminUser = user("admin@donatjs.com");
        List<Campaign> existingCampaigns = existingCampaigns(testUser, adminUser);
        SavedCampaign savedCampaign = SavedCampaign.builder()
                .userId(testUser.getId().toString())
                .campaignId(String.valueOf(existingCampaigns.get(2).getId()))
                .campaignTitle(existingCampaigns.get(2).getTitle())
                .campaignOrganizer("DonatJS Team")
                .campaignImageUrl("https://old.example/image.jpg")
                .build();
        Donation existingSchoolDonation = Donation.builder()
                .campaignId(existingCampaigns.get(0).getId())
                .amount(250_000L)
                .build();
        Donation existingMedicalDonation = Donation.builder()
                .campaignId(existingCampaigns.get(1).getId())
                .amount(100_000L)
                .build();

        when(userRepository.findByEmail("test@donatjs.com")).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail("admin@donatjs.com")).thenReturn(Optional.of(adminUser));
        when(walletRepository.findByUserId(anyString())).thenReturn(Optional.of(Wallet.builder().id("existing").build()));
        when(campaignRepository.findAll()).thenReturn(existingCampaigns);
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(donationRepository.findByUserIdOrderByCreatedAtDesc(anyString()))
                .thenReturn(List.of(existingSchoolDonation, existingMedicalDonation));
        when(savedCampaignRepository.findByUserIdAndCampaignId(anyString(), anyString()))
                .thenReturn(Optional.of(savedCampaign));
        when(savedCampaignRepository.save(any(SavedCampaign.class))).thenAnswer(invocation -> invocation.getArgument(0));

        seedRunner().run();

        verify(userRepository, never()).save(any());
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
        verify(donationRepository, never()).save(any());
        verify(campaignRepository, times(5)).save(any(Campaign.class));
        verify(savedCampaignRepository, times(2)).save(any(SavedCampaign.class));

        assertThat(existingCampaigns)
                .extracting(Campaign::getImageUrl)
                .allSatisfy(imageUrl -> assertThat(imageUrl).startsWith("https://images.unsplash.com/"));
        assertThat(savedCampaign.getCampaignImageUrl())
                .startsWith("https://images.unsplash.com/")
                .contains("auto=format");
    }

    private CommandLineRunner seedRunner() {
        return initializer.seedLocalData(
                userRepository,
                passwordEncoder,
                walletRepository,
                transactionRepository,
                campaignRepository,
                donationRepository,
                savedCampaignRepository);
    }

    private AppUser assignStableId(AppUser user) {
        user.setId(UUID.nameUUIDFromBytes(user.getEmail().getBytes(StandardCharsets.UTF_8)));
        return user;
    }

    private AppUser user(String email) {
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setId(UUID.nameUUIDFromBytes(email.getBytes(StandardCharsets.UTF_8)));
        return user;
    }

    private List<Campaign> existingCampaigns(AppUser testUser, AppUser adminUser) {
        List<Campaign> campaigns = new ArrayList<>();
        campaigns.add(existingCampaign(1L, "Help Build a School in Sulawesi", adminUser));
        campaigns.add(existingCampaign(2L, "Emergency Medical Aid Flood Victims", adminUser));
        campaigns.add(existingCampaign(3L, "Monthly Food Packages for Orphanages", testUser));
        campaigns.add(existingCampaign(4L, "Awaiting Review: Reforestation Project", testUser));
        campaigns.add(existingCampaign(5L, "Almost There: Animal Shelter Renovation", adminUser));
        return campaigns;
    }

    private Campaign existingCampaign(Long id, String title, AppUser creator) {
        Campaign campaign = new Campaign();
        campaign.setId(id);
        campaign.setTitle(title);
        campaign.setDescription("Existing " + title);
        campaign.setTargetAmount(java.math.BigDecimal.TEN);
        campaign.setTotalRaised(java.math.BigDecimal.ZERO);
        campaign.setStatus(CampaignStatus.OPEN);
        campaign.setCreatorId(creator.getId().toString());
        campaign.setImageUrl("https://old.example/" + id + ".jpg");
        return campaign;
    }
}
