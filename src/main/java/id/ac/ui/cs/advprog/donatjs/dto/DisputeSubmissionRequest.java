package id.ac.ui.cs.advprog.donatjs.dto;

import jakarta.validation.constraints.NotBlank;

public class DisputeSubmissionRequest {
    @NotBlank(message = "Reason is mandatory")
    private String reason;

    public DisputeSubmissionRequest() {
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
