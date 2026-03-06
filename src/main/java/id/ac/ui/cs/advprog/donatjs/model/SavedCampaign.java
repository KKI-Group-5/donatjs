package id.ac.ui.cs.advprog.donatjs.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "saved_campaigns",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "campaign_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "campaign_id", nullable = false)
    private String campaignId;

    @Column(name = "campaign_title", nullable = false)
    private String campaignTitle;

    @Column(name = "campaign_organizer")
    private String campaignOrganizer;

    @Column(name = "campaign_image_url")
    private String campaignImageUrl;

    @Column(name = "saved_at", nullable = false)
    private LocalDateTime savedAt;

    @PrePersist
    protected void onCreate() {
        if (savedAt == null) {
            savedAt = LocalDateTime.now();
        }
    }
}
