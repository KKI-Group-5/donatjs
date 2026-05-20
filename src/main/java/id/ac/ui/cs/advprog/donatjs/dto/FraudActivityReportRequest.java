package id.ac.ui.cs.advprog.donatjs.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class FraudActivityReportRequest {

    @Email
    @NotBlank
    private String userEmail;

    @NotBlank
    private String reason;

    public FraudActivityReportRequest() {
    }

    public FraudActivityReportRequest(String userEmail, String reason) {
        this.userEmail = userEmail;
        this.reason = reason;
    }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
