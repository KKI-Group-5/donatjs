package id.ac.ui.cs.advprog.donatjs.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SecurityRedirectTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGuestAccess_CanListCampaigns() throws Exception {
        mockMvc.perform(get("/campaigns"))
                .andExpect(status().isOk());
    }

    @Test
    void testGuestAccess_CannotCreateCampaign_RedirectsToLogin() throws Exception {
        // Accessing the creation page as guest should redirect to login
        mockMvc.perform(get("/campaigns/create"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "http://localhost/login"));
    }

    @Test
    void testGuestAccess_CannotPostDonation_RedirectsToLogin() throws Exception {
        // POST to donations should be protected and redirect to login
        mockMvc.perform(post("/api/donations"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "http://localhost/login"));
    }
}
