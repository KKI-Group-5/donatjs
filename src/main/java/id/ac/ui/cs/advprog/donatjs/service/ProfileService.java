package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.dto.UpdateProfileRequest;
import id.ac.ui.cs.advprog.donatjs.dto.UserProfileDTO;
import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import id.ac.ui.cs.advprog.donatjs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final CampaignService campaignService;
    private final DonationService donationService;
    private final SavedCampaignService savedCampaignService;
    private final SubscriptionService subscriptionService;
    private final UserActivityService userActivityService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public UserProfileDTO getUserProfile(String email) {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String userId = user.getId().toString();
        UserProfileDTO dto = new UserProfileDTO(user.getName(), user.getEmail(), user.getBio(), user.getDateOfBirth());
        
        dto.setCreatedCampaigns(campaignService.findByCreatorId(userId));
        dto.setDonations(donationService.getDonationsByUser(userId));
        dto.setSavedCampaigns(savedCampaignService.getSavedCampaigns(userId));
        dto.setSubscriptions(subscriptionService.getSubscriptionsByUser(userId));
        dto.setActivityUpdates(userActivityService.getUserActivities(userId));
        
        dto.setSuspended(user.isSuspended());
        dto.setFlaggedForReview(user.isFlaggedForReview());
        dto.setRejectedDonationCount(user.getRejectedDonationCount());
        dto.setRejectedCampaignCount(user.getRejectedCampaignCount());
        
        return dto;
    }

    @SuppressWarnings("null")
    public UserProfileDTO updateUserProfile(String email, UpdateProfileRequest request) {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getName() != null) user.setName(request.getName());
        if (request.getBio() != null) user.setBio(request.getBio());
        if (request.getDateOfBirth() != null) user.setDateOfBirth(request.getDateOfBirth());

        AppUser updatedUser = userRepository.save(user);

        // Milestone 3: Asynchronous communication
        eventPublisher.publishEvent(new id.ac.ui.cs.advprog.donatjs.event.ProfileUpdatedEvent(
                updatedUser.getId().toString(),
                updatedUser.getName(),
                updatedUser.getBio(),
                updatedUser.getDateOfBirth()
        ));

        // For the update response, we also aggregate activities
        return getUserProfile(updatedUser.getEmail());
    }
}