package id.ac.ui.cs.advprog.donatjs.controller;

import id.ac.ui.cs.advprog.donatjs.model.Campaign;
import id.ac.ui.cs.advprog.donatjs.dto.UpdateCampaignDescriptionRequest;
import id.ac.ui.cs.advprog.donatjs.service.CampaignService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;

@Controller
@RequestMapping("/campaigns")
public class CampaignController {

    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @GetMapping
    public String listCampaigns(Model model) {
        model.addAttribute("campaigns", campaignService.findOpenCampaigns());
        return "campaigns/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("campaign", new Campaign());
        return "campaigns/create";
    }

    @PostMapping("/create")
    public String createCampaign(@Valid @ModelAttribute("campaign") Campaign campaign,
                                 BindingResult bindingResult) {
        if (campaign.getDeadline() != null && !campaign.getDeadline().isAfter(LocalDate.now())) {
            bindingResult.rejectValue("deadline", "deadline.notFuture", "Deadline must be in the future");
        }

        if (bindingResult.hasErrors()) {
            return "campaigns/create";
        }

        campaignService.createCampaign(campaign);
        return "redirect:/campaigns";
    }

    @GetMapping("/{id}")
    public String campaignDetail(@PathVariable("id") Long id, Model model) {
        Campaign campaign = campaignService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("campaign", campaign);
        return "campaigns/detail";
    }

    @GetMapping("/{id}/edit")
    public String showEditDescriptionForm(@PathVariable("id") Long id, Model model) {
        Campaign campaign = campaignService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        UpdateCampaignDescriptionRequest req = new UpdateCampaignDescriptionRequest();
        req.setDescription(campaign.getDescription());

        model.addAttribute("campaign", campaign);
        model.addAttribute("req", req);

        return "campaigns/edit";
    }

    @PostMapping("/{id}/edit")
    public String updateDescription(@PathVariable("id") Long id,
                                    @Valid @ModelAttribute("req") UpdateCampaignDescriptionRequest req,
                                    BindingResult bindingResult,
                                    Model model) {
        if (bindingResult.hasErrors()) {
            Campaign campaign = campaignService.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            model.addAttribute("campaign", campaign);
            return "campaigns/edit";
        }

        Campaign updated = campaignService.updateDescription(id, req.getDescription());
        return "redirect:/campaigns/" + updated.getId();
    }

    @PostMapping("/{id}/delete")
    public String deleteCampaign(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            campaignService.deleteIfNoDonations(id);
            return "redirect:/campaigns";
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode() == HttpStatus.BAD_REQUEST) {
                String message = ex.getReason() != null
                        ? ex.getReason()
                        : "Cannot delete campaign with donations";
                redirectAttributes.addFlashAttribute("deleteError", message);
                return "redirect:/campaigns/" + id;
            }
            throw ex;
        }
    }
}

