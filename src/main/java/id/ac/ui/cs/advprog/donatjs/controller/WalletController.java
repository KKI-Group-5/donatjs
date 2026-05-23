package id.ac.ui.cs.advprog.donatjs.controller;

import id.ac.ui.cs.advprog.donatjs.exception.InsufficientBalanceException;
import id.ac.ui.cs.advprog.donatjs.model.Wallet;
import id.ac.ui.cs.advprog.donatjs.service.CurrentUserService;
import id.ac.ui.cs.advprog.donatjs.service.WalletService;
import id.ac.ui.cs.advprog.donatjs.util.IdrMoney;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
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

    private static final Logger log = LoggerFactory.getLogger(WalletController.class);

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

    @PostMapping("/top-up")
    public String topUp(
            @RequestParam("amount") double amount,
            @RequestParam(value = "description", defaultValue = "") String description,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            String currentUserId = currentUserService.getCurrentUserId(authentication);
            walletService.topUp(currentUserId, amount, description);
            NumberFormat nf = NumberFormat.getIntegerInstance(Locale.of("id", "ID"));
            long rupiah = IdrMoney.wholeRupiah(amount);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Top up of Rp " + nf.format(rupiah) + " was successful.");
        } catch (IllegalArgumentException | ArithmeticException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (DataAccessException e) {
            log.warn("Top up failed (database)", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Could not complete top up due to a database error. Please try again.");
        }
        return "redirect:/wallet";
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
            long rupiah = IdrMoney.wholeRupiah(amount);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Withdrawal of Rp " + nf.format(rupiah) + " was successful.");
        } catch (InsufficientBalanceException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (DataAccessException e) {
            log.warn("Withdraw failed (database)", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Could not complete withdrawal due to a database error. Please try again.");
        }
        return "redirect:/wallet";
    }
}
