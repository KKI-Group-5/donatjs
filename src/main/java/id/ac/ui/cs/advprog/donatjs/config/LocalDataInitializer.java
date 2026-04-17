package id.ac.ui.cs.advprog.donatjs.config;

import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import id.ac.ui.cs.advprog.donatjs.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

@Configuration
@Profile("local")
public class LocalDataInitializer {

    @Value("${donatjs.local.test-user.password}")
    private String testUserPassword;

    @Bean
    public CommandLineRunner seedTestUser(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.findByEmail("test@donatjs.com").isEmpty()) {
                AppUser user = new AppUser();
                user.setEmail("test@donatjs.com");
                user.setPassword(passwordEncoder.encode(testUserPassword));
                user.setName("Test User");
                user.setBio("Local dev account");
                user.setDateOfBirth(LocalDate.of(2000, 1, 1));
                userRepository.save(user);
            }
        };
    }
}