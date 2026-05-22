package id.ac.ui.cs.advprog.donatjs.controller;

import id.ac.ui.cs.advprog.donatjs.dto.CreateDonationRequest;
import id.ac.ui.cs.advprog.donatjs.dto.DonationResponse;
import id.ac.ui.cs.advprog.donatjs.dto.LeaderboardEntry;
import id.ac.ui.cs.advprog.donatjs.dto.WithdrawalRequestResponse;
import id.ac.ui.cs.advprog.donatjs.model.Donation.DonationStatus;
import id.ac.ui.cs.advprog.donatjs.model.WithdrawalRequestStatus;
import id.ac.ui.cs.advprog.donatjs.service.CurrentUserService;
import id.ac.ui.cs.advprog.donatjs.service.DonationService;
import id.ac.ui.cs.advprog.donatjs.service.WithdrawalRequestService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DonationControllerTest {

    @Mock
    private DonationService donationService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private WithdrawalRequestService withdrawalRequestService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private DonationController donationController;


    @Test
    void testCreateDonationSuccess() {
        CreateDonationRequest request = new CreateDonationRequest();
        when(currentUserService.getCurrentUserId(authentication)).thenReturn("user-1");
        
        DonationResponse response = DonationResponse.builder().status(DonationStatus.SUCCESS).build();
        when(donationService.createDonation(any())).thenReturn(response);

        ResponseEntity<DonationResponse> res = donationController.createDonation(request, authentication);

        assertEquals(HttpStatus.CREATED, res.getStatusCode());
        assertEquals(response, res.getBody());
    }

    @Test
    void testCreateDonationRejected() {
        CreateDonationRequest request = new CreateDonationRequest();
        when(currentUserService.getCurrentUserId(authentication)).thenReturn("user-1");
        
        DonationResponse response = DonationResponse.builder().status(DonationStatus.REJECTED).build();
        when(donationService.createDonation(any())).thenReturn(response);

        ResponseEntity<DonationResponse> res = donationController.createDonation(request, authentication);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, res.getStatusCode());
        assertEquals(response, res.getBody());
    }

    @Test
    void testGetDonationById() {
        DonationResponse response = DonationResponse.builder().build();
        when(donationService.getDonationById(1L)).thenReturn(response);

        ResponseEntity<DonationResponse> res = donationController.getDonationById(1L);
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void testGetDonationsByUser() {
        when(donationService.getDonationsByUser("user-1")).thenReturn(Arrays.asList(DonationResponse.builder().build()));
        ResponseEntity<List<DonationResponse>> res = donationController.getDonationsByUser("user-1");
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(1, java.util.Objects.requireNonNull(res.getBody()).size());
    }

    @Test
    void testGetTotalDonationsByCampaign() {
        when(donationService.getTotalDonationsByCampaign(1L)).thenReturn(100L);
        ResponseEntity<Long> res = donationController.getTotalDonationsByCampaign(1L);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(100L, res.getBody());
    }

    @Test
    void testGetSuccessfulDonationsByCampaign() {
        when(donationService.getSuccessfulDonationsByCampaign(1L)).thenReturn(Arrays.asList(DonationResponse.builder().build()));
        ResponseEntity<List<DonationResponse>> res = donationController.getSuccessfulDonationsByCampaign(1L);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(1, java.util.Objects.requireNonNull(res.getBody()).size());
    }

    @Test
    void testUpdateDonationNotes() {
        DonationResponse response = DonationResponse.builder().build();
        when(donationService.updateDonationNotes(1L, "user-1", "notes")).thenReturn(response);
        ResponseEntity<DonationResponse> res = donationController.updateDonationNotes(1L, "user-1", "notes");
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void testGetRejectedDonationCount() {
        when(donationService.countRejectedDonationsByUser("user-1")).thenReturn(5L);
        ResponseEntity<Long> res = donationController.getRejectedDonationCount("user-1");
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(5L, res.getBody());
    }

    @Test
    void testProcessCampaignRefund() {
        ResponseEntity<Void> res = donationController.processCampaignRefund(1L);
        verify(donationService).processRefundForCampaign(1L);
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void testHandleIllegalArgument() {
        ResponseEntity<?> res = donationController.handleIllegalArgument(new IllegalArgumentException("error msg"));
        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
    }

    @Test
    void testHandleIllegalState() {
        ResponseEntity<?> res = donationController.handleIllegalState(new IllegalStateException("error state"));
        assertEquals(HttpStatus.CONFLICT, res.getStatusCode());
    }

    @Test
    void testGetOverallLeaderboard() {
        LeaderboardEntry entry = LeaderboardEntry.builder().rank(1).userId("u1").totalAmount(100L).build();
        when(donationService.getOverallLeaderboard(10)).thenReturn(Arrays.asList(entry));
        ResponseEntity<List<LeaderboardEntry>> res = donationController.getOverallLeaderboard(10);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(1, java.util.Objects.requireNonNull(res.getBody()).size());
    }

    @Test
    void testGetCampaignLeaderboard() {
        LeaderboardEntry entry = LeaderboardEntry.builder().rank(1).userId("u1").totalAmount(200L).build();
        when(donationService.getCampaignLeaderboard(1L, 5)).thenReturn(Arrays.asList(entry));
        ResponseEntity<List<LeaderboardEntry>> res = donationController.getCampaignLeaderboard(1L, 5);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(1, java.util.Objects.requireNonNull(res.getBody()).size());
    }

    @Test
    void testRequestWithdrawal() {
        when(currentUserService.getCurrentUserId(authentication)).thenReturn("user-1");
        WithdrawalRequestResponse response = WithdrawalRequestResponse.builder()
                .id(1L).donationId(5L).userId("user-1")
                .status(WithdrawalRequestStatus.PENDING).build();
        when(withdrawalRequestService.requestWithdrawal(5L, "user-1", "reason")).thenReturn(response);

        ResponseEntity<WithdrawalRequestResponse> res =
                donationController.requestWithdrawal(5L, "reason", authentication);

        assertEquals(HttpStatus.CREATED, res.getStatusCode());
        assertEquals(response, res.getBody());
    }

    @Test
    void testGetPendingWithdrawalRequests() {
        WithdrawalRequestResponse response = WithdrawalRequestResponse.builder()
                .id(2L).status(WithdrawalRequestStatus.PENDING).build();
        when(withdrawalRequestService.getPendingRequests()).thenReturn(Arrays.asList(response));

        ResponseEntity<List<WithdrawalRequestResponse>> res = donationController.getPendingWithdrawalRequests();

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(1, java.util.Objects.requireNonNull(res.getBody()).size());
    }

    @Test
    void testApproveWithdrawal() {
        WithdrawalRequestResponse response = WithdrawalRequestResponse.builder()
                .id(3L).status(WithdrawalRequestStatus.APPROVED).build();
        when(withdrawalRequestService.approveWithdrawal(3L)).thenReturn(response);

        ResponseEntity<WithdrawalRequestResponse> res = donationController.approveWithdrawal(3L);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(WithdrawalRequestStatus.APPROVED, java.util.Objects.requireNonNull(res.getBody()).getStatus());
    }

    @Test
    void testRejectWithdrawal() {
        WithdrawalRequestResponse response = WithdrawalRequestResponse.builder()
                .id(4L).status(WithdrawalRequestStatus.REJECTED).build();
        when(withdrawalRequestService.rejectWithdrawal(4L)).thenReturn(response);

        ResponseEntity<WithdrawalRequestResponse> res = donationController.rejectWithdrawal(4L);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(WithdrawalRequestStatus.REJECTED, java.util.Objects.requireNonNull(res.getBody()).getStatus());
    }
}
