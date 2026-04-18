package id.ac.ui.cs.advprog.donatjs.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A recurring donation the user has committed to. The scheduler scans for
 * {@code ACTIVE} subscriptions whose {@code nextBillingAt} has elapsed and
 * debits the user's wallet once per period.
 */
@Entity
@Table(name = "subscriptions", indexes = {
        @Index(name = "idx_subscription_user",          columnList = "user_id"),
        @Index(name = "idx_subscription_next_billing",  columnList = "next_billing_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(name = "campaign_title")
    private String campaignTitle;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_interval", nullable = false)
    private Interval interval;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "next_billing_at", nullable = false)
    private LocalDateTime nextBillingAt;

    @Column(name = "last_billed_at")
    private LocalDateTime lastBilledAt;

    @Column(name = "last_failure_message")
    private String lastFailureMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = Status.ACTIVE;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /** Billing cadence for a subscription. */
    public enum Interval {
        DAILY, WEEKLY, MONTHLY;

        /** Advances the given timestamp by one period. */
        public LocalDateTime advance(LocalDateTime from) {
            return switch (this) {
                case DAILY   -> from.plusDays(1);
                case WEEKLY  -> from.plusWeeks(1);
                case MONTHLY -> from.plusMonths(1);
            };
        }
    }

    public enum Status { ACTIVE, CANCELLED, PAUSED }
}
