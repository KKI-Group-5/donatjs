package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.dto.CreateDonationRequest;
import org.junit.jupiter.api.BeforeEach;
import id.ac.ui.cs.advprog.donatjs.dto.DonationResponse;
import id.ac.ui.cs.advprog.donatjs.model.Campaign;
import id.ac.ui.cs.advprog.donatjs.model.CampaignStatus;
import id.ac.ui.cs.advprog.donatjs.model.Donation;
import id.ac.ui.cs.advprog.donatjs.model.Donation.DonationStatus;
import id.ac.ui.cs.advprog.donatjs.model.Donation.DonationType;
import id.ac.ui.cs.advprog.donatjs.model.Donation.PaymentMethod;
import id.ac.ui.cs.advprog.donatjs.repository.DonationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class DonationServiceTest {

    @Mock
    private DonationRepository donationRepository;

    @Mock
    private CampaignService campaignService;

    @Mock
    private WalletService walletService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DonationService donationService;

    // ── Setup ─────────────────────────────────────────────────────────────────

    @BeforeEach
    void setUp() {
        Campaign openCampaign = mock(Campaign.class);
        lenient().when(openCampaign.getStatus()).thenReturn(CampaignStatus.OPEN);
        lenient().when(openCampaign.getTitle()).thenReturn("Mocked Campaign");
        lenient().when(campaignService.findById(anyLong())).thenReturn(Optional.of(openCampaign));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private CreateDonationRequest buildRequest(long amount, PaymentMethod method, DonationType type) {
        CreateDonationRequest req = new CreateDonationRequest();
        req.setUserId("user-abc-123");
        req.setCampaignId(1L);
        req.setAmount(amount);
        req.setNotes("Test note");
        req.setPaymentMethod(method);
        req.setType(type);
        return req;
    }

    private Donation buildSavedDonation(long id, long amount, PaymentMethod method,
            long fee, DonationStatus status, DonationType type) {
        return Donation.builder()
                .id(id)
                .userId("user-abc-123")
                .campaignId(1L)
                .amount(amount)
                .notes("Test note")
                .paymentMethod(method)
                .fee(fee)
                .totalAmount(amount + fee)
                .status(status)
                .type(type)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ── createDonation — fee calculation ─────────────────────────────────────

    @Test
    @DisplayName("GOPAY donation gets Rp 2,000 fee and correct total")
    void createDonation_gopay_correctFeeAndTotal() {
        Donation saved = buildSavedDonation(1L, 150_000L, PaymentMethod.GOPAY, 2_000L, DonationStatus.SUCCESS,
                DonationType.ONE_TIME);
        when(donationRepository.save(any())).thenReturn(saved);

        DonationResponse response = donationService.createDonation(
                buildRequest(150_000L, PaymentMethod.GOPAY, DonationType.ONE_TIME));

        assertEquals(2_000L, response.getFee());
        assertEquals(152_000L, response.getTotalAmount());
        verify(campaignService).recordSuccessfulDonation(1L, BigDecimal.valueOf(150_000L));
    }

    @Test
    @DisplayName("BANK_BCA donation gets Rp 1,500 fee and correct total")
    void createDonation_bankBca_correctFeeAndTotal() {
        Donation saved = buildSavedDonation(2L, 150_000L, PaymentMethod.BANK_BCA, 1_500L, DonationStatus.SUCCESS,
                DonationType.ONE_TIME);
        when(donationRepository.save(any())).thenReturn(saved);

        DonationResponse response = donationService.createDonation(
                buildRequest(150_000L, PaymentMethod.BANK_BCA, DonationType.ONE_TIME));

        assertEquals(1_500L, response.getFee());
        assertEquals(151_500L, response.getTotalAmount());
    }

    @Test
    @DisplayName("WALLET donation gets zero fee")
    void createDonation_wallet_zeroFee() {
        Donation saved = buildSavedDonation(3L, 100_000L, PaymentMethod.WALLET, 0L, DonationStatus.SUCCESS,
                DonationType.ONE_TIME);
        when(donationRepository.save(any())).thenReturn(saved);

        DonationResponse response = donationService.createDonation(
                buildRequest(100_000L, PaymentMethod.WALLET, DonationType.ONE_TIME));

        assertEquals(0L, response.getFee());
        assertEquals(100_000L, response.getTotalAmount());
    }

    // ── createDonation — Rp 5,000,000 limit ──────────────────────────────────

    @Test
    @DisplayName("Donation of exactly Rp 5,000,000 is SUCCESS")
    void createDonation_exactlyMaxAmount_isSuccess() {
        Donation saved = buildSavedDonation(4L, 5_000_000L, PaymentMethod.GOPAY, 2_000L, DonationStatus.SUCCESS,
                DonationType.ONE_TIME);
        when(donationRepository.save(any())).thenReturn(saved);

        DonationResponse response = donationService.createDonation(
                buildRequest(5_000_000L, PaymentMethod.GOPAY, DonationType.ONE_TIME));

        assertEquals(DonationStatus.SUCCESS, response.getStatus());
    }

    @Test
    @DisplayName("Donation of Rp 5,000,001 is REJECTED")
    void createDonation_oneAboveMax_isRejected() {
        Donation saved = buildSavedDonation(5L, 5_000_001L, PaymentMethod.GOPAY, 2_000L, DonationStatus.REJECTED,
                DonationType.ONE_TIME);
        when(donationRepository.save(any())).thenReturn(saved);

        DonationResponse response = donationService.createDonation(
                buildRequest(5_000_001L, PaymentMethod.GOPAY, DonationType.ONE_TIME));

        assertEquals(DonationStatus.REJECTED, response.getStatus());
        verify(campaignService, never()).recordSuccessfulDonation(anyLong(), any());
    }

    @Test
    @DisplayName("REJECTED donation is still persisted to the database and fires event")
    void createDonation_rejected_stillSavedToDb() {
        Donation saved = buildSavedDonation(6L, 9_000_000L, PaymentMethod.BANK_BCA, 1_500L, DonationStatus.REJECTED, DonationType.ONE_TIME);
        when(donationRepository.save(any())).thenReturn(saved);

        donationService.createDonation(
                buildRequest(9_000_000L, PaymentMethod.BANK_BCA, DonationType.ONE_TIME));

        org.mockito.ArgumentCaptor<Donation> captor = org.mockito.ArgumentCaptor.forClass(Donation.class);
        verify(donationRepository).save(captor.capture());
        assertEquals(DonationStatus.REJECTED, captor.getValue().getStatus());
        
        org.mockito.ArgumentCaptor<id.ac.ui.cs.advprog.donatjs.event.RejectedDonationEvent> eventCaptor = org.mockito.ArgumentCaptor.forClass(id.ac.ui.cs.advprog.donatjs.event.RejectedDonationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertEquals("user-abc-123", eventCaptor.getValue().getDonation().getUserId());
    }

    @Test
    @DisplayName("REJECTED donation still has correct fee applied")
    void createDonation_rejected_feeStillApplied() {
        Donation saved = buildSavedDonation(7L, 9_000_000L, PaymentMethod.GOPAY, 2_000L, DonationStatus.REJECTED,
                DonationType.ONE_TIME);
        when(donationRepository.save(any())).thenReturn(saved);

        DonationResponse response = donationService.createDonation(
                buildRequest(9_000_000L, PaymentMethod.GOPAY, DonationType.ONE_TIME));

        assertEquals(2_000L, response.getFee());
    }

    // ── createDonation — subscription rules ──────────────────────────────────

    @Test
    @DisplayName("SUBSCRIPTION with WALLET is allowed")
    void createDonation_subscriptionWithWallet_isAllowed() {
        Donation saved = buildSavedDonation(8L, 50_000L, PaymentMethod.WALLET, 0L, DonationStatus.SUCCESS,
                DonationType.SUBSCRIPTION);
        when(donationRepository.save(any())).thenReturn(saved);

        assertDoesNotThrow(() -> donationService.createDonation(
                buildRequest(50_000L, PaymentMethod.WALLET, DonationType.SUBSCRIPTION)));
    }

    @Test
    @DisplayName("SUBSCRIPTION with non-WALLET method throws IllegalArgumentException")
    void createDonation_subscriptionWithGopay_throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> donationService.createDonation(
                        buildRequest(50_000L, PaymentMethod.GOPAY, DonationType.SUBSCRIPTION)));

        assertTrue(ex.getMessage().contains("WALLET"));
    }

    @Test
    @DisplayName("SUBSCRIPTION with BANK method throws IllegalArgumentException")
    void createDonation_subscriptionWithBank_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> donationService.createDonation(
                        buildRequest(50_000L, PaymentMethod.BANK_BCA, DonationType.SUBSCRIPTION)));
    }

    @Test
    @DisplayName("SUBSCRIPTION rejection does not call repository")
    void createDonation_subscriptionRejected_neverSaves() {
        assertThrows(IllegalArgumentException.class,
                () -> donationService.createDonation(
                        buildRequest(50_000L, PaymentMethod.GOPAY, DonationType.SUBSCRIPTION)));

        verify(donationRepository, never()).save(any());
    }

    // ── getDonationById ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getDonationById returns donation when found")
    void getDonationById_found_returnsResponse() {
        Donation donation = buildSavedDonation(1L, 100_000L, PaymentMethod.GOPAY, 2_000L, DonationStatus.SUCCESS,
                DonationType.ONE_TIME);
        when(donationRepository.findById(1L)).thenReturn(Optional.of(donation));

        DonationResponse response = donationService.getDonationById(1L);

        assertEquals(1L, response.getId());
        assertEquals(DonationStatus.SUCCESS, response.getStatus());
    }

    @Test
    @DisplayName("getDonationById throws EntityNotFoundException when not found")
    void getDonationById_notFound_throwsEntityNotFoundException() {
        when(donationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> donationService.getDonationById(999L));
    }

    @Test
    void testGetDonationsByUser() {
        Donation donation = buildSavedDonation(1L, 100L, PaymentMethod.GOPAY, 0L, DonationStatus.SUCCESS, DonationType.ONE_TIME);
        when(donationRepository.findByUserIdOrderByCreatedAtDesc("user-1")).thenReturn(java.util.Arrays.asList(donation));
        java.util.List<DonationResponse> res = donationService.getDonationsByUser("user-1");
        assertEquals(1, res.size());
    }

    @Test
    void testGetTotalDonationsByCampaign() {
        when(donationRepository.sumSuccessfulAmountByCampaignId(1L)).thenReturn(500L);
        Long total = donationService.getTotalDonationsByCampaign(1L);
        assertEquals(500L, total);
    }

    @Test
    void testGetSuccessfulDonationsByCampaign() {
        Donation donation = buildSavedDonation(1L, 100L, PaymentMethod.GOPAY, 0L, DonationStatus.SUCCESS, DonationType.ONE_TIME);
        when(donationRepository.findByCampaignIdAndStatus(1L, Donation.DonationStatus.SUCCESS)).thenReturn(java.util.Arrays.asList(donation));
        java.util.List<DonationResponse> res = donationService.getSuccessfulDonationsByCampaign(1L);
        assertEquals(1, res.size());
    }

    @Test
    void testUpdateDonationNotesSuccess() {
        Donation donation = buildSavedDonation(1L, 100L, PaymentMethod.GOPAY, 0L, DonationStatus.SUCCESS, DonationType.ONE_TIME);
        when(donationRepository.findByIdAndUserId(1L, "user-1")).thenReturn(Optional.of(donation));
        when(donationRepository.save(donation)).thenReturn(donation);
        
        DonationResponse res = donationService.updateDonationNotes(1L, "user-1", "New Notes");
        assertNotNull(res);
        assertEquals("New Notes", donation.getNotes());
    }

    @Test
    void testUpdateDonationNotesNotFound() {
        when(donationRepository.findByIdAndUserId(10L, "user-1")).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> donationService.updateDonationNotes(10L, "user-1", "Notes"));
    }

    @Test
    void testCountRejectedDonationsByUser() {
        when(donationRepository.countByUserIdAndStatus("user-1", Donation.DonationStatus.REJECTED)).thenReturn(3L);
        assertEquals(3L, donationService.countRejectedDonationsByUser("user-1"));
    }

    @Test
    void testProcessRefundForCampaign() {
        Donation donation = buildSavedDonation(1L, 100L, PaymentMethod.GOPAY, 0L, DonationStatus.SUCCESS, DonationType.ONE_TIME);
        when(donationRepository.findByCampaignIdAndStatus(1L, Donation.DonationStatus.SUCCESS)).thenReturn(java.util.Arrays.asList(donation));

        donationService.processRefundForCampaign(1L);

        assertEquals(Donation.DonationStatus.REFUNDED, donation.getStatus());
        verify(donationRepository, times(1)).saveAll(anyList());
    }

    // ── createDonation — campaign validation ──────────────────────────────────

    @Test
    @DisplayName("Campaign not found throws EntityNotFoundException")
    void createDonation_campaignNotFound_throwsEntityNotFoundException() {
        when(campaignService.findById(99L)).thenReturn(Optional.empty());

        CreateDonationRequest req = buildRequest(100_000L, PaymentMethod.GOPAY, DonationType.ONE_TIME);
        req.setCampaignId(99L);

        assertThrows(jakarta.persistence.EntityNotFoundException.class,
                () -> donationService.createDonation(req));
    }

    @Test
    @DisplayName("Campaign not OPEN throws IllegalStateException")
    void createDonation_campaignNotOpen_throwsIllegalStateException() {
        Campaign closedCampaign = mock(Campaign.class);
        when(closedCampaign.getStatus()).thenReturn(id.ac.ui.cs.advprog.donatjs.model.CampaignStatus.CLOSED);
        when(campaignService.findById(2L)).thenReturn(Optional.of(closedCampaign));

        CreateDonationRequest req = buildRequest(100_000L, PaymentMethod.GOPAY, DonationType.ONE_TIME);
        req.setCampaignId(2L);

        assertThrows(IllegalStateException.class, () -> donationService.createDonation(req));
    }

    @Test
    @DisplayName("WALLET payment deducts from wallet on success")
    void createDonation_walletPayment_deductsWallet() {
        Donation saved = buildSavedDonation(10L, 100_000L, PaymentMethod.WALLET, 0L, DonationStatus.SUCCESS, DonationType.ONE_TIME);
        when(donationRepository.save(any())).thenReturn(saved);

        donationService.createDonation(buildRequest(100_000L, PaymentMethod.WALLET, DonationType.ONE_TIME));

        verify(walletService).deductForDonation(eq("user-abc-123"), anyDouble(), any());
    }

    @Test
    @DisplayName("Insufficient wallet balance throws InsufficientBalanceException")
    void createDonation_walletPayment_insufficientBalance_throws() {
        doThrow(new id.ac.ui.cs.advprog.donatjs.exception.InsufficientBalanceException("Not enough"))
                .when(walletService).deductForDonation(any(), anyDouble(), any());

        assertThrows(id.ac.ui.cs.advprog.donatjs.exception.InsufficientBalanceException.class,
                () -> donationService.createDonation(buildRequest(100_000L, PaymentMethod.WALLET, DonationType.ONE_TIME)));

        verify(donationRepository, never()).save(any());
    }

    // ── leaderboard ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getOverallLeaderboard maps rows to LeaderboardEntry with correct ranks")
    void getOverallLeaderboard_mapsCorrectly() {
        java.util.List<Object[]> rows = java.util.Arrays.asList(
                new Object[]{"user-A", 500_000L, 3L, 2L},
                new Object[]{"user-B", 200_000L, 1L, 1L}
        );
        when(donationRepository.findOverallTopDonators(any())).thenReturn(rows);

        java.util.List<id.ac.ui.cs.advprog.donatjs.dto.LeaderboardEntry> result =
                donationService.getOverallLeaderboard(10);

        assertEquals(2, result.size());
        assertEquals(1, result.get(0).getRank());
        assertEquals("user-A", result.get(0).getUserId());
        assertEquals(500_000L, result.get(0).getTotalAmount());
        assertEquals(2, result.get(1).getRank());
    }

    @Test
    @DisplayName("getOverallLeaderboard with empty result returns empty list")
    void getOverallLeaderboard_empty_returnsEmptyList() {
        when(donationRepository.findOverallTopDonators(any()))
                .thenReturn(java.util.Collections.emptyList());

        assertTrue(donationService.getOverallLeaderboard(10).isEmpty());
    }

    @Test
    @DisplayName("getCampaignLeaderboard maps rows to LeaderboardEntry with correct ranks")
    void getCampaignLeaderboard_mapsCorrectly() {
        java.util.List<Object[]> rows = java.util.Collections.singletonList(
                new Object[]{"user-C", 100_000L, 2L}
        );
        when(donationRepository.findTopDonatorsByCampaign(eq(5L), any())).thenReturn(rows);

        java.util.List<id.ac.ui.cs.advprog.donatjs.dto.LeaderboardEntry> result =
                donationService.getCampaignLeaderboard(5L, 10);

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getRank());
        assertEquals("user-C", result.get(0).getUserId());
        assertEquals(100_000L, result.get(0).getTotalAmount());
    }

    @Test
    @DisplayName("getCampaignLeaderboard with empty rows returns empty list")
    void getCampaignLeaderboard_empty_returnsEmptyList() {
        when(donationRepository.findTopDonatorsByCampaign(eq(5L), any()))
                .thenReturn(java.util.Collections.emptyList());

        assertTrue(donationService.getCampaignLeaderboard(5L, 10).isEmpty());
    }
}