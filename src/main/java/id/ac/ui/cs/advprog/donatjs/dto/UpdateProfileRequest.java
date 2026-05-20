package id.ac.ui.cs.advprog.donatjs.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Setter
@Getter
public class UpdateProfileRequest {
    private String name;
    private String bio;
    private LocalDate dateOfBirth;

}