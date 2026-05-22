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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class WalletServiceImplTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    private WalletServiceImpl walletService;

    private Wallet wallet;

    @BeforeEach
    void setUp() {
        walletService = new WalletServiceImpl(walletRepository, transactionRepository, 0.0);
        wallet = Wallet.builder()
                .id("wallet-1")
                .userId("user-001")
                .balance(1000000.0)
                .build();
    }

    // ── withdraw() ───────────────────────────────────────────────────────────

    @Test
    void withdraw_success_deductsBalanceAndSavesWithdrawalTransaction() {
        when(walletRepository.findByUserIdForWrite("user-001")).thenReturn(Optional.of(wallet));
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
        when(walletRepository.findByUserIdForWrite("user-001")).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        walletService.withdraw("user-001", 100000.0, "");

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        assertThat(txCaptor.getValue().getDescription()).isEqualTo("Withdrawal");
    }

    @Test
    void withdraw_exactBalance_succeeds_andBalanceBecomesZero() {
        when(walletRepository.findByUserIdForWrite("user-001")).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Wallet result = walletService.withdraw("user-001", 1000000.0, "Full withdrawal");

        assertThat(result.getBalance()).isEqualTo(0.0);
    }

    @Test
    void withdraw_insufficientBalance_throwsAndNothingSaved() {
        when(walletRepository.findByUserIdForWrite("user-001")).thenReturn(Optional.of(wallet));

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
        when(walletRepository.findByUserIdForWrite("user-001")).thenReturn(Optional.of(wallet));
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
        when(walletRepository.findByUserIdForWrite("user-001")).thenReturn(Optional.of(wallet));

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
        when(walletRepository.findByUserIdForWrite("user-001")).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> walletService.deductForDonation("user-001", 101.0, "Just over"))
                .isInstanceOf(InsufficientBalanceException.class);

        // Wallet balance must remain unchanged
        assertThat(wallet.getBalance()).isEqualTo(100.0);
    }

    // ── getWalletByUserId() ──────────────────────────────────────────────────

    @Test
    void getWalletByUserId_found_returnsWallet() {
        when(walletRepository.findByUserId("user-001")).thenReturn(Optional.of(wallet));

        Wallet result = walletService.getWalletByUserId("user-001");

        assertThat(result).isSameAs(wallet);
    }

    @Test
    void getWalletByUserId_notFound_autoProvisionsFreshWallet() {
        // When no wallet exists for a user we now auto-create one with zero
        // balance rather than throwing. This keeps wallet-related features
        // friction-free for newly registered users.
        when(walletRepository.findByUserId("no-one")).thenReturn(Optional.empty());
        Wallet provisioned = Wallet.builder().id("w-new").userId("no-one").balance(0.0).build();
        when(walletRepository.save(org.mockito.ArgumentMatchers.any(Wallet.class)))
                .thenReturn(provisioned);

        Wallet result = walletService.getWalletByUserId("no-one");

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo("no-one");
        assertThat(result.getBalance()).isZero();
    }

    // ── getTransactionHistory() ──────────────────────────────────────────────

    @Test
    void getTransactionHistory_returnsListFromRepository() {
        Transaction tx = Transaction.builder()
                .id("tx-1")
                .wallet(wallet)
                .amount(100000.0)
                .type(TransactionType.DEPOSIT)
                .description("Top up")
                .timestamp(java.time.LocalDateTime.now())
                .build();
        when(transactionRepository.findByWalletIdOrderByTimestampDesc("wallet-1"))
                .thenReturn(List.of(tx));

        List<Transaction> result = walletService.getTransactionHistory("wallet-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo(TransactionType.DEPOSIT);
    }

    @Test
    void getTransactionHistory_emptyWallet_returnsEmptyList() {
        when(transactionRepository.findByWalletIdOrderByTimestampDesc("wallet-1"))
                .thenReturn(List.of());

        List<Transaction> result = walletService.getTransactionHistory("wallet-1");

        assertThat(result).isEmpty();
    }

    // ── deductBalance() ──────────────────────────────────────────────────────

    @Test
    void deductBalance_success_deductsAndSavesSubscriptionTransaction() {
        when(walletRepository.findByUserIdForWrite("user-001")).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Wallet result = walletService.deductBalance("user-001", 300000.0, "Monthly sub");

        assertThat(result.getBalance()).isEqualTo(700000.0);

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        assertThat(txCaptor.getValue().getType()).isEqualTo(TransactionType.SUBSCRIPTION);
        assertThat(txCaptor.getValue().getDescription()).isEqualTo("Monthly sub");
    }

    @Test
    void deductBalance_blankDescription_usesDefault() {
        when(walletRepository.findByUserIdForWrite("user-001")).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        walletService.deductBalance("user-001", 100000.0, "");
    void deductBalance_success_deductsBalanceAndSavesSubscriptionTransaction() {
        when(walletRepository.findByUserIdForWrite("user-001")).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Wallet result = walletService.deductBalance("user-001", 50000.0, "Monthly Subscription");

        assertThat(result.getBalance()).isEqualTo(950000.0);

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        Transaction saved = txCaptor.getValue();
        assertThat(saved.getType()).isEqualTo(TransactionType.SUBSCRIPTION);
        assertThat(saved.getAmount()).isEqualTo(50000.0);
        assertThat(saved.getDescription()).isEqualTo("Monthly Subscription");
    }

    @Test
    void deductBalance_noDescription_usesDefaultDescription() {
        when(walletRepository.findByUserIdForWrite("user-001")).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        walletService.deductBalance("user-001", 50000.0, "");

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        assertThat(txCaptor.getValue().getDescription()).isEqualTo("Subscription debit");
    }

    @Test
    void deductBalance_nullDescription_usesDefault() {
        when(walletRepository.findByUserIdForWrite("user-001")).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        walletService.deductBalance("user-001", 100000.0, null);

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        assertThat(txCaptor.getValue().getDescription()).isEqualTo("Subscription debit");
    }

    @Test
    void deductBalance_insufficientBalance_throwsIllegalStateException() {
    void deductBalance_insufficientBalance_throwsAndNothingSaved() {
        when(walletRepository.findByUserIdForWrite("user-001")).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> walletService.deductBalance("user-001", 2000000.0, "Too much"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Insufficient balance");

        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void deductBalance_zeroAmount_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> walletService.deductBalance("user-001", 0, "Zero"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deductBalance_walletNotFound_throwsIllegalStateException() {
        when(walletRepository.findByUserIdForWrite("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.deductBalance("unknown", 100000.0, "sub"))
                .isInstanceOf(IllegalStateException.class);
    }

    // ── getWalletByUserId — opening balance branch ────────────────────────────

    @Test
    void getWalletByUserId_notFound_savesOpeningBalanceTransaction() {
        WalletServiceImpl serviceWithBalance = new WalletServiceImpl(walletRepository, transactionRepository, 50000.0);
        Wallet provisioned = Wallet.builder().id("w-new").userId("new-user").balance(50000.0).build();
        when(walletRepository.findByUserId("new-user")).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenReturn(provisioned);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Wallet result = serviceWithBalance.getWalletByUserId("new-user");

        assertThat(result.getBalance()).isEqualTo(50000.0);
        verify(transactionRepository).save(any(Transaction.class));
    }
}
