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
@SuppressWarnings("null")
class WithdrawalRequestServiceTest {

    @Mock
    private WithdrawalRequestRepository withdrawalRequestRepository;

    @Mock
    private DonationRepository donationRepository;

    @InjectMocks
    private WithdrawalRequestService withdrawalRequestService;

    private Donation validDonation;
    private WithdrawalRequest pendingRequest;

    @BeforeEach
    void setUp() {
        validDonation = new Donation();
        validDonation.setId(1L);
        validDonation.setUserId("user-1");
        validDonation.setStatus(Donation.DonationStatus.SUCCESS);

        pendingRequest = new WithdrawalRequest();
        pendingRequest.setId(10L);
        pendingRequest.setDonationId(1L);
        pendingRequest.setUserId("user-1");
        pendingRequest.setStatus(WithdrawalRequestStatus.PENDING);
        pendingRequest.setRequestedAt(LocalDateTime.now());
    }

    @Test
    void requestWithdrawal_Success() {
        when(donationRepository.findByIdAndUserId(1L, "user-1")).thenReturn(Optional.of(validDonation));
        when(withdrawalRequestRepository.existsByDonationIdAndStatus(1L, WithdrawalRequestStatus.PENDING)).thenReturn(false);
        when(withdrawalRequestRepository.save(any(WithdrawalRequest.class))).thenAnswer(i -> {
            WithdrawalRequest req = i.getArgument(0);
            req.setId(100L);
            return req;
        });

        WithdrawalRequestResponse response = withdrawalRequestService.requestWithdrawal(1L, "user-1", "Need refund");

        assertNotNull(response);
        assertEquals(WithdrawalRequestStatus.PENDING, response.getStatus());
        assertEquals("Need refund", response.getReason());
        verify(withdrawalRequestRepository).save(any(WithdrawalRequest.class));
    }

    @Test
    void requestWithdrawal_DonationNotFound() {
        when(donationRepository.findByIdAndUserId(1L, "user-1")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                withdrawalRequestService.requestWithdrawal(1L, "user-1", "Reason"));
    }

    @Test
    void requestWithdrawal_NotSuccessStatus() {
        validDonation.setStatus(Donation.DonationStatus.REJECTED);
        when(donationRepository.findByIdAndUserId(1L, "user-1")).thenReturn(Optional.of(validDonation));

        assertThrows(IllegalStateException.class, () ->
                withdrawalRequestService.requestWithdrawal(1L, "user-1", "Reason"));
    }

    @Test
    void requestWithdrawal_AlreadyPending() {
        when(donationRepository.findByIdAndUserId(1L, "user-1")).thenReturn(Optional.of(validDonation));
        when(withdrawalRequestRepository.existsByDonationIdAndStatus(1L, WithdrawalRequestStatus.PENDING)).thenReturn(true);

        assertThrows(IllegalStateException.class, () ->
                withdrawalRequestService.requestWithdrawal(1L, "user-1", "Reason"));
    }

    @Test
    void getPendingRequests_Success() {
        when(withdrawalRequestRepository.findByStatusOrderByRequestedAtDesc(WithdrawalRequestStatus.PENDING))
                .thenReturn(List.of(pendingRequest));

        List<WithdrawalRequestResponse> responses = withdrawalRequestService.getPendingRequests();

        assertEquals(1, responses.size());
        assertEquals(10L, responses.get(0).getId());
    }

    @Test
    void getRequestsByUser_Success() {
        when(withdrawalRequestRepository.findByUserIdOrderByRequestedAtDesc("user-1"))
                .thenReturn(List.of(pendingRequest));

        List<WithdrawalRequestResponse> responses = withdrawalRequestService.getRequestsByUser("user-1");

        assertEquals(1, responses.size());
    }

    @Test
    void approveWithdrawal_Success() {
        when(withdrawalRequestRepository.findById(10L)).thenReturn(Optional.of(pendingRequest));
        when(donationRepository.findById(1L)).thenReturn(Optional.of(validDonation));
        when(withdrawalRequestRepository.save(any(WithdrawalRequest.class))).thenReturn(pendingRequest);

        WithdrawalRequestResponse response = withdrawalRequestService.approveWithdrawal(10L);

        assertEquals(WithdrawalRequestStatus.APPROVED, response.getStatus());
        assertEquals(Donation.DonationStatus.REFUNDED, validDonation.getStatus());
        verify(donationRepository).save(validDonation);
    }

    @Test
    void approveWithdrawal_RequestNotFound() {
        when(withdrawalRequestRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> withdrawalRequestService.approveWithdrawal(10L));
    }

    @Test
    void approveWithdrawal_NotPending() {
        pendingRequest.setStatus(WithdrawalRequestStatus.APPROVED);
        when(withdrawalRequestRepository.findById(10L)).thenReturn(Optional.of(pendingRequest));

        assertThrows(IllegalStateException.class, () -> withdrawalRequestService.approveWithdrawal(10L));
    }

    @Test
    void approveWithdrawal_DonationNotFound() {
        when(withdrawalRequestRepository.findById(10L)).thenReturn(Optional.of(pendingRequest));
        when(donationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> withdrawalRequestService.approveWithdrawal(10L));
    }

    @Test
    void rejectWithdrawal_Success() {
        when(withdrawalRequestRepository.findById(10L)).thenReturn(Optional.of(pendingRequest));
        when(withdrawalRequestRepository.save(any(WithdrawalRequest.class))).thenReturn(pendingRequest);

        WithdrawalRequestResponse response = withdrawalRequestService.rejectWithdrawal(10L);

        assertEquals(WithdrawalRequestStatus.REJECTED, response.getStatus());
        assertNotNull(pendingRequest.getProcessedAt());
    }
}
