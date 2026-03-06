package id.ac.ui.cs.advprog.donatjs.dto;

import id.ac.ui.cs.advprog.donatjs.model.Donation;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateDonationRequest {

    @NotNull(message = "Campaign ID is required")
    private Long campaignId;

    @NotNull(message = "User ID is required")
    private String userId;

    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount must be at least Rp 1")
    private Long amount;

    private String notes;

    @NotNull(message = "Payment method is required")
    private Donation.PaymentMethod paymentMethod;

    @NotNull(message = "Donation type is required")
    private Donation.DonationType type = Donation.DonationType.ONE_TIME;
}