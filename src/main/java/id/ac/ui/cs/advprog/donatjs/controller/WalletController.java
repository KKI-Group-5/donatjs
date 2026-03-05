package id.ac.ui.cs.advprog.donatjs.controller;

import id.ac.ui.cs.advprog.donatjs.model.Wallet;
import id.ac.ui.cs.advprog.donatjs.service.WalletService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/wallet")
public class WalletController {

    private final WalletService walletService;
    // Hardcoded for Milestone 1 & 2 since auth isn't fully set up
    private final String CURRENT_USER_ID = "user-demo-001";

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    public String getWalletDashboard(Model model) {
        Wallet wallet = walletService.getWalletByUserId(CURRENT_USER_ID);
        model.addAttribute("wallet", wallet);
        model.addAttribute("transactions", walletService.getTransactionHistory(wallet.getId()));
        return "wallet";
    }
}