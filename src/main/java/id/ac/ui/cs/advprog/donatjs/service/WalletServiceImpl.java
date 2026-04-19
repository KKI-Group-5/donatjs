package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.exception.InsufficientBalanceException;
import id.ac.ui.cs.advprog.donatjs.model.Transaction;
import id.ac.ui.cs.advprog.donatjs.model.TransactionType;
import id.ac.ui.cs.advprog.donatjs.model.Wallet;
import id.ac.ui.cs.advprog.donatjs.repository.TransactionRepository;
import id.ac.ui.cs.advprog.donatjs.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class WalletServiceImpl implements WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletServiceImpl.class);

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public WalletServiceImpl(WalletRepository walletRepository, TransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional
    public Wallet getWalletByUserId(String userId) {
        return walletRepository.findByUserId(userId).orElseGet(() -> {
            log.info("Auto-provisioning wallet for userId={}", userId);
            Wallet wallet = walletRepository.save(Wallet.builder()
                    .userId(userId)
                    .balance(0.0)
                    .build());
            transactionRepository.save(Transaction.builder()
                    .wallet(wallet)
                    .amount(0.0)
                    .type(TransactionType.DEPOSIT)
                    .description("Wallet created")
                    .timestamp(LocalDateTime.now())
                    .build());
            return wallet;
        });
    }

    @Override
    public List<Transaction> getTransactionHistory(String walletId) {
        return transactionRepository.findByWalletIdOrderByTimestampDesc(walletId);
    }

    @Override
    @Transactional
    public Wallet deductBalance(String userId, double amount, String description) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deduction amount must be positive.");
        }
        Wallet wallet = getWalletByUserId(userId);
        if (wallet.getBalance() < amount) {
            throw new IllegalStateException("Insufficient balance");
        }
        wallet.setBalance(wallet.getBalance() - amount);
        walletRepository.save(wallet);

        transactionRepository.save(Transaction.builder()
                .wallet(wallet)
                .amount(amount)
                .type(TransactionType.SUBSCRIPTION)
                .description(description != null && !description.isBlank() ? description : "Subscription debit")
                .timestamp(LocalDateTime.now())
                .build());

        return wallet;
    }

    @Override
    @Transactional
    public Wallet withdraw(String userId, double amount, String description) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive.");
        }
        Wallet wallet = getWalletByUserId(userId);
        if (wallet.getBalance() < amount) {
            NumberFormat nf = NumberFormat.getIntegerInstance(Locale.of("id", "ID"));
            throw new InsufficientBalanceException(
                "Insufficient balance. Available: Rp " + nf.format((long) wallet.getBalance().doubleValue())
                + ", Requested: Rp " + nf.format((long) amount));
        }
        wallet.setBalance(wallet.getBalance() - amount);
        walletRepository.save(wallet);

        transactionRepository.save(Transaction.builder()
                .wallet(wallet)
                .amount(amount)
                .type(TransactionType.WITHDRAWAL)
                .description(description != null && !description.isBlank() ? description : "Withdrawal")
                .timestamp(LocalDateTime.now())
                .build());

        log.info("[NOTIFICATION] Withdrawal of Rp {} processed for user {}. New balance: Rp {}",
                amount, userId, wallet.getBalance());
        return wallet;
    }

    @Override
    @Transactional
    public Wallet deductForDonation(String userId, double amount, String campaignName) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Donation amount must be positive.");
        }
        Wallet wallet = getWalletByUserId(userId);
        if (wallet.getBalance() < amount) {
            NumberFormat nf = NumberFormat.getIntegerInstance(Locale.of("id", "ID"));
            throw new InsufficientBalanceException(
                "Insufficient balance for donation. Available: Rp " + nf.format((long) wallet.getBalance().doubleValue())
                + ", Required: Rp " + nf.format((long) amount));
        }
        wallet.setBalance(wallet.getBalance() - amount);
        walletRepository.save(wallet);

        transactionRepository.save(Transaction.builder()
                .wallet(wallet)
                .amount(amount)
                .type(TransactionType.DONATION)
                .description("Donation to: " + campaignName)
                .timestamp(LocalDateTime.now())
                .build());

        log.info("[NOTIFICATION] Donation of Rp {} deducted for user {} to campaign '{}'. New balance: Rp {}",
                amount, userId, campaignName, wallet.getBalance());
        return wallet;
    }
}