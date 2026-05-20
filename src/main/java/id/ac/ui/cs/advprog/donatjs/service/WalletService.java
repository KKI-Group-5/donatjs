package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.model.Transaction;
import id.ac.ui.cs.advprog.donatjs.model.Wallet;
import java.util.List;

public interface WalletService {
    Wallet getWalletByUserId(String userId);
    List<Transaction> getTransactionHistory(String walletId);
    Wallet deductBalance(String userId, double amount, String description);
    Wallet withdraw(String userId, double amount, String description);
    Wallet deductForDonation(String userId, double amount, String campaignName);
    int bulkRefundForCampaign(Long campaignId, String campaignName);
}