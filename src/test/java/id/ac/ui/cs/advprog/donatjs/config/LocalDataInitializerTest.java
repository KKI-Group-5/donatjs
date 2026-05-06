package id.ac.ui.cs.advprog.donatjs.config;

import id.ac.ui.cs.advprog.donatjs.repository.CampaignRepository;
import id.ac.ui.cs.advprog.donatjs.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

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
    @SuppressWarnings("null")
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(localDataInitializer, "testUserPassword", "password123");
    }

    // @Test
    // void testSeedData_RunsSuccessfully() throws Exception {
    //     // Arrange
    //     when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
    //     when(campaignRepository.findAll()).thenReturn(Collections.emptyList());
    //     when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

    //     CommandLineRunner runner = localDataInitializer.seedLocalData(userRepository, campaignRepository, passwordEncoder);

    //     // Act
    //     runner.run();

    //     // Assert
    //     verify(userRepository, times(1)).save(any());
    //     verify(campaignRepository, times(2)).save(any());
    // }
}
