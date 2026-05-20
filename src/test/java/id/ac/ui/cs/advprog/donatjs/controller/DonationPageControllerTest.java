package id.ac.ui.cs.advprog.donatjs.controller;

import id.ac.ui.cs.advprog.donatjs.dto.DonationResponse;
import id.ac.ui.cs.advprog.donatjs.service.CurrentUserService;
import id.ac.ui.cs.advprog.donatjs.service.DonationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DonationPageControllerTest {

    @Mock
    private DonationService donationService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private Model model;

    @InjectMocks
    private DonationPageController donationPageController;


    @Test
    void testMyDonationsPage() {
        String userId = "test-user-123";
        List<DonationResponse> donations = new ArrayList<>();
        donations.add(DonationResponse.builder().build());

        when(currentUserService.requireCurrentUserId()).thenReturn(userId);
        when(donationService.getDonationsByUser(userId)).thenReturn(donations);

        String viewName = donationPageController.myDonationsPage(model);

        assertEquals("donations", viewName);
        verify(model).addAttribute("donations", donations);
    }
}
