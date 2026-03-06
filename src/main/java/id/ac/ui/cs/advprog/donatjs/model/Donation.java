package id.ac.ui.cs.advprog.donatjs.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "donations", indexes = {
        @Index(name = "idx_donation_user",     columnList = "user_id"),
        @Index(name = "idx_donation_campaign", columnList = "campaign_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Donation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DonationType type;

    @Column(nullable = false)
    private Long amount;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Column(nullable = false)
    private Long fee;

    @Column(nullable = false)
    private Long totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DonationStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum DonationType {
        ONE_TIME,
        SUBSCRIPTION
    }

    public enum DonationStatus {
        SUCCESS,
        REJECTED
    }

    public enum PaymentMethod {
        WALLET,
        BANK_TRANSFER,
        BANK_BCA,
        BANK_MANDIRI,
        BANK_BNI,
        BANK_BRI,
        GOPAY,
        OVO,
        DANA,
        SHOPEEPAY,
        LINKAJA
    }
}