package id.ac.ui.cs.advprog.donatjs.event;

import lombok.Getter;
import java.time.LocalDate;

@Getter
public class ProfileUpdatedEvent {
    private final String userId;
    private final String name;
    private final String bio;
    private final LocalDate dateOfBirth;

    public ProfileUpdatedEvent(String userId, String name, String bio, LocalDate dateOfBirth) {
        this.userId = userId;
        this.name = name;
        this.bio = bio;
        this.dateOfBirth = dateOfBirth;
    }
}
