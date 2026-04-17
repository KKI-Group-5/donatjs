package id.ac.ui.cs.advprog.donatjs.dto;

import id.ac.ui.cs.advprog.donatjs.model.Campaign;
import id.ac.ui.cs.advprog.donatjs.model.SavedCampaign;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
public class UserProfileDTO {
    private String name;
    private String email;
    private String bio;
    private LocalDate dateOfBirth;

    // Milestone 3: Aggregated activities
    private List<Campaign> createdCampaigns;
    private List<DonationResponse> donations;
    private List<SavedCampaign> savedCampaigns;

    public UserProfileDTO(String name, String email, String bio, LocalDate dateOfBirth) {
        this.name = name;
        this.email = email;
        this.bio = bio;
        this.dateOfBirth = dateOfBirth;
    }
}