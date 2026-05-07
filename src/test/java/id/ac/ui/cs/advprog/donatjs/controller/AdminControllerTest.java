package id.ac.ui.cs.advprog.donatjs.controller;

import id.ac.ui.cs.advprog.donatjs.config.SecurityConfig;
import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import id.ac.ui.cs.advprog.donatjs.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
@SuppressWarnings("null")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @WithMockUser(roles = "USER")
    void getFlaggedUsers_AsUser_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/users/flagged"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getFlaggedUsers_AsAdmin_ReturnsList() throws Exception {
        AppUser flaggedUser = new AppUser();
        flaggedUser.setId(UUID.randomUUID());
        flaggedUser.setFlaggedForReview(true);

        when(userRepository.findAll()).thenReturn(List.of(flaggedUser));

        mockMvc.perform(get("/api/admin/users/flagged"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void suspendUser_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);
        user.setFlaggedForReview(true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/admin/users/" + userId + "/suspend").with(csrf()))
                .andExpect(status().isOk());

        verify(userRepository).save(argThat(u -> u.isSuspended() && !u.isFlaggedForReview()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void unsuspendUser_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);
        user.setSuspended(true);
        user.setFlaggedForReview(true);
        user.setRejectedCampaignCount(3);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/admin/users/" + userId + "/unsuspend").with(csrf()))
                .andExpect(status().isOk());

        verify(userRepository).save(argThat(u -> 
            !u.isSuspended() && 
            !u.isFlaggedForReview() && 
            u.getRejectedCampaignCount() == 0 &&
            u.getRejectedDonationCount() == 0
        ));
    }
}
