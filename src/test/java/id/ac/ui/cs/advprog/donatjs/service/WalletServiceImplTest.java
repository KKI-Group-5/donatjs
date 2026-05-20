package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.exception.InsufficientBalanceException;
import id.ac.ui.cs.advprog.donatjs.model.Donation;
import id.ac.ui.cs.advprog.donatjs.model.Transaction;
import id.ac.ui.cs.advprog.donatjs.model.TransactionType;
import id.ac.ui.cs.advprog.donatjs.model.Wallet;
import id.ac.ui.cs.advprog.donatjs.repository.DonationRepository;
import id.ac.ui.cs.advprog.donatjs.repository.TransactionRepository;
import id.ac.ui.cs.advprog.donatjs.repository.WalletRepository;
import java.util.List;
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

    @Mock
    private DonationRepository donationRepository;

    private WalletServiceImpl walletService;

    private Wallet wallet;

    @BeforeEach
    void setUp() {
        walletService = new WalletServiceImpl(walletRepository, transactionRepository, donationRepository, 0.0);
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

    // ── bulkRefundForCampaign() ──────────────────────────────────────────────

    @Test
    void bulkRefundForCampaign_walletDonationsRefunded_balanceCreditedAndStatusSet() {
        Donation d1 = new Donation();
        d1.setId(1L); d1.setUserId("user-001"); d1.setCampaignId(42L);
        d1.setAmount(100000L); d1.setPaymentMethod(Donation.PaymentMethod.WALLET);
        d1.setStatus(Donation.DonationStatus.SUCCESS);

        Donation d2 = new Donation();
        d2.setId(2L); d2.setUserId("user-002"); d2.setCampaignId(42L);
        d2.setAmount(200000L); d2.setPaymentMethod(Donation.PaymentMethod.WALLET);
        d2.setStatus(Donation.DonationStatus.SUCCESS);

        Wallet w2 = Wallet.builder().id("w2").userId("user-002").balance(50000.0).build();

        when(donationRepository.findByCampaignIdAndStatus(42L, Donation.DonationStatus.SUCCESS))
                .thenReturn(List.of(d1, d2));
        when(walletRepository.findByUserId("user-001")).thenReturn(Optional.of(wallet));
        when(walletRepository.findByUserId("user-002")).thenReturn(Optional.of(w2));
        when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int result = walletService.bulkRefundForCampaign(42L, "Help Build a School");

        assertThat(result).isEqualTo(2);
        assertThat(wallet.getBalance()).isEqualTo(1100000.0);
        assertThat(w2.getBalance()).isEqualTo(250000.0);
        assertThat(d1.getStatus()).isEqualTo(Donation.DonationStatus.REFUNDED);
        assertThat(d2.getStatus()).isEqualTo(Donation.DonationStatus.REFUNDED);

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(txCaptor.capture());
        assertThat(txCaptor.getAllValues())
                .allMatch(tx -> tx.getType() == TransactionType.REFUND)
                .allMatch(tx -> tx.getDescription().equals("Refund: Help Build a School"));
    }

    @Test
    void bulkRefundForCampaign_nonWalletDonationsSkipped_onlyWalletIsRefunded() {
        Donation walletDonation = new Donation();
        walletDonation.setId(1L); walletDonation.setUserId("user-001"); walletDonation.setCampaignId(42L);
        walletDonation.setAmount(100000L); walletDonation.setPaymentMethod(Donation.PaymentMethod.WALLET);
        walletDonation.setStatus(Donation.DonationStatus.SUCCESS);

        Donation gopayDonation = new Donation();
        gopayDonation.setId(2L); gopayDonation.setUserId("user-002"); gopayDonation.setCampaignId(42L);
        gopayDonation.setAmount(50000L); gopayDonation.setPaymentMethod(Donation.PaymentMethod.GOPAY);
        gopayDonation.setStatus(Donation.DonationStatus.SUCCESS);

        when(donationRepository.findByCampaignIdAndStatus(42L, Donation.DonationStatus.SUCCESS))
                .thenReturn(List.of(walletDonation, gopayDonation));
        when(walletRepository.findByUserId("user-001")).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int result = walletService.bulkRefundForCampaign(42L, "Test Campaign");

        assertThat(result).isEqualTo(1);
        verify(walletRepository, never()).findByUserId("user-002");
        verify(transactionRepository, times(1)).save(any());
        verify(donationRepository, times(1)).save(walletDonation);
        verify(donationRepository, never()).save(gopayDonation);
    }

    @Test
    void bulkRefundForCampaign_noSuccessDonations_returnsZero() {
        when(donationRepository.findByCampaignIdAndStatus(99L, Donation.DonationStatus.SUCCESS))
                .thenReturn(List.of());

        int result = walletService.bulkRefundForCampaign(99L, "Empty Campaign");

        assertThat(result).isEqualTo(0);
        verify(walletRepository, never()).findByUserId(any());
        verify(transactionRepository, never()).save(any());
        verify(donationRepository, never()).save(any(Donation.class));
    }
}
