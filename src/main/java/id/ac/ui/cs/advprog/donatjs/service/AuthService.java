package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.dto.RegisterRequest;
import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import id.ac.ui.cs.advprog.donatjs.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

import id.ac.ui.cs.advprog.donatjs.model.VerificationToken;
import id.ac.ui.cs.advprog.donatjs.repository.VerificationTokenRepository;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationTokenRepository tokenRepository;
    private final EmailService emailService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       VerificationTokenRepository tokenRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
    }

    public AppUser registerUser(RegisterRequest request) {
        Optional<AppUser> existingUser = userRepository.findByEmail(request.getEmail());
        if (existingUser.isPresent()) {
            throw new RuntimeException("Email is already registered");
        }

        AppUser newUser = new AppUser();
        newUser.setEmail(request.getEmail());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setName(request.getName());
        newUser.setBio(request.getBio());
        newUser.setDateOfBirth(request.getDateOfBirth());
        newUser.setVerified(false);

        AppUser savedUser = userRepository.save(newUser);

        // Generate and save verification token
        String token = UUID.randomUUID().toString();
        VerificationToken vToken = new VerificationToken(token, savedUser);
        tokenRepository.save(vToken);

        // Send verification email
        String verifyUrl = "http://localhost:8080/api/auth/verify?token=" + token;
        emailService.sendEmail(savedUser.getEmail(), "Verify your DonatJS account",
                "Please click the following link to verify your email address:\n\n" + verifyUrl);

        return savedUser;
    }

    public boolean verifyEmail(String token) {
        Optional<VerificationToken> tokenOpt = tokenRepository.findByToken(token);
        if (tokenOpt.isPresent()) {
            VerificationToken vToken = tokenOpt.get();
            if (vToken.getExpiryDate().isAfter(java.time.LocalDateTime.now())) {
                AppUser user = vToken.getUser();
                user.setVerified(true);
                userRepository.save(user);
                tokenRepository.delete(vToken);
                return true;
            }
        }
        return false;
    }
}
