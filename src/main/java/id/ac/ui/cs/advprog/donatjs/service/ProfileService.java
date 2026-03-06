package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.dto.UpdateProfileRequest;
import id.ac.ui.cs.advprog.donatjs.dto.UserProfileDTO;
import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import id.ac.ui.cs.advprog.donatjs.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {

    private final UserRepository userRepository;

    public ProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserProfileDTO getUserProfile(String email) {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return new UserProfileDTO(user.getName(), user.getEmail(), user.getBio(), user.getDateOfBirth());
    }

    public UserProfileDTO updateUserProfile(String email, UpdateProfileRequest request) {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getName() != null) user.setName(request.getName());
        if (request.getBio() != null) user.setBio(request.getBio());
        if (request.getDateOfBirth() != null) user.setDateOfBirth(request.getDateOfBirth());

        AppUser updatedUser = userRepository.save(user);

        return new UserProfileDTO(updatedUser.getName(), updatedUser.getEmail(), updatedUser.getBio(), updatedUser.getDateOfBirth());
    }
}