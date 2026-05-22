package id.ac.ui.cs.advprog.donatjs.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;

    private String name;
    private String bio;
    private LocalDate dateOfBirth;

    // Milestone 3: Tracking for FRAUD/REJECTED activity
    private int rejectedDonationCount = 0;
    private int rejectedCampaignCount = 0;
    private boolean isSuspended = false;
    private boolean flaggedForReview = false;

    @Column(nullable = false)
    private int fraudActivityCount = 0;

    @Column(nullable = false)
    private boolean flagged = false;

    @Column(nullable = false)
    private boolean suspended = false;

    @Column(nullable = false)
    private boolean admin = false;

    @Column(nullable = false)
    private boolean isVerified = true;

    public AppUser() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public int getRejectedDonationCount() { return rejectedDonationCount; }
    public void setRejectedDonationCount(int rejectedDonationCount) { this.rejectedDonationCount = rejectedDonationCount; }

    public int getRejectedCampaignCount() { return rejectedCampaignCount; }
    public void setRejectedCampaignCount(int rejectedCampaignCount) { this.rejectedCampaignCount = rejectedCampaignCount; }

    public boolean isSuspendedLegacy() { return isSuspended; }
    public void setSuspendedLegacy(boolean suspended) { isSuspended = suspended; }

    public boolean isFlaggedForReview() { return flaggedForReview; }
    public void setFlaggedForReview(boolean flaggedForReview) { this.flaggedForReview = flaggedForReview; }

    public int getFraudActivityCount() { return fraudActivityCount; }
    public void setFraudActivityCount(int fraudActivityCount) { this.fraudActivityCount = fraudActivityCount; }

    public boolean isFlagged() { return flagged; }
    public void setFlagged(boolean flagged) { this.flagged = flagged; }

    public boolean isSuspended() { return suspended; }
    public void setSuspended(boolean suspended) { this.suspended = suspended; }

    public boolean isAdmin() { return admin; }
    public void setAdmin(boolean admin) { this.admin = admin; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }
}
