package id.ac.ui.cs.advprog.donatjs.config;

import id.ac.ui.cs.advprog.donatjs.repository.CampaignRepository;
import id.ac.ui.cs.advprog.donatjs.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class LocalDataInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private LocalDataInitializer localDataInitializer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(localDataInitializer, "testUserPassword", "password123");
    }

    @Test
    void testSeedData_RunsSuccessfully() throws Exception {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(campaignRepository.findAll()).thenReturn(Collections.emptyList());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        CommandLineRunner runner = localDataInitializer.seedData(userRepository, campaignRepository, passwordEncoder);

        // Act
        runner.run();

        // Assert
        verify(userRepository, times(1)).save(any());
        verify(campaignRepository, times(2)).save(any());
    }
}
