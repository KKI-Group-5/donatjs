package id.ac.ui.cs.advprog.donatjs.config;

import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import id.ac.ui.cs.advprog.donatjs.repository.UserRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import java.time.LocalDate;

@Component
@Profile("local")
public class LocalDataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public LocalDataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void seedTestUser() {
        if (userRepository.findByEmail("test@donatjs.com").isEmpty()) {
            AppUser user = new AppUser();
            user.setEmail("test@donatjs.com");
            user.setPassword(passwordEncoder.encode("password123"));
            user.setName("Test User");
            user.setBio("Local dev account");
            user.setDateOfBirth(LocalDate.of(2000, 1, 1));
            userRepository.save(user);
        }
    }
}
