package id.ac.ui.cs.advprog.donatjs.controller;

import id.ac.ui.cs.advprog.donatjs.exception.InsufficientBalanceException;
import id.ac.ui.cs.advprog.donatjs.model.Wallet;
import id.ac.ui.cs.advprog.donatjs.service.WalletService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/wallet")
public class WalletController {

    private final WalletService walletService;
    // Hardcoded until auth is wired up
    private static final String CURRENT_USER_ID = "user-demo-001";

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

    @PostMapping("/withdraw")
    public String withdraw(
            @RequestParam("amount") double amount,
            @RequestParam(value = "description", defaultValue = "") String description,
            RedirectAttributes redirectAttributes) {
        try {
            walletService.withdraw(CURRENT_USER_ID, amount, description);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Withdrawal of Rp " + String.format("%,.0f", amount) + " was successful.");
        } catch (InsufficientBalanceException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/wallet";
    }
}
