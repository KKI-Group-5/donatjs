package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.dto.UpdateProfileRequest;
import id.ac.ui.cs.advprog.donatjs.dto.UserProfileDTO;
import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import id.ac.ui.cs.advprog.donatjs.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProfileService profileService; // Matches your actual class name

    private AppUser sampleUser;
    private final String testEmail = "aldebaran@ui.ac.id";

    @BeforeEach
    void setUp() {
        sampleUser = new AppUser();
        sampleUser.setEmail(testEmail);
        sampleUser.setName("Aldebaran");
        sampleUser.setBio("Initial Bio");
    }

    @Test
    void testGetUserProfile_Success() {
        // Arrange: tell the mock repository what to return
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(sampleUser));

        // Act: call the actual service method
        UserProfileDTO result = profileService.getUserProfile(testEmail);

        // Assert: verify the results match
        assertNotNull(result);
        assertEquals("Aldebaran", result.getName());
        assertEquals(testEmail, result.getEmail());
        verify(userRepository, times(1)).findByEmail(testEmail);
    }

    @Test
    void testUpdateUserProfile_Success() {
        // Arrange
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setName("New Name");
        request.setBio("New Bio");

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(AppUser.class))).thenReturn(sampleUser);

        // Act
        UserProfileDTO result = profileService.updateUserProfile(testEmail, request);

        // Assert
        assertEquals("New Name", result.getName());
        assertEquals("New Bio", result.getBio());
        verify(userRepository, times(1)).save(any(AppUser.class));
    }

    @Test
    void testGetUserProfile_UserNotFound_ThrowsException() {
        // Arrange: Mock the repository to return empty
        when(userRepository.findByEmail("wrong@email.com")).thenReturn(Optional.empty());

        // Act & Assert: Verify that the service throws the expected exception
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            profileService.getUserProfile("wrong@email.com");
        });

        assertEquals("User not found", exception.getMessage());
    }
}