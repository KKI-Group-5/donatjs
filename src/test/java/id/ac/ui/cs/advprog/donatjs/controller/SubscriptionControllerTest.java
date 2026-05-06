package id.ac.ui.cs.advprog.donatjs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.donatjs.dto.CreateSubscriptionRequest;
import id.ac.ui.cs.advprog.donatjs.dto.SubscriptionResponse;
import id.ac.ui.cs.advprog.donatjs.dto.UpdateSubscriptionRequest;
import id.ac.ui.cs.advprog.donatjs.model.Subscription.SubscriptionFrequency;
import id.ac.ui.cs.advprog.donatjs.model.Subscription.SubscriptionStatus;
import id.ac.ui.cs.advprog.donatjs.service.CurrentUserService;
import id.ac.ui.cs.advprog.donatjs.service.SubscriptionService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SubscriptionController.class)
@WithMockUser
@SuppressWarnings("null")
class SubscriptionControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private SubscriptionService subscriptionService;
    @MockitoBean private CurrentUserService currentUserService;
    @Autowired private ObjectMapper objectMapper;

    private static final String USER_ID     = "user-001";
    private static final Long   SUB_ID      = 1L;
    private static final Long   CAMPAIGN_ID = 1L;
    private static final Long   AMOUNT      = 50_000L;

    private SubscriptionResponse buildResponse(SubscriptionStatus status) {
        return SubscriptionResponse.builder()
                .id(SUB_ID).userId(USER_ID).campaignId(CAMPAIGN_ID)
                .amount(AMOUNT).frequency(SubscriptionFrequency.MONTHLY)
                .status(status)
                .nextDebitDate(LocalDate.now().plusMonths(1))
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createSubscription_returnsCreated() throws Exception {
        CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                .campaignId(CAMPAIGN_ID)
                .amount(AMOUNT).frequency(SubscriptionFrequency.MONTHLY)
                .build();

        when(currentUserService.getCurrentUserId(any())).thenReturn(USER_ID);
        when(subscriptionService.createSubscription(any())).thenReturn(buildResponse(SubscriptionStatus.ACTIVE));

        mockMvc.perform(post("/api/subscriptions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(USER_ID))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void createSubscription_duplicate_returnsConflict() throws Exception {
        CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                .campaignId(CAMPAIGN_ID)
                .amount(AMOUNT).frequency(SubscriptionFrequency.MONTHLY)
                .build();

        when(currentUserService.getCurrentUserId(any())).thenReturn(USER_ID);
        when(subscriptionService.createSubscription(any()))
                .thenThrow(new IllegalStateException("Active subscription already exists for this campaign"));

        mockMvc.perform(post("/api/subscriptions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Active subscription already exists for this campaign"));
    }

    @Test
    void createSubscription_insufficientBalance_returnsUnprocessable() throws Exception {
        CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                .campaignId(CAMPAIGN_ID)
                .amount(AMOUNT).frequency(SubscriptionFrequency.MONTHLY)
                .build();

        when(currentUserService.getCurrentUserId(any())).thenReturn(USER_ID);
        when(subscriptionService.createSubscription(any()))
                .thenThrow(new IllegalStateException("Insufficient balance"));

        mockMvc.perform(post("/api/subscriptions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Insufficient balance"));
    }

    @Test
    void cancelSubscription_returnsOk() throws Exception {
        when(currentUserService.getCurrentUserId(any())).thenReturn(USER_ID);
        when(subscriptionService.cancelSubscription(SUB_ID, USER_ID))
                .thenReturn(buildResponse(SubscriptionStatus.CANCELLED));

        mockMvc.perform(delete("/api/subscriptions/{id}", SUB_ID)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancelSubscription_notFound_returns404() throws Exception {
        when(currentUserService.getCurrentUserId(any())).thenReturn(USER_ID);
        when(subscriptionService.cancelSubscription(99L, USER_ID))
                .thenThrow(new EntityNotFoundException("Subscription not found: 99"));

        mockMvc.perform(delete("/api/subscriptions/{id}", 99L)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Subscription not found: 99"));
    }

    @Test
    void cancelSubscription_wrongUser_returnsForbidden() throws Exception {
        when(currentUserService.getCurrentUserId(any())).thenReturn(USER_ID);
        when(subscriptionService.cancelSubscription(SUB_ID, USER_ID))
                .thenThrow(new IllegalStateException("You do not own this subscription"));

        mockMvc.perform(delete("/api/subscriptions/{id}", SUB_ID)
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("You do not own this subscription"));
    }

    @Test
    void updateFrequency_returnsOk() throws Exception {
        UpdateSubscriptionRequest request = new UpdateSubscriptionRequest(SubscriptionFrequency.WEEKLY);

        SubscriptionResponse updated = buildResponse(SubscriptionStatus.ACTIVE);

        when(currentUserService.getCurrentUserId(any())).thenReturn(USER_ID);
        when(subscriptionService.updateFrequency(SUB_ID, USER_ID, SubscriptionFrequency.WEEKLY))
                .thenReturn(updated);

        mockMvc.perform(patch("/api/subscriptions/{id}/frequency", SUB_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID));
    }

    @Test
    void getSubscriptionsByUser_returnsList() throws Exception {
        when(currentUserService.getCurrentUserId(any())).thenReturn(USER_ID);
        when(subscriptionService.getSubscriptionsByUser(USER_ID))
                .thenReturn(List.of(buildResponse(SubscriptionStatus.ACTIVE)));

        mockMvc.perform(get("/api/subscriptions/user/{userId}", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(USER_ID));
    }

    @Test
    void getSubscriptionsByUser_otherUser_returnsForbidden() throws Exception {
        when(currentUserService.getCurrentUserId(any())).thenReturn(USER_ID);

        mockMvc.perform(get("/api/subscriptions/user/{userId}", "other-user"))
                .andExpect(status().isForbidden());
    }
}
