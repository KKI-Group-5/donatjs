package id.ac.ui.cs.advprog.donatjs.controller;

import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import id.ac.ui.cs.advprog.donatjs.model.Campaign;
import id.ac.ui.cs.advprog.donatjs.repository.UserRepository;
import id.ac.ui.cs.advprog.donatjs.service.CampaignService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@SuppressWarnings("null")
public class EndToEndModerationTest {

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private UserRepository userRepository;

    private AppUser testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        testUser = new AppUser();
        testUser.setName("Test Moderation User");
        testUser.setEmail("modtest@example.com");
        testUser.setPassword("password");
        testUser = userRepository.save(testUser);
    }

    @Test
    void testThreeCampaignRejectionsFlagUser() throws InterruptedException {
        // Create 3 campaigns for the user
        for (int i = 0; i < 3; i++) {
            Campaign c = new Campaign();
            c.setTitle("Test Campaign " + i);
            c.setDescription("Description " + i);
            c.setTargetAmount(new BigDecimal("1000"));
            c.setDeadline(LocalDate.now().plusDays(10));
            campaignService.createCampaign(c, testUser.getId().toString());
            
            // Reject the campaign
            campaignService.moderateCampaign(c.getId(), false);
            
            // Sleep to avoid concurrent lost updates in async listener
            Thread.sleep(50);
        }

        // Wait a short moment for async event listener to finish
        Thread.sleep(100);

        // Fetch user from DB and verify
        AppUser updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(3, updatedUser.getRejectedCampaignCount());
        assertTrue(updatedUser.isFlaggedForReview(), "User should be flagged after 3 rejections");
    }
}
