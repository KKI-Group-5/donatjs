package id.ac.ui.cs.advprog.donatjs.controller;

import id.ac.ui.cs.advprog.donatjs.model.Campaign;
import id.ac.ui.cs.advprog.donatjs.service.CampaignService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class LandingControllerTest {

    @Mock
    private CampaignService campaignService;

    @Mock
    private Model model;

    @InjectMocks
    private LandingController landingController;


    @Test
    void landingPage() {
        Campaign c1 = new Campaign();
        Campaign c2 = new Campaign();
        when(campaignService.findOpenCampaigns()).thenReturn(Arrays.asList(c1, c2));

        String view = landingController.landingPage(model);

        assertEquals("index", view);
        verify(model).addAttribute(eq("campaigns"), anyList());
    }
}
