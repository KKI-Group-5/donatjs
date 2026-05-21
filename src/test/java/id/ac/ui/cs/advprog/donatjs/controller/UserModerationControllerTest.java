package id.ac.ui.cs.advprog.donatjs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.donatjs.dto.FraudActivityReportRequest;
import id.ac.ui.cs.advprog.donatjs.model.AdminNotification;
import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import id.ac.ui.cs.advprog.donatjs.service.UserModerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
public class UserModerationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserModerationService moderationService;

    @InjectMocks
    private UserModerationController userModerationController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userModerationController).build();
    }

    @Test
    void reportFraudActivity() throws Exception {
        FraudActivityReportRequest req = new FraudActivityReportRequest();
        req.setUserEmail("test@test.com");
        req.setReason("Scam");

        mockMvc.perform(post("/api/users/report-fraud-activity")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        verify(moderationService, times(1)).reportFraudActivity(eq("test@test.com"), eq("Scam"));
    }

    @Test
    @WithMockUser
    void getSuspendedUsers() throws Exception {
        when(moderationService.getSuspendedUsers()).thenReturn(Arrays.asList(new AppUser()));

        mockMvc.perform(get("/api/admin/users/suspended"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser
    void getAdminNotifications() throws Exception {
        when(moderationService.getAdminNotifications()).thenReturn(Arrays.asList(new AdminNotification()));

        mockMvc.perform(get("/api/admin/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser
    void getUnreadNotifications() throws Exception {
        when(moderationService.getUnreadAdminNotifications()).thenReturn(Arrays.asList(new AdminNotification()));

        mockMvc.perform(get("/api/admin/notifications/unread"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser
    void markNotificationRead() throws Exception {
        mockMvc.perform(patch("/api/admin/notifications/1/read")
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(moderationService, times(1)).markNotificationRead(1L);
    }
}
