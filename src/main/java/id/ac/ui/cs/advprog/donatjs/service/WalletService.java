package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.model.Transaction;
import id.ac.ui.cs.advprog.donatjs.model.Wallet;
import java.util.List;

public interface WalletService {
    Wallet getWalletByUserId(String userId);
    List<Transaction> getTransactionHistory(String walletId);
}