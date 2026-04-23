package id.ac.ui.cs.advprog.donatjs.controller;

import id.ac.ui.cs.advprog.donatjs.service.SavedCampaignService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.Model;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PageControllerTest {

    @Mock
    private SavedCampaignService savedCampaignService;

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
    void testProfilePage() {
        String viewName = pageController.profilePage();
        assertEquals("profile", viewName);
    }
}
