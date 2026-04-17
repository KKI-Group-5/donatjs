package id.ac.ui.cs.advprog.donatjs.dto;

import jakarta.validation.constraints.NotNull;

public class CampaignModerationRequest {
    @NotNull
    private Boolean approve;

    public Boolean getApprove() {
        return approve;
    }

    public void setApprove(Boolean approve) {
        this.approve = approve;
    }
}
