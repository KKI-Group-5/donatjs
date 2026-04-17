package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.exception.InsufficientBalanceException;
import id.ac.ui.cs.advprog.donatjs.model.Transaction;
import id.ac.ui.cs.advprog.donatjs.model.TransactionType;
import id.ac.ui.cs.advprog.donatjs.model.Wallet;
import id.ac.ui.cs.advprog.donatjs.repository.TransactionRepository;
import id.ac.ui.cs.advprog.donatjs.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private WalletServiceImpl walletService;

    private Wallet wallet;

    @BeforeEach
    void setUp() {
        wallet = Wallet.builder()
                .id("wallet-1")
                .userId("user-001")
                .balance(1000000.0)
                .build();
    }

    // ── withdraw() ───────────────────────────────────────────────────────────

    @Test
    void withdraw_success_deductsBalanceAndSavesWithdrawalTransaction() {
        when(walletRepository.findByUserId("user-001")).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Wallet result = walletService.withdraw("user-001", 200000.0, "ATM Withdrawal");

        assertThat(result.getBalance()).isEqualTo(800000.0);

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        Transaction saved = txCaptor.getValue();
        assertThat(saved.getType()).isEqualTo(TransactionType.WITHDRAWAL);
        assertThat(saved.getAmount()).isEqualTo(200000.0);
        assertThat(saved.getDescription()).isEqualTo("ATM Withdrawal");
    }

    @Test
    void withdraw_noDescription_usesDefaultDescription() {
        when(walletRepository.findByUserId("user-001")).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        walletService.withdraw("user-001", 100000.0, "");

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        assertThat(txCaptor.getValue().getDescription()).isEqualTo("Withdrawal");
    }

    @Test
    void withdraw_exactBalance_succeeds_andBalanceBecomesZero() {
        when(walletRepository.findByUserId("user-001")).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Wallet result = walletService.withdraw("user-001", 1000000.0, "Full withdrawal");

        assertThat(result.getBalance()).isEqualTo(0.0);
    }

    @Test
    void withdraw_insufficientBalance_throwsAndNothingSaved() {
        when(walletRepository.findByUserId("user-001")).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> walletService.withdraw("user-001", 2000000.0, "Too much"))
                .isInstanceOf(InsufficientBalanceException.class);

        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void withdraw_zeroAmount_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> walletService.withdraw("user-001", 0, "Zero"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void withdraw_negativeAmount_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> walletService.withdraw("user-001", -500, "Negative"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── deductForDonation() ──────────────────────────────────────────────────

    @Test
    void deductForDonation_success_deductsBalanceAndSavesDonationTransaction() {
        when(walletRepository.findByUserId("user-001")).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Wallet result = walletService.deductForDonation("user-001", 500000.0, "Build a School");

        assertThat(result.getBalance()).isEqualTo(500000.0);

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        Transaction saved = txCaptor.getValue();
        assertThat(saved.getType()).isEqualTo(TransactionType.DONATION);
        assertThat(saved.getAmount()).isEqualTo(500000.0);
        assertThat(saved.getDescription()).isEqualTo("Donation to: Build a School");
    }

    @Test
    void deductForDonation_insufficientBalance_throwsAndNothingSaved() {
        when(walletRepository.findByUserId("user-001")).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> walletService.deductForDonation("user-001", 5000000.0, "Big Campaign"))
                .isInstanceOf(InsufficientBalanceException.class);

        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void deductForDonation_zeroAmount_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> walletService.deductForDonation("user-001", 0, "Zero"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void deductForDonation_negativeAmount_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> walletService.deductForDonation("user-001", -100, "Negative"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deductForDonation_balanceNeverGoesNegative() {
        wallet.setBalance(100.0);
        when(walletRepository.findByUserId("user-001")).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> walletService.deductForDonation("user-001", 101.0, "Just over"))
                .isInstanceOf(InsufficientBalanceException.class);

        // Wallet balance must remain unchanged
        assertThat(wallet.getBalance()).isEqualTo(100.0);
    }
}
