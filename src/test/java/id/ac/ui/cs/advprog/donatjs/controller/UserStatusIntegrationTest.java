package id.ac.ui.cs.advprog.donatjs.controller;

import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import id.ac.ui.cs.advprog.donatjs.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public @SuppressWarnings("null")
class UserStatusIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    private AppUser suspendedUser;
    private AppUser incompleteUser;

    @BeforeEach
    void setUp() {
        suspendedUser = new AppUser();
        suspendedUser.setEmail("suspended@test.com");
        suspendedUser.setSuspended(true);

        incompleteUser = new AppUser();
        incompleteUser.setEmail("incomplete@test.com");
        incompleteUser.setBio(null); // Incomplete
    }

    @Test
    void testAction_BlockedWhenSuspended() throws Exception {
        when(userRepository.findByEmail("suspended@test.com")).thenReturn(Optional.of(suspendedUser));

        mockMvc.perform(post("/api/donations")
                        .with(oauth2Login().attributes(attrs -> attrs.put("email", "suspended@test.com")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAction_BlockedWhenProfileIncomplete() throws Exception {
        when(userRepository.findByEmail("incomplete@test.com")).thenReturn(Optional.of(incompleteUser));

        mockMvc.perform(post("/api/donations")
                        .with(oauth2Login().attributes(attrs -> attrs.put("email", "incomplete@test.com")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAction_AllowedWhenProfileComplete() throws Exception {
        AppUser completeUser = new AppUser();
        completeUser.setEmail("complete@test.com");
        completeUser.setBio("Detailed Bio");
        completeUser.setDateOfBirth(LocalDate.of(1990, 1, 1));
        completeUser.setSuspended(false);

        when(userRepository.findByEmail("complete@test.com")).thenReturn(Optional.of(completeUser));
        
        // This should pass the interceptor (status will be 4xx but not 403 Forbidden from interceptor)
        mockMvc.perform(post("/api/donations")
                        .with(oauth2Login().attributes(attrs -> attrs.put("email", "complete@test.com")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"campaignId\": 1, \"amount\": 1000}"))
                .andExpect(result -> {
                    if (result.getResponse().getStatus() == 403) {
                        throw new AssertionError("Request was blocked by interceptor unexpectedly");
                    }
                });
    }
}
