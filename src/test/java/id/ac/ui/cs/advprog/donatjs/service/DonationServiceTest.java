package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.dto.CreateDonationRequest;
import id.ac.ui.cs.advprog.donatjs.dto.DonationResponse;
import id.ac.ui.cs.advprog.donatjs.model.Donation;
import id.ac.ui.cs.advprog.donatjs.model.Donation.DonationStatus;
import id.ac.ui.cs.advprog.donatjs.model.Donation.DonationType;
import id.ac.ui.cs.advprog.donatjs.model.Donation.PaymentMethod;
import id.ac.ui.cs.advprog.donatjs.repository.DonationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DonationServiceTest {

    @Mock
    private DonationRepository donationRepository;

    @InjectMocks
    private DonationService donationService;

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
        Donation saved = buildSavedDonation(1L, 150_000L, PaymentMethod.GOPAY, 2_000L, DonationStatus.SUCCESS, DonationType.ONE_TIME);
        when(donationRepository.save(any())).thenReturn(saved);

        DonationResponse response = donationService.createDonation(
                buildRequest(150_000L, PaymentMethod.GOPAY, DonationType.ONE_TIME));

        assertEquals(2_000L,   response.getFee());
        assertEquals(152_000L, response.getTotalAmount());
    }

    @Test
    @DisplayName("BANK_BCA donation gets Rp 1,500 fee and correct total")
    void createDonation_bankBca_correctFeeAndTotal() {
        Donation saved = buildSavedDonation(2L, 150_000L, PaymentMethod.BANK_BCA, 1_500L, DonationStatus.SUCCESS, DonationType.ONE_TIME);
        when(donationRepository.save(any())).thenReturn(saved);

        DonationResponse response = donationService.createDonation(
                buildRequest(150_000L, PaymentMethod.BANK_BCA, DonationType.ONE_TIME));

        assertEquals(1_500L,   response.getFee());
        assertEquals(151_500L, response.getTotalAmount());
    }

    @Test
    @DisplayName("WALLET donation gets zero fee")
    void createDonation_wallet_zeroFee() {
        Donation saved = buildSavedDonation(3L, 100_000L, PaymentMethod.WALLET, 0L, DonationStatus.SUCCESS, DonationType.ONE_TIME);
        when(donationRepository.save(any())).thenReturn(saved);

        DonationResponse response = donationService.createDonation(
                buildRequest(100_000L, PaymentMethod.WALLET, DonationType.ONE_TIME));

        assertEquals(0L,       response.getFee());
        assertEquals(100_000L, response.getTotalAmount());
    }

    // ── createDonation — Rp 5,000,000 limit ──────────────────────────────────

    @Test
    @DisplayName("Donation of exactly Rp 5,000,000 is SUCCESS")
    void createDonation_exactlyMaxAmount_isSuccess() {
        Donation saved = buildSavedDonation(4L, 5_000_000L, PaymentMethod.GOPAY, 2_000L, DonationStatus.SUCCESS, DonationType.ONE_TIME);
        when(donationRepository.save(any())).thenReturn(saved);

        DonationResponse response = donationService.createDonation(
                buildRequest(5_000_000L, PaymentMethod.GOPAY, DonationType.ONE_TIME));

        assertEquals(DonationStatus.SUCCESS, response.getStatus());
    }

    @Test
    @DisplayName("Donation of Rp 5,000,001 is REJECTED")
    void createDonation_oneAboveMax_isRejected() {
        Donation saved = buildSavedDonation(5L, 5_000_001L, PaymentMethod.GOPAY, 2_000L, DonationStatus.REJECTED, DonationType.ONE_TIME);
        when(donationRepository.save(any())).thenReturn(saved);

        DonationResponse response = donationService.createDonation(
                buildRequest(5_000_001L, PaymentMethod.GOPAY, DonationType.ONE_TIME));

        assertEquals(DonationStatus.REJECTED, response.getStatus());
    }

    @Test
    @DisplayName("REJECTED donation is still persisted to the database")
    void createDonation_rejected_stillSavedToDb() {
        Donation saved = buildSavedDonation(6L, 9_000_000L, PaymentMethod.BANK_BCA, 1_500L, DonationStatus.REJECTED, DonationType.ONE_TIME);
        when(donationRepository.save(any())).thenReturn(saved);

        donationService.createDonation(
                buildRequest(9_000_000L, PaymentMethod.BANK_BCA, DonationType.ONE_TIME));

        ArgumentCaptor<Donation> captor = ArgumentCaptor.forClass(Donation.class);
        verify(donationRepository).save(captor.capture());
        assertEquals(DonationStatus.REJECTED, captor.getValue().getStatus());
    }

    @Test
    @DisplayName("REJECTED donation still has correct fee applied")
    void createDonation_rejected_feeStillApplied() {
        Donation saved = buildSavedDonation(7L, 9_000_000L, PaymentMethod.GOPAY, 2_000L, DonationStatus.REJECTED, DonationType.ONE_TIME);
        when(donationRepository.save(any())).thenReturn(saved);

        DonationResponse response = donationService.createDonation(
                buildRequest(9_000_000L, PaymentMethod.GOPAY, DonationType.ONE_TIME));

        assertEquals(2_000L, response.getFee());
    }

    // ── createDonation — subscription rules ──────────────────────────────────

    @Test
    @DisplayName("SUBSCRIPTION with WALLET is allowed")
    void createDonation_subscriptionWithWallet_isAllowed() {
        Donation saved = buildSavedDonation(8L, 50_000L, PaymentMethod.WALLET, 0L, DonationStatus.SUCCESS, DonationType.SUBSCRIPTION);
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
        Donation donation = buildSavedDonation(1L, 100_000L, PaymentMethod.GOPAY, 2_000L, DonationStatus.SUCCESS, DonationType.ONE_TIME);
        when(donationRepository.findById(1L)).thenReturn(Optional.of(donation));

        DonationResponse response = donationService.getDonationById(1L);

        assertEquals(1L, response.getId());
        assertEquals(DonationStatus.SUCCESS, response.getStatus());
    }

    @Test
    @DisplayName("getDonationById throws EntityNotFoundException when not found")
    void getDonationById_notFound_throws() {
        when(donationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> donationService.getDonationById(99L));
    }

    // ── getDonationsByUser ────────────────────────────────────────────────────

    @Test
    @DisplayName("getDonationsByUser returns list of donations for user")
    void getDonationsByUser_returnsList() {
        List<Donation> donations = List.of(
                buildSavedDonation(1L, 100_000L, PaymentMethod.GOPAY,    2_000L, DonationStatus.SUCCESS,  DonationType.ONE_TIME),
                buildSavedDonation(2L, 9_000_000L, PaymentMethod.BANK_BCA, 1_500L, DonationStatus.REJECTED, DonationType.ONE_TIME)
        );
        when(donationRepository.findByUserIdOrderByCreatedAtDesc("user-abc-123"))
                .thenReturn(donations);

        List<DonationResponse> result = donationService.getDonationsByUser("user-abc-123");

        assertEquals(2, result.size());
        assertEquals(DonationStatus.SUCCESS,  result.get(0).getStatus());
        assertEquals(DonationStatus.REJECTED, result.get(1).getStatus());
    }

    @Test
    @DisplayName("getDonationsByUser returns empty list when user has no donations")
    void getDonationsByUser_noDonations_returnsEmptyList() {
        when(donationRepository.findByUserIdOrderByCreatedAtDesc("user-xyz"))
                .thenReturn(List.of());

        List<DonationResponse> result = donationService.getDonationsByUser("user-xyz");

        assertTrue(result.isEmpty());
    }

    // ── getTotalDonationsByCampaign ───────────────────────────────────────────

    @Test
    @DisplayName("getTotalDonationsByCampaign returns sum from repository")
    void getTotalDonationsByCampaign_returnsSum() {
        when(donationRepository.sumSuccessfulAmountByCampaignId(1L)).thenReturn(500_000L);

        Long total = donationService.getTotalDonationsByCampaign(1L);

        assertEquals(500_000L, total);
    }

    @Test
    @DisplayName("getTotalDonationsByCampaign returns 0 when no donations")
    void getTotalDonationsByCampaign_noDonations_returnsZero() {
        when(donationRepository.sumSuccessfulAmountByCampaignId(1L)).thenReturn(0L);

        assertEquals(0L, donationService.getTotalDonationsByCampaign(1L));
    }

    // ── updateDonationNotes ───────────────────────────────────────────────────

    @Test
    @DisplayName("updateDonationNotes updates notes when owner calls it")
    void updateDonationNotes_owner_updatesSuccessfully() {
        Donation existing = buildSavedDonation(1L, 100_000L, PaymentMethod.GOPAY, 2_000L, DonationStatus.SUCCESS, DonationType.ONE_TIME);
        Donation updated  = buildSavedDonation(1L, 100_000L, PaymentMethod.GOPAY, 2_000L, DonationStatus.SUCCESS, DonationType.ONE_TIME);
        updated.setNotes("Updated notes");

        when(donationRepository.findByIdAndUserId(1L, "user-abc-123")).thenReturn(Optional.of(existing));
        when(donationRepository.save(any())).thenReturn(updated);

        DonationResponse response = donationService.updateDonationNotes(1L, "user-abc-123", "Updated notes");

        assertEquals("Updated notes", response.getNotes());
        verify(donationRepository).save(existing);
    }

    @Test
    @DisplayName("updateDonationNotes throws EntityNotFoundException for non-owner")
    void updateDonationNotes_nonOwner_throws() {
        when(donationRepository.findByIdAndUserId(1L, "other-user")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> donationService.updateDonationNotes(1L, "other-user", "Hacked notes"));
    }

    @Test
    @DisplayName("updateDonationNotes does not change amount or status")
    void updateDonationNotes_doesNotMutateAmountOrStatus() {
        Donation existing = buildSavedDonation(1L, 100_000L, PaymentMethod.GOPAY, 2_000L, DonationStatus.SUCCESS, DonationType.ONE_TIME);
        when(donationRepository.findByIdAndUserId(1L, "user-abc-123")).thenReturn(Optional.of(existing));
        when(donationRepository.save(any())).thenReturn(existing);

        donationService.updateDonationNotes(1L, "user-abc-123", "New note");

        ArgumentCaptor<Donation> captor = ArgumentCaptor.forClass(Donation.class);
        verify(donationRepository).save(captor.capture());
        assertEquals(100_000L,           captor.getValue().getAmount());
        assertEquals(DonationStatus.SUCCESS, captor.getValue().getStatus());
    }

    // ── countRejectedDonationsByUser ──────────────────────────────────────────

    @Test
    @DisplayName("countRejectedDonationsByUser returns count from repository")
    void countRejectedDonationsByUser_returnsCount() {
        when(donationRepository.countByUserIdAndStatus("user-abc-123", DonationStatus.REJECTED))
                .thenReturn(2L);

        assertEquals(2L, donationService.countRejectedDonationsByUser("user-abc-123"));
    }

    @Test
    @DisplayName("countRejectedDonationsByUser returns 0 when no rejections")
    void countRejectedDonationsByUser_noRejections_returnsZero() {
        when(donationRepository.countByUserIdAndStatus("user-abc-123", DonationStatus.REJECTED))
                .thenReturn(0L);

        assertEquals(0L, donationService.countRejectedDonationsByUser("user-abc-123"));
    }
}