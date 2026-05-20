package id.ac.ui.cs.advprog.donatjs.dto;

import id.ac.ui.cs.advprog.donatjs.model.WithdrawalRequest;
import id.ac.ui.cs.advprog.donatjs.model.WithdrawalRequestStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class WithdrawalRequestResponse {

    private Long id;
    private Long donationId;
    private String userId;
    private WithdrawalRequestStatus status;
    private String reason;
    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;

    public static WithdrawalRequestResponse from(WithdrawalRequest req) {
        return WithdrawalRequestResponse.builder()
                .id(req.getId())
                .donationId(req.getDonationId())
                .userId(req.getUserId())
                .status(req.getStatus())
                .reason(req.getReason())
                .requestedAt(req.getRequestedAt())
                .processedAt(req.getProcessedAt())
                .build();
    }
}
