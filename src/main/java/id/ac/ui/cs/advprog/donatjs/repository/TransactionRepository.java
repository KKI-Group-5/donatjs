package id.ac.ui.cs.advprog.donatjs.repository;

import id.ac.ui.cs.advprog.donatjs.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, String> {
    List<Transaction> findByWalletIdOrderByTimestampDesc(String walletId);
}