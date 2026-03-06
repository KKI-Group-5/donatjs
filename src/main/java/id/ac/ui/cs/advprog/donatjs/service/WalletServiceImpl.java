package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.model.Transaction;
import id.ac.ui.cs.advprog.donatjs.model.TransactionType;
import id.ac.ui.cs.advprog.donatjs.model.Wallet;
import id.ac.ui.cs.advprog.donatjs.repository.TransactionRepository;
import id.ac.ui.cs.advprog.donatjs.repository.WalletRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public WalletServiceImpl(WalletRepository walletRepository, TransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    // This creates our dummy integration slice data on startup
    @PostConstruct
    public void initDummyData() {
        if (walletRepository.findByUserId("user-demo-001").isEmpty()) {
            Wallet wallet = Wallet.builder()
                    .userId("user-demo-001")
                    .balance(1500000.0)
                    .build();
            walletRepository.save(wallet);

            transactionRepository.save(Transaction.builder()
                    .wallet(wallet)
                    .amount(2000000.0)
                    .type(TransactionType.DEPOSIT)
                    .description("Initial Top Up")
                    .timestamp(LocalDateTime.now().minusDays(2))
                    .build());

            transactionRepository.save(Transaction.builder()
                    .wallet(wallet)
                    .amount(500000.0)
                    .type(TransactionType.DONATION)
                    .description("Donation to: Help Build a School")
                    .timestamp(LocalDateTime.now().minusDays(1))
                    .build());
        }
    }

    @Override
    public Wallet getWalletByUserId(String userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
    }

    @Override
    public List<Transaction> getTransactionHistory(String walletId) {
        return transactionRepository.findByWalletIdOrderByTimestampDesc(walletId);
    }
}