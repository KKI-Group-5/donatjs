package id.ac.ui.cs.advprog.donatjs.controller;

import id.ac.ui.cs.advprog.donatjs.exception.InsufficientBalanceException;
import id.ac.ui.cs.advprog.donatjs.model.Wallet;
import id.ac.ui.cs.advprog.donatjs.service.CurrentUserService;
import id.ac.ui.cs.advprog.donatjs.service.WalletService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.text.NumberFormat;
import java.util.Locale;

@Controller
@RequestMapping("/wallet")
public class WalletController {

    private final WalletService walletService;
    private final CurrentUserService currentUserService;

    public WalletController(WalletService walletService, CurrentUserService currentUserService) {
        this.walletService = walletService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public String getWalletDashboard(Model model, Authentication authentication) {
        String currentUserId = currentUserService.getCurrentUserId(authentication);
        Wallet wallet = walletService.getWalletByUserId(currentUserId);
        model.addAttribute("wallet", wallet);
        model.addAttribute("transactions", walletService.getTransactionHistory(wallet.getId()));
        return "wallet";
    }

    @PostMapping("/withdraw")
    public String withdraw(
            @RequestParam("amount") double amount,
            @RequestParam(value = "description", defaultValue = "") String description,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            String currentUserId = currentUserService.getCurrentUserId(authentication);
            walletService.withdraw(currentUserId, amount, description);
            NumberFormat nf = NumberFormat.getIntegerInstance(Locale.of("id", "ID"));
            redirectAttributes.addFlashAttribute("successMessage",
                    "Withdrawal of Rp " + nf.format((long) amount) + " was successful.");
        } catch (InsufficientBalanceException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/wallet";
    }
}
