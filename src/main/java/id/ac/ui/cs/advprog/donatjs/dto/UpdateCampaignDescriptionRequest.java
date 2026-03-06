package id.ac.ui.cs.advprog.donatjs.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateCampaignDescriptionRequest {

    @NotBlank
    private String description;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

