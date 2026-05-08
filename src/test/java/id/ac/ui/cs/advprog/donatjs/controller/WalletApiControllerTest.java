package id.ac.ui.cs.advprog.donatjs.controller;

import id.ac.ui.cs.advprog.donatjs.config.SecurityConfig;
import id.ac.ui.cs.advprog.donatjs.exception.InsufficientBalanceException;
import id.ac.ui.cs.advprog.donatjs.model.Wallet;
import id.ac.ui.cs.advprog.donatjs.repository.UserRepository;
import id.ac.ui.cs.advprog.donatjs.service.CurrentUserService;
import id.ac.ui.cs.advprog.donatjs.service.WalletService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalletApiController.class)
@Import(SecurityConfig.class)
@WithMockUser
@SuppressWarnings("null")
class WalletApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WalletService walletService;

    @MockitoBean
    private CurrentUserService currentUserService;

    @MockitoBean
    private UserRepository userRepository;

    // ── POST /api/internal/wallet/deduct ──────────────────────────────────────

    @Test
    void deduct_success_returns200WithNewBalance() throws Exception {
        Wallet updated = Wallet.builder().userId("user-001").balance(500000.0).build();
        when(walletService.deductForDonation("user-001", 100000.0, "Build a School"))
                .thenReturn(updated);

        mockMvc.perform(post("/api/internal/wallet/deduct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"user-001","amount":100000,"campaignName":"Build a School"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.newBalance").value(500000.0));
    }

    @Test
    void deduct_missingUserId_returns400() throws Exception {
        mockMvc.perform(post("/api/internal/wallet/deduct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":100000}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void deduct_missingAmount_returns400() throws Exception {
        mockMvc.perform(post("/api/internal/wallet/deduct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"user-001"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void deduct_insufficientBalance_returns422() throws Exception {
        when(walletService.deductForDonation(anyString(), anyDouble(), anyString()))
                .thenThrow(new InsufficientBalanceException("Insufficient balance"));

        mockMvc.perform(post("/api/internal/wallet/deduct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"user-001","amount":9999999,"campaignName":"Expensive"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void deduct_illegalArgument_returns400() throws Exception {
        when(walletService.deductForDonation(anyString(), anyDouble(), anyString()))
                .thenThrow(new IllegalArgumentException("amount must be positive"));

        mockMvc.perform(post("/api/internal/wallet/deduct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"user-001","amount":-100,"campaignName":"Bad"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void deduct_walletNotFound_returns404() throws Exception {
        when(walletService.deductForDonation(anyString(), anyDouble(), anyString()))
                .thenThrow(new RuntimeException("Wallet not found"));

        mockMvc.perform(post("/api/internal/wallet/deduct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"unknown","amount":50000,"campaignName":"X"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── GET /api/internal/wallet/balance/{userId} ─────────────────────────────

    @Test
    void getBalance_found_returns200WithBalance() throws Exception {
        Wallet wallet = Wallet.builder().id("w-1").userId("user-001").balance(750000.0).build();
        when(walletService.getWalletByUserId("user-001")).thenReturn(wallet);

        mockMvc.perform(get("/api/internal/wallet/balance/user-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.balance").value(750000.0))
                .andExpect(jsonPath("$.userId").value("user-001"));
    }

    @Test
    void getBalance_notFound_returns404() throws Exception {
        when(walletService.getWalletByUserId("unknown"))
                .thenThrow(new RuntimeException("Wallet not found"));

        mockMvc.perform(get("/api/internal/wallet/balance/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
