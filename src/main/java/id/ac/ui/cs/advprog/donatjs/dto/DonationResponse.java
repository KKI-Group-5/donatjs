package id.ac.ui.cs.advprog.donatjs.dto;

import id.ac.ui.cs.advprog.donatjs.model.Donation;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DonationResponse {

    private Long id;
    private String userId;
    private Long campaignId;
    private Donation.DonationType type;
    private Long amount;
    private String notes;
    private Donation.PaymentMethod paymentMethod;
    private Long fee;
    private Long totalAmount;
    private Donation.DonationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DonationResponse from(Donation donation) {
        return DonationResponse.builder()
                .id(donation.getId())
                .userId(donation.getUserId())
                .campaignId(donation.getCampaignId())
                .type(donation.getType())
                .amount(donation.getAmount())
                .notes(donation.getNotes())
                .paymentMethod(donation.getPaymentMethod())
                .fee(donation.getFee())
                .totalAmount(donation.getTotalAmount())
                .status(donation.getStatus())
                .createdAt(donation.getCreatedAt())
                .updatedAt(donation.getUpdatedAt())
                .build();
    }
}