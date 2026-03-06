package id.ac.ui.cs.advprog.donatjs.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Setter
@Getter
public class UserProfileDTO {
    // Generate Getters and Setters here (Alt+Insert)
    private String name;
    private String email;
    private String bio;
    private LocalDate dateOfBirth;

    // Constructors
    public UserProfileDTO(String name, String email, String bio, LocalDate dateOfBirth) {
        this.name = name;
        this.email = email;
        this.bio = bio;
        this.dateOfBirth = dateOfBirth;
    }

}