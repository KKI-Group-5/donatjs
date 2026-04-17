package id.ac.ui.cs.advprog.donatjs.config;

import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import id.ac.ui.cs.advprog.donatjs.model.Campaign;
import id.ac.ui.cs.advprog.donatjs.model.CampaignStatus;
import id.ac.ui.cs.advprog.donatjs.repository.CampaignRepository;
import id.ac.ui.cs.advprog.donatjs.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Configuration
@Profile("local")
public class LocalDataInitializer {

    @Value("${donatjs.local.test-user.password}")
    private String testUserPassword;

    @Bean
    public CommandLineRunner seedData(UserRepository userRepository, 
                                      CampaignRepository campaignRepository,
                                      PasswordEncoder passwordEncoder) {
        return args -> {
            // 1. Seed Test User
            if (userRepository.findByEmail("test@donatjs.com").isEmpty()) {
                AppUser user = new AppUser();
                user.setEmail("test@donatjs.com");
                user.setPassword(passwordEncoder.encode(testUserPassword));
                user.setName("Test User");
                user.setBio("Local dev account");
                user.setDateOfBirth(LocalDate.of(2000, 1, 1));
                userRepository.save(user);
            }

            // 2. Seed Sample Campaigns
            if (campaignRepository.findAll().isEmpty()) {
                Campaign c1 = new Campaign();
                c1.setTitle("Help Flood Victims in Jakarta");
                c1.setDescription("Providing emergency food and shelter for those affected by the recent floods.");
                c1.setTargetAmount(new BigDecimal("10000000"));
                c1.setDeadline(LocalDate.now().plusMonths(2));
                c1.setStatus(CampaignStatus.OPEN);
                c1.setCreatorId("system");
                campaignRepository.save(c1);

                Campaign c2 = new Campaign();
                c2.setTitle("Build a Library for Kids");
                c2.setDescription("A project to build a community library in a remote village.");
                c2.setTargetAmount(new BigDecimal("25000000"));
                c2.setDeadline(LocalDate.now().plusMonths(6));
                c2.setStatus(CampaignStatus.OPEN);
                c2.setCreatorId("system");
                campaignRepository.save(c2);
            }
        };
    }
}