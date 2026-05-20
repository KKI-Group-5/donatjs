package id.ac.ui.cs.advprog.donatjs.controller;

import id.ac.ui.cs.advprog.donatjs.dto.DisputeDTO;
import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import id.ac.ui.cs.advprog.donatjs.service.CurrentUserService;
import id.ac.ui.cs.advprog.donatjs.service.DisputeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Objects;

@Controller
@RequiredArgsConstructor
public class DisputePageController {

    private final DisputeService disputeService;
    private final CurrentUserService currentUserService;

    @GetMapping("/dispute")
    public String disputeForm(Model model) {
        AppUser user = currentUserService.requireCurrentUser();
        
        if (!user.isSuspended()) {
            return "redirect:/profile";
        }
        
        List<DisputeDTO> myDisputes = disputeService.getDisputesByUser(Objects.requireNonNull(user.getId()));
        model.addAttribute("myDisputes", myDisputes);
        return "dispute";
    }

    @GetMapping("/admin/disputes")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminDisputes(Model model) {
        List<DisputeDTO> pendingDisputes = disputeService.getAllPendingDisputes();
        model.addAttribute("pendingDisputes", pendingDisputes);
        return "admin-disputes";
    }
}
