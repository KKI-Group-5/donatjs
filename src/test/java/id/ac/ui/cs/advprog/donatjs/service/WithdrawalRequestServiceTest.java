package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.dto.WithdrawalRequestResponse;
import id.ac.ui.cs.advprog.donatjs.model.Donation;
import id.ac.ui.cs.advprog.donatjs.model.WithdrawalRequest;
import id.ac.ui.cs.advprog.donatjs.model.WithdrawalRequestStatus;
import id.ac.ui.cs.advprog.donatjs.repository.DonationRepository;
import id.ac.ui.cs.advprog.donatjs.repository.WithdrawalRequestRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class WithdrawalRequestServiceTest {

    @Mock
    private WithdrawalRequestRepository withdrawalRequestRepository;

    @Mock
    private DonationRepository donationRepository;

    @InjectMocks
    private WithdrawalRequestService withdrawalRequestService;

    private Donation successDonation;
    private WithdrawalRequest pendingRequest;

    @BeforeEach
    void setUp() {
        successDonation = Donation.builder()
                .id(1L)
                .userId("user-1")
                .campaignId(10L)
                .amount(100_000L)
                .status(Donation.DonationStatus.SUCCESS)
                .build();

        pendingRequest = WithdrawalRequest.builder()
                .id(5L)
                .donationId(1L)
                .userId("user-1")
                .status(WithdrawalRequestStatus.PENDING)
                .reason("Need money back")
                .requestedAt(LocalDateTime.now())
                .build();
    }

    // ── requestWithdrawal ─────────────────────────────────────────────────────

    @Test
    void requestWithdrawal_success_returnsSavedResponse() {
        when(donationRepository.findByIdAndUserId(1L, "user-1")).thenReturn(Optional.of(successDonation));
        when(withdrawalRequestRepository.existsByDonationIdAndStatus(1L, WithdrawalRequestStatus.PENDING)).thenReturn(false);
        when(withdrawalRequestRepository.save(any())).thenReturn(pendingRequest);

        WithdrawalRequestResponse response = withdrawalRequestService.requestWithdrawal(1L, "user-1", "reason");

        assertThat(response).isNotNull();
        assertThat(response.getDonationId()).isEqualTo(1L);
        assertThat(response.getUserId()).isEqualTo("user-1");
        assertThat(response.getStatus()).isEqualTo(WithdrawalRequestStatus.PENDING);

        ArgumentCaptor<WithdrawalRequest> captor = ArgumentCaptor.forClass(WithdrawalRequest.class);
        verify(withdrawalRequestRepository).save(captor.capture());
        assertThat(captor.getValue().getReason()).isEqualTo("reason");
    }

    @Test
    void requestWithdrawal_donationNotFound_throwsEntityNotFoundException() {
        when(donationRepository.findByIdAndUserId(99L, "user-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> withdrawalRequestService.requestWithdrawal(99L, "user-1", null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void requestWithdrawal_donationNotSuccess_throwsIllegalStateException() {
        successDonation.setStatus(Donation.DonationStatus.REJECTED);
        when(donationRepository.findByIdAndUserId(1L, "user-1")).thenReturn(Optional.of(successDonation));

        assertThatThrownBy(() -> withdrawalRequestService.requestWithdrawal(1L, "user-1", "reason"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SUCCESS");
    }

    @Test
    void requestWithdrawal_duplicatePending_throwsIllegalStateException() {
        when(donationRepository.findByIdAndUserId(1L, "user-1")).thenReturn(Optional.of(successDonation));
        when(withdrawalRequestRepository.existsByDonationIdAndStatus(1L, WithdrawalRequestStatus.PENDING)).thenReturn(true);

        assertThatThrownBy(() -> withdrawalRequestService.requestWithdrawal(1L, "user-1", "reason"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already pending");
    }

    @Test
    void requestWithdrawal_nullReason_savesWithNullReason() {
        when(donationRepository.findByIdAndUserId(1L, "user-1")).thenReturn(Optional.of(successDonation));
        when(withdrawalRequestRepository.existsByDonationIdAndStatus(1L, WithdrawalRequestStatus.PENDING)).thenReturn(false);
        WithdrawalRequest saved = WithdrawalRequest.builder().id(6L).donationId(1L).userId("user-1")
                .status(WithdrawalRequestStatus.PENDING).requestedAt(LocalDateTime.now()).build();
        when(withdrawalRequestRepository.save(any())).thenReturn(saved);

        WithdrawalRequestResponse response = withdrawalRequestService.requestWithdrawal(1L, "user-1", null);
        assertThat(response).isNotNull();
    }

    // ── getPendingRequests ────────────────────────────────────────────────────

    @Test
    void getPendingRequests_returnsMappedResponses() {
        when(withdrawalRequestRepository.findByStatusOrderByRequestedAtDesc(WithdrawalRequestStatus.PENDING))
                .thenReturn(List.of(pendingRequest));

        List<WithdrawalRequestResponse> result = withdrawalRequestService.getPendingRequests();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(WithdrawalRequestStatus.PENDING);
    }

    @Test
    void getPendingRequests_empty_returnsEmptyList() {
        when(withdrawalRequestRepository.findByStatusOrderByRequestedAtDesc(WithdrawalRequestStatus.PENDING))
                .thenReturn(List.of());

        assertThat(withdrawalRequestService.getPendingRequests()).isEmpty();
    }

    // ── getRequestsByUser ─────────────────────────────────────────────────────

    @Test
    void getRequestsByUser_returnsMappedResponses() {
        when(withdrawalRequestRepository.findByUserIdOrderByRequestedAtDesc("user-1"))
                .thenReturn(List.of(pendingRequest));

        List<WithdrawalRequestResponse> result = withdrawalRequestService.getRequestsByUser("user-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo("user-1");
    }

    @Test
    void getRequestsByUser_noRequests_returnsEmptyList() {
        when(withdrawalRequestRepository.findByUserIdOrderByRequestedAtDesc("user-2"))
                .thenReturn(List.of());

        assertThat(withdrawalRequestService.getRequestsByUser("user-2")).isEmpty();
    }

    // ── approveWithdrawal ─────────────────────────────────────────────────────

    @Test
    void approveWithdrawal_success_setsDonationRefundedAndRequestApproved() {
        when(withdrawalRequestRepository.findById(5L)).thenReturn(Optional.of(pendingRequest));
        when(donationRepository.findById(1L)).thenReturn(Optional.of(successDonation));
        when(donationRepository.save(any())).thenReturn(successDonation);
        WithdrawalRequest approved = WithdrawalRequest.builder()
                .id(5L).donationId(1L).userId("user-1")
                .status(WithdrawalRequestStatus.APPROVED)
                .requestedAt(LocalDateTime.now())
                .processedAt(LocalDateTime.now())
                .build();
        when(withdrawalRequestRepository.save(any())).thenReturn(approved);

        WithdrawalRequestResponse response = withdrawalRequestService.approveWithdrawal(5L);

        assertThat(response.getStatus()).isEqualTo(WithdrawalRequestStatus.APPROVED);
        verify(donationRepository).save(successDonation);
        assertThat(successDonation.getStatus()).isEqualTo(Donation.DonationStatus.REFUNDED);
    }

    @Test
    void approveWithdrawal_requestNotFound_throwsEntityNotFoundException() {
        when(withdrawalRequestRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> withdrawalRequestService.approveWithdrawal(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void approveWithdrawal_notPendingStatus_throwsIllegalStateException() {
        pendingRequest.setStatus(WithdrawalRequestStatus.APPROVED);
        when(withdrawalRequestRepository.findById(5L)).thenReturn(Optional.of(pendingRequest));

        assertThatThrownBy(() -> withdrawalRequestService.approveWithdrawal(5L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    void approveWithdrawal_donationNotFound_throwsEntityNotFoundException() {
        when(withdrawalRequestRepository.findById(5L)).thenReturn(Optional.of(pendingRequest));
        when(donationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> withdrawalRequestService.approveWithdrawal(5L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── rejectWithdrawal ──────────────────────────────────────────────────────

    @Test
    void rejectWithdrawal_success_setsStatusRejected() {
        when(withdrawalRequestRepository.findById(5L)).thenReturn(Optional.of(pendingRequest));
        WithdrawalRequest rejected = WithdrawalRequest.builder()
                .id(5L).donationId(1L).userId("user-1")
                .status(WithdrawalRequestStatus.REJECTED)
                .requestedAt(LocalDateTime.now())
                .processedAt(LocalDateTime.now())
                .build();
        when(withdrawalRequestRepository.save(any())).thenReturn(rejected);

        WithdrawalRequestResponse response = withdrawalRequestService.rejectWithdrawal(5L);

        assertThat(response.getStatus()).isEqualTo(WithdrawalRequestStatus.REJECTED);
        verify(withdrawalRequestRepository).save(pendingRequest);
    }

    @Test
    void rejectWithdrawal_requestNotFound_throwsEntityNotFoundException() {
        when(withdrawalRequestRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> withdrawalRequestService.rejectWithdrawal(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void rejectWithdrawal_notPendingStatus_throwsIllegalStateException() {
        pendingRequest.setStatus(WithdrawalRequestStatus.REJECTED);
        when(withdrawalRequestRepository.findById(5L)).thenReturn(Optional.of(pendingRequest));

        assertThatThrownBy(() -> withdrawalRequestService.rejectWithdrawal(5L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDING");
    }
}
