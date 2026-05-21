package id.ac.ui.cs.advprog.donatjs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.donatjs.dto.DisputeDTO;
import id.ac.ui.cs.advprog.donatjs.dto.DisputeResolutionRequest;
import id.ac.ui.cs.advprog.donatjs.dto.DisputeSubmissionRequest;
import id.ac.ui.cs.advprog.donatjs.service.CurrentUserService;
import id.ac.ui.cs.advprog.donatjs.service.DisputeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
public class DisputeControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DisputeService disputeService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private DisputeController disputeController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(disputeController).build();
    }

    @Test
    void testSubmitDispute() throws Exception {
        UUID userId = UUID.randomUUID();
        DisputeSubmissionRequest req = new DisputeSubmissionRequest();
        req.setReason("Account suspended by mistake");

        DisputeDTO dto = new DisputeDTO();
        dto.setId(UUID.randomUUID());
        dto.setUserId(userId);
        dto.setReason("Account suspended by mistake");
        dto.setStatus("PENDING");

        when(currentUserService.getCurrentUserId(any())).thenReturn(userId.toString());
        when(disputeService.submitDispute(eq(userId), eq("Account suspended by mistake"))).thenReturn(dto);

        mockMvc.perform(post("/api/disputes/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.reason").value("Account suspended by mistake"));
    }

    @Test
    void testGetMyDisputes() throws Exception {
        UUID userId = UUID.randomUUID();
        DisputeDTO dto = new DisputeDTO();
        dto.setId(UUID.randomUUID());

        when(currentUserService.getCurrentUserId(any())).thenReturn(userId.toString());
        when(disputeService.getDisputesByUser(userId)).thenReturn(Collections.singletonList(dto));

        mockMvc.perform(get("/api/disputes/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void testGetAllPendingDisputes() throws Exception {
        DisputeDTO dto = new DisputeDTO();
        dto.setId(UUID.randomUUID());

        when(disputeService.getAllPendingDisputes()).thenReturn(Collections.singletonList(dto));

        mockMvc.perform(get("/api/disputes/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void testResolveDispute() throws Exception {
        UUID disputeId = UUID.randomUUID();
        DisputeResolutionRequest req = new DisputeResolutionRequest();
        req.setApprove(true);
        req.setAdminNotes("Approved");

        DisputeDTO dto = new DisputeDTO();
        dto.setId(disputeId);
        dto.setStatus("APPROVED");

        when(disputeService.resolveDispute(eq(disputeId), eq(true), eq("Approved"))).thenReturn(dto);

        mockMvc.perform(post("/api/disputes/" + disputeId + "/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }
}
