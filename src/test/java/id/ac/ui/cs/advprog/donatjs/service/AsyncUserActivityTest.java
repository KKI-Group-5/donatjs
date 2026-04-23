package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.dto.DonationResponse;
import id.ac.ui.cs.advprog.donatjs.event.RejectedDonationEvent;
import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import id.ac.ui.cs.advprog.donatjs.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
public class AsyncUserActivityTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void testRejectedDonation_IsProcessedAsynchronously() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);
        user.setRejectedDonationCount(0);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        DonationResponse donation = DonationResponse.builder()
                .userId(userId.toString())
                .build();
        
        CountDownLatch latch = new CountDownLatch(1);
        
        doAnswer(invocation -> {
            System.out.println("Listener thread: " + Thread.currentThread().getName());
            latch.countDown();
            return null;
        }).when(userRepository).save(any());

        // Act
        System.out.println("Main thread: " + Thread.currentThread().getName());
        eventPublisher.publishEvent(new RejectedDonationEvent(this, donation));
        
        // Assert
        boolean completed = latch.await(2, TimeUnit.SECONDS);
        assertTrue(completed, "Async listener should have completed execution");
        verify(userRepository, atLeastOnce()).save(any());
    }
}
