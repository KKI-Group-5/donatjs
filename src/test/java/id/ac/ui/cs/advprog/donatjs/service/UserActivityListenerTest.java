package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.dto.DonationResponse;
import id.ac.ui.cs.advprog.donatjs.event.RejectedCampaignEvent;
import id.ac.ui.cs.advprog.donatjs.event.RejectedDonationEvent;
import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import id.ac.ui.cs.advprog.donatjs.model.Campaign;
import id.ac.ui.cs.advprog.donatjs.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public @SuppressWarnings("null")
class UserActivityListenerTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserActivityListener userActivityListener;

    private AppUser sampleUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sampleUser = new AppUser();
        sampleUser.setId(userId);
        sampleUser.setRejectedDonationCount(0);
        sampleUser.setSuspended(false);
    }

    @Test
    void testHandleRejectedDonation_IncrementsCount() {
        // Arrange
        DonationResponse donation = DonationResponse.builder()
                .userId(userId.toString())
                .build();
        RejectedDonationEvent event = new RejectedDonationEvent(this, donation);

        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

        // Act
        userActivityListener.handleRejectedDonation(event);

        // Assert
        assertEquals(1, sampleUser.getRejectedDonationCount());
        assertFalse(sampleUser.isSuspended());
        verify(userRepository).save(sampleUser);
    }

    @Test
    void testHandleRejectedDonation_SuspendsUserAtThreshold() {
        // Arrange
        sampleUser.setRejectedDonationCount(2); // Current count is 2
        DonationResponse donation = DonationResponse.builder()
                .userId(userId.toString())
                .build();
        RejectedDonationEvent event = new RejectedDonationEvent(this, donation);

        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

        // Act
        userActivityListener.handleRejectedDonation(event);

        // Assert
        assertEquals(3, sampleUser.getRejectedDonationCount());
        assertTrue(sampleUser.isSuspended());
        verify(userRepository).save(sampleUser);
    }

    @Test
    void testHandleRejectedCampaign_IncrementsCount() {
        Campaign campaign = new Campaign();
        campaign.setCreatorId(userId.toString());
        RejectedCampaignEvent event = new RejectedCampaignEvent(this, campaign);

        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

        userActivityListener.handleRejectedCampaign(event);

        assertEquals(1, sampleUser.getRejectedCampaignCount());
        assertFalse(sampleUser.isSuspended());
        verify(userRepository).save(sampleUser);
    }

    @Test
    void testHandleRejectedCampaign_SuspendsUserAtThreshold() {
        sampleUser.setRejectedCampaignCount(1);
        sampleUser.setRejectedDonationCount(1); // Total 2
        Campaign campaign = new Campaign();
        campaign.setCreatorId(userId.toString());
        RejectedCampaignEvent event = new RejectedCampaignEvent(this, campaign);

        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

        userActivityListener.handleRejectedCampaign(event);

        assertEquals(2, sampleUser.getRejectedCampaignCount());
        assertTrue(sampleUser.isSuspended()); // 2 + 1 = 3 -> Should suspend
        verify(userRepository).save(sampleUser);
    }
}
