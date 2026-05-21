package id.ac.ui.cs.advprog.donatjs.controller;

import id.ac.ui.cs.advprog.donatjs.service.SavedCampaignService;
import id.ac.ui.cs.advprog.donatjs.repository.UserRepository;
import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import id.ac.ui.cs.advprog.donatjs.dto.UserProfileDTO;
import id.ac.ui.cs.advprog.donatjs.service.CurrentUserService;
import id.ac.ui.cs.advprog.donatjs.service.ProfileService;
import id.ac.ui.cs.advprog.donatjs.service.SubscriptionService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
class PageControllerTest {

    @Mock
    private SavedCampaignService savedCampaignService;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private ProfileService profileService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Model model;

    @InjectMocks
    private PageController pageController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSavedCampaignsPage() {
        String userId = "user123";
        when(savedCampaignService.getSavedCampaigns(anyString())).thenReturn(Collections.emptyList());

        String viewName = pageController.savedCampaignsPage(userId, model);

        assertEquals("saved-campaigns", viewName);
        verify(savedCampaignService).getSavedCampaigns(userId);
        verify(model).addAttribute(eq("savedCampaigns"), eq(Collections.emptyList()));
        verify(model).addAttribute("userId", userId);
    }

    @Test
    void testAdminDashboard() {
        AppUser flaggedUser = new AppUser();
        flaggedUser.setFlaggedForReview(true);
        AppUser normalUser = new AppUser();
        
        when(userRepository.findAll()).thenReturn(List.of(flaggedUser, normalUser));

        String viewName = pageController.adminDashboard(model);

        assertEquals("admin-dashboard", viewName);
        verify(userRepository).findAll();
        verify(model).addAttribute(eq("flaggedUsers"), anyList());
    }

    @Test
    void testMySavedCampaignsPage() {
        when(currentUserService.requireCurrentUserId()).thenReturn("user123");
        when(savedCampaignService.getSavedCampaigns(anyString())).thenReturn(Collections.emptyList());

        String viewName = pageController.mySavedCampaignsPage(model);

        assertEquals("saved-campaigns", viewName);
        verify(savedCampaignService).getSavedCampaigns("user123");
        verify(model).addAttribute("userId", "user123");
    }

    @Test
    void testSubscriptionsPage() {
        when(subscriptionService.getSubscriptionsByUser("user123")).thenReturn(Collections.emptyList());

        String viewName = pageController.subscriptionsPage("user123", model);

        assertEquals("subscriptions", viewName);
        verify(subscriptionService).getSubscriptionsByUser("user123");
        verify(model).addAttribute("userId", "user123");
        verify(model).addAttribute(eq("subscriptions"), eq(Collections.emptyList()));
    }

    @Test
    void testMySubscriptionsPage() {
        when(currentUserService.requireCurrentUserId()).thenReturn("user123");
        when(subscriptionService.getSubscriptionsByUser("user123")).thenReturn(Collections.emptyList());

        String viewName = pageController.mySubscriptionsPage(model);

        assertEquals("subscriptions", viewName);
        verify(subscriptionService).getSubscriptionsByUser("user123");
        verify(model).addAttribute("userId", "user123");
    }

    @Test
    void testProfileDashboard() {
        SecurityContext securityContext = org.mockito.Mockito.mock(SecurityContext.class);
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(currentUserService.getCurrentUserEmail(authentication)).thenReturn("test@test.com");
        when(profileService.getUserProfile("test@test.com")).thenReturn(new UserProfileDTO("", "", "", null));

        String viewName = pageController.profileDashboard(model);

        assertEquals("profile", viewName);
        verify(model).addAttribute(eq("profile"), org.mockito.ArgumentMatchers.any(UserProfileDTO.class));
    }

}
