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

    // JPA requires a default, empty constructor
    public AppUser() {
    }

    // --- Generate Getters and Setters below this line ---
    // (In IntelliJ, you can press Alt+Insert -> Getter and Setter -> Select all fields)

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

    public boolean isSuspended() { return isSuspended; }
    public void setSuspended(boolean suspended) { isSuspended = suspended; }

    public boolean isFlaggedForReview() { return flaggedForReview; }
    public void setFlaggedForReview(boolean flaggedForReview) { this.flaggedForReview = flaggedForReview; }
}