package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.dto.UpdateProfileRequest;
import id.ac.ui.cs.advprog.donatjs.dto.UserProfileDTO;
import id.ac.ui.cs.advprog.donatjs.event.ProfileUpdatedEvent;
import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import id.ac.ui.cs.advprog.donatjs.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CampaignService campaignService;

    @Mock
    private DonationService donationService;

    @Mock
    private SavedCampaignService savedCampaignService;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ProfileService profileService;

    private AppUser sampleUser;
    private final String testEmail = "aldebaran@ui.ac.id";
    private final UUID testId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        sampleUser = new AppUser();
        sampleUser.setId(testId);
        sampleUser.setEmail(testEmail);
        sampleUser.setName("Aldebaran");
        sampleUser.setBio("Initial Bio");
    }

    @Test
    void testGetUserProfile_Success_AggregatesActivities() {
        // Arrange
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(sampleUser));
        when(campaignService.findByCreatorId(anyString())).thenReturn(new ArrayList<>());
        when(donationService.getDonationsByUser(anyString())).thenReturn(new ArrayList<>());
        when(savedCampaignService.getSavedCampaigns(anyString())).thenReturn(new ArrayList<>());
        when(subscriptionService.getSubscriptionsByUser(anyString())).thenReturn(new ArrayList<>());

        // Act
        UserProfileDTO result = profileService.getUserProfile(testEmail);

        // Assert
        assertNotNull(result);
        assertEquals("Aldebaran", result.getName());
        assertNotNull(result.getCreatedCampaigns());
        assertNotNull(result.getDonations());
        verify(campaignService).findByCreatorId(testId.toString());
        verify(donationService).getDonationsByUser(testId.toString());
        verify(subscriptionService).getSubscriptionsByUser(testId.toString());
    }

    @Test
    void testUpdateUserProfile_Success_PublishesEvent() {
        // Arrange
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setName("New Name");
        request.setBio("New Bio");

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(AppUser.class))).thenReturn(sampleUser);
        
        // Success aggregation mocks for getUserProfile internal call
        when(campaignService.findByCreatorId(anyString())).thenReturn(new ArrayList<>());
        when(donationService.getDonationsByUser(anyString())).thenReturn(new ArrayList<>());
        when(savedCampaignService.getSavedCampaigns(anyString())).thenReturn(new ArrayList<>());
        when(subscriptionService.getSubscriptionsByUser(anyString())).thenReturn(new ArrayList<>());

        // Act
        UserProfileDTO result = profileService.updateUserProfile(testEmail, request);

        // Assert
        assertEquals("New Name", result.getName());
        verify(eventPublisher, times(1)).publishEvent(any(ProfileUpdatedEvent.class));
        verify(userRepository, times(1)).save(any(AppUser.class));
    }

    @Test
    void testGetUserProfile_UserNotFound_ThrowsException() {
        when(userRepository.findByEmail("wrong@email.com")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            profileService.getUserProfile("wrong@email.com");
        });
    }
    @Test
    void testGetUserProfile_IncludesRejectionMetrics() {
        // Arrange
        sampleUser.setRejectedDonationCount(2);
        sampleUser.setRejectedCampaignCount(1);
        sampleUser.setSuspended(true);

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(sampleUser));
        when(campaignService.findByCreatorId(anyString())).thenReturn(new ArrayList<>());
        when(donationService.getDonationsByUser(anyString())).thenReturn(new ArrayList<>());
        when(savedCampaignService.getSavedCampaigns(anyString())).thenReturn(new ArrayList<>());
        when(subscriptionService.getSubscriptionsByUser(anyString())).thenReturn(new ArrayList<>());

        // Act
        UserProfileDTO result = profileService.getUserProfile(testEmail);

        // Assert
        assertEquals(2, result.getRejectedDonationCount());
        assertEquals(1, result.getRejectedCampaignCount());
        assertTrue(result.isSuspended());
    }
}