package id.ac.ui.cs.advprog.donatjs.controller;

import id.ac.ui.cs.advprog.donatjs.exception.InsufficientBalanceException;
import id.ac.ui.cs.advprog.donatjs.model.Wallet;
import id.ac.ui.cs.advprog.donatjs.service.CurrentUserService;
import id.ac.ui.cs.advprog.donatjs.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
class WalletControllerTest {

    private static final String TEST_USER_ID = "user-demo-001";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WalletService walletService;

    @MockitoBean
    private CurrentUserService currentUserService;

    @BeforeEach
    void stubCurrentUser() {
        when(currentUserService.getCurrentUserId(any())).thenReturn(TEST_USER_ID);
        when(currentUserService.requireCurrentUserId()).thenReturn(TEST_USER_ID);
    }

    private Wallet demoWallet() {
        return Wallet.builder().id("w-demo").userId(TEST_USER_ID).balance(1500000.0).build();
    }

    @Test
    void getDashboard_returnsWalletView() throws Exception {
        when(walletService.getWalletByUserId(TEST_USER_ID)).thenReturn(demoWallet());
        when(walletService.getTransactionHistory("w-demo")).thenReturn(List.of());

        mockMvc.perform(get("/wallet"))
                .andExpect(status().isOk())
                .andExpect(view().name("wallet"))
                .andExpect(model().attributeExists("wallet"))
                .andExpect(model().attributeExists("transactions"));
    }

    @Test
    void withdraw_success_redirectsWithSuccessMessage() throws Exception {
        when(walletService.withdraw(TEST_USER_ID, 200000.0, "ATM"))
                .thenReturn(demoWallet());

        mockMvc.perform(post("/wallet/withdraw")
                        .with(csrf())
                        .param("amount", "200000")
                        .param("description", "ATM"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/wallet"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    void topUp_success_redirectsWithSuccessMessage() throws Exception {
        when(walletService.topUp(TEST_USER_ID, 250000.0, ""))
                .thenReturn(Wallet.builder().id("w-demo").userId(TEST_USER_ID).balance(1750000.0).build());

        mockMvc.perform(post("/wallet/top-up")
                        .with(csrf())
                        .param("amount", "250000")
                        .param("description", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/wallet"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(walletService).topUp(TEST_USER_ID, 250000.0, "");
    }

    @Test
    void topUp_illegalAmount_redirectsWithErrorMessage() throws Exception {
        when(walletService.topUp(eq(TEST_USER_ID), anyDouble(), anyString()))
                .thenThrow(new IllegalArgumentException("Top up amount must be positive."));

        mockMvc.perform(post("/wallet/top-up")
                        .with(csrf())
                        .param("amount", "0")
                        .param("description", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/wallet"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    void withdraw_insufficientBalance_redirectsWithErrorMessage() throws Exception {
        when(walletService.withdraw(eq(TEST_USER_ID), anyDouble(), anyString()))
                .thenThrow(new InsufficientBalanceException("Insufficient balance"));

        mockMvc.perform(post("/wallet/withdraw")
                        .with(csrf())
                        .param("amount", "9999999")
                        .param("description", "Too much"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/wallet"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    void withdraw_illegalAmount_redirectsWithErrorMessage() throws Exception {
        when(walletService.withdraw(eq(TEST_USER_ID), anyDouble(), anyString()))
                .thenThrow(new IllegalArgumentException("Withdrawal amount must be positive."));

        mockMvc.perform(post("/wallet/withdraw")
                        .with(csrf())
                        .param("amount", "-100")
                        .param("description", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/wallet"))
                .andExpect(flash().attributeExists("errorMessage"));
    }
}
