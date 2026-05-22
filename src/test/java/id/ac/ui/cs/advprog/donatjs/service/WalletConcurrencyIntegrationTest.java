package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import id.ac.ui.cs.advprog.donatjs.model.Wallet;
import id.ac.ui.cs.advprog.donatjs.repository.TransactionRepository;
import id.ac.ui.cs.advprog.donatjs.repository.UserRepository;
import id.ac.ui.cs.advprog.donatjs.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the PESSIMISTIC_WRITE lock on WalletRepository.findByUserIdForWrite
 * prevents overdraft when concurrent deductions race against the same wallet.
 *
 * Scenario: 10 threads each try to deduct Rp 50,000 from a Rp 100,000 wallet.
 * Only 2 should succeed; the balance must not go negative.
 */
@SpringBootTest
@ActiveProfiles("test")
@SuppressWarnings("null")
class WalletConcurrencyIntegrationTest {

    @MockitoBean
    private EmailService emailService;

    @Autowired private WalletService walletService;
    @Autowired private WalletRepository walletRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private UserRepository userRepository;

    private String userId;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        walletRepository.deleteAll();
        userRepository.deleteAll();

        AppUser user = new AppUser();
        user.setEmail("concurrency-test@example.com");
        user.setPassword("irrelevant");
        user.setName("Concurrency Tester");
        user.setBio("");
        user.setDateOfBirth(LocalDate.of(2000, 1, 1));
        user = userRepository.save(user);
        userId = user.getId().toString();

        walletRepository.save(Wallet.builder()
                .userId(userId)
                .balance(100_000.0)
                .build());
    }

    @Test
    void tenConcurrentDeductions_exactlyTwoSucceed_balanceNeverGoesNegative() throws InterruptedException {
        int threads = 10;
        double deductionPerThread = 50_000.0;

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    walletService.deductBalance(userId, deductionPerThread, "concurrency-test");
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startGate.countDown();
        assertThat(doneLatch.await(15, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        assertThat(successCount.get())
                .as("Exactly 2 deductions of Rp 50,000 should fit within a Rp 100,000 balance")
                .isEqualTo(2);
        assertThat(failureCount.get())
                .as("The remaining 8 threads should have been rejected")
                .isEqualTo(8);

        Wallet wallet = walletRepository.findByUserId(userId).orElseThrow();
        assertThat(wallet.getBalance())
                .as("Balance must be exactly 0 — no overdraft, no double-deduction")
                .isEqualTo(0.0);
    }
}
