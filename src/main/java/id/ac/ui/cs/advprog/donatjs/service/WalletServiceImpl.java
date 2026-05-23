package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.exception.InsufficientBalanceException;
import id.ac.ui.cs.advprog.donatjs.model.Transaction;
import id.ac.ui.cs.advprog.donatjs.model.TransactionType;
import id.ac.ui.cs.advprog.donatjs.model.Wallet;
import id.ac.ui.cs.advprog.donatjs.repository.TransactionRepository;
import id.ac.ui.cs.advprog.donatjs.repository.WalletRepository;
import id.ac.ui.cs.advprog.donatjs.util.IdrMoney;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
    private final double walletInitialBalance;

    public WalletServiceImpl(WalletRepository walletRepository,
                             TransactionRepository transactionRepository,
                             @Value("${donatjs.wallet.initial-balance:0}") double walletInitialBalance) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.walletInitialBalance = walletInitialBalance;
    }

    @Override
    @Transactional
    @SuppressWarnings("null")
    public Wallet getWalletByUserId(String userId) {
        return walletRepository.findByUserId(userId).orElseGet(() -> {
            long opening = Math.max(0L, IdrMoney.wholeRupiah(walletInitialBalance));
            log.info("Auto-provisioning wallet for userId={} with opening balance Rp {}", userId, opening);
            Wallet wallet = walletRepository.save(Wallet.builder()
                    .userId(userId)
                    .balance(IdrMoney.asDouble(opening))
                    .build());
            transactionRepository.save(Transaction.builder()
                    .wallet(wallet)
                    .amount(IdrMoney.asDouble(opening))
                    .type(TransactionType.DEPOSIT)
                    .description(opening > 0 ? "Opening balance (configured)" : "Wallet created")
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
    @SuppressWarnings("null")
    public Wallet topUp(String userId, double amount, String description) {
        long amt = IdrMoney.wholeRupiah(amount);
        if (amt <= 0) {
            throw new IllegalArgumentException("Top up amount must be a positive whole rupiah amount.");
        }
        Wallet wallet = walletRepository.findByUserIdForWrite(userId)
                .orElseGet(() -> getWalletByUserId(userId));
        long bal = IdrMoney.wholeRupiah(wallet.getBalance());
        long newBal = Math.addExact(bal, amt);
        wallet.setBalance(IdrMoney.asDouble(newBal));
        walletRepository.save(wallet);

        transactionRepository.save(Transaction.builder()
                .wallet(wallet)
                .amount(IdrMoney.asDouble(amt))
                .type(TransactionType.DEPOSIT)
                .description(description != null && !description.isBlank()
                        ? description
                        : "Wallet top up")
                .timestamp(LocalDateTime.now())
                .build());

        log.info("[DEMO] Wallet top up of Rp {} processed for user {}. New balance: Rp {}",
                amt, userId, newBal);
        return wallet;
    }

    @Override
    @Transactional
    @SuppressWarnings("null")
    public Wallet deductBalance(String userId, double amount, String description) {
        long amt = IdrMoney.wholeRupiah(amount);
        if (amt <= 0) {
            throw new IllegalArgumentException("Deduction amount must be a positive whole rupiah amount.");
        }
        Wallet wallet = walletRepository.findByUserIdForWrite(userId)
                .orElseThrow(() -> new IllegalStateException("Insufficient balance"));
        long bal = IdrMoney.wholeRupiah(wallet.getBalance());
        if (bal < amt) {
            throw new IllegalStateException("Insufficient balance");
        }
        long newBal = bal - amt;
        wallet.setBalance(IdrMoney.asDouble(newBal));
        walletRepository.save(wallet);

        transactionRepository.save(Transaction.builder()
                .wallet(wallet)
                .amount(IdrMoney.asDouble(amt))
                .type(TransactionType.SUBSCRIPTION)
                .description(description != null && !description.isBlank() ? description : "Subscription debit")
                .timestamp(LocalDateTime.now())
                .build());

        return wallet;
    }

    @Override
    @Transactional
    @SuppressWarnings("null")
    public Wallet withdraw(String userId, double amount, String description) {
        long amt = IdrMoney.wholeRupiah(amount);
        if (amt <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be a positive whole rupiah amount.");
        }
        Wallet wallet = walletRepository.findByUserIdForWrite(userId)
                .orElseThrow(() -> new InsufficientBalanceException("No wallet found for user."));
        long bal = IdrMoney.wholeRupiah(wallet.getBalance());
        if (bal < amt) {
            NumberFormat nf = NumberFormat.getIntegerInstance(Locale.of("id", "ID"));
            throw new InsufficientBalanceException(
                "Insufficient balance. Available: Rp " + nf.format(bal)
                + ", Requested: Rp " + nf.format(amt));
        }
        long newBal = bal - amt;
        wallet.setBalance(IdrMoney.asDouble(newBal));
        walletRepository.save(wallet);

        transactionRepository.save(Transaction.builder()
                .wallet(wallet)
                .amount(IdrMoney.asDouble(amt))
                .type(TransactionType.WITHDRAWAL)
                .description(description != null && !description.isBlank() ? description : "Withdrawal")
                .timestamp(LocalDateTime.now())
                .build());

        log.info("[NOTIFICATION] Withdrawal of Rp {} processed for user {}. New balance: Rp {}",
                amt, userId, newBal);
        return wallet;
    }

    @Override
    @Transactional
    @SuppressWarnings("null")
    public Wallet deductForDonation(String userId, double amount, String campaignName) {
        long amt = IdrMoney.wholeRupiah(amount);
        if (amt <= 0) {
            throw new IllegalArgumentException("Donation amount must be a positive whole rupiah amount.");
        }
        Wallet wallet = walletRepository.findByUserIdForWrite(userId)
                .orElseThrow(() -> new InsufficientBalanceException("No wallet found for user."));
        long bal = IdrMoney.wholeRupiah(wallet.getBalance());
        if (bal < amt) {
            NumberFormat nf = NumberFormat.getIntegerInstance(Locale.of("id", "ID"));
            throw new InsufficientBalanceException(
                "Insufficient balance for donation. Available: Rp " + nf.format(bal)
                + ", Required: Rp " + nf.format(amt));
        }
        long newBal = bal - amt;
        wallet.setBalance(IdrMoney.asDouble(newBal));
        walletRepository.save(wallet);

        transactionRepository.save(Transaction.builder()
                .wallet(wallet)
                .amount(IdrMoney.asDouble(amt))
                .type(TransactionType.DONATION)
                .description("Donation to: " + campaignName)
                .timestamp(LocalDateTime.now())
                .build());

        log.info("[NOTIFICATION] Donation of Rp {} deducted for user {} to campaign '{}'. New balance: Rp {}",
                amt, userId, campaignName, newBal);
        return wallet;
    }
}
