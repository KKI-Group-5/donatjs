package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.dto.CreateSubscriptionRequest;
import id.ac.ui.cs.advprog.donatjs.dto.SubscriptionResponse;
import id.ac.ui.cs.advprog.donatjs.model.Subscription;
import id.ac.ui.cs.advprog.donatjs.model.Subscription.SubscriptionFrequency;
import id.ac.ui.cs.advprog.donatjs.model.Subscription.SubscriptionStatus;
import id.ac.ui.cs.advprog.donatjs.repository.SubscriptionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class SubscriptionServiceImplTest {

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private WalletService walletService;
    @Mock private DonationService donationService;

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

    private static final String USER_ID    = "user-001";
    private static final Long   CAMPAIGN_ID = 1L;
    private static final Long   AMOUNT      = 50_000L;

    private Subscription activeSubscription;

    @BeforeEach
    void setUp() {
        activeSubscription = Subscription.builder()
                .id(1L)
                .userId(USER_ID)
                .campaignId(CAMPAIGN_ID)
                .amount(AMOUNT)
                .frequency(SubscriptionFrequency.MONTHLY)
                .status(SubscriptionStatus.ACTIVE)
                .nextDebitDate(LocalDate.now().plusMonths(1))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createSubscription_success() {
        CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                .userId(USER_ID).campaignId(CAMPAIGN_ID)
                .amount(AMOUNT).frequency(SubscriptionFrequency.MONTHLY)
                .build();

        when(subscriptionRepository.existsByUserIdAndCampaignIdAndStatus(
                USER_ID, CAMPAIGN_ID, SubscriptionStatus.ACTIVE)).thenReturn(false);
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(activeSubscription);

        SubscriptionResponse response = subscriptionService.createSubscription(request);

        assertNotNull(response);
        assertEquals(USER_ID, response.getUserId());
        assertEquals(CAMPAIGN_ID, response.getCampaignId());
        assertEquals(SubscriptionStatus.ACTIVE, response.getStatus());
        verify(walletService).deductBalance(eq(USER_ID), eq(AMOUNT.doubleValue()), anyString());
        verify(donationService).createDonation(any());
        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    void createSubscription_duplicateActive_throwsException() {
        CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                .userId(USER_ID).campaignId(CAMPAIGN_ID)
                .amount(AMOUNT).frequency(SubscriptionFrequency.MONTHLY)
                .build();

        when(subscriptionRepository.existsByUserIdAndCampaignIdAndStatus(
                USER_ID, CAMPAIGN_ID, SubscriptionStatus.ACTIVE)).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> subscriptionService.createSubscription(request));

        assertTrue(ex.getMessage().contains("already exists"));
        verify(walletService, never()).deductBalance(anyString(), anyDouble(), anyString());
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void createSubscription_insufficientBalance_throwsException() {
        CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                .userId(USER_ID).campaignId(CAMPAIGN_ID)
                .amount(AMOUNT).frequency(SubscriptionFrequency.MONTHLY)
                .build();

        when(subscriptionRepository.existsByUserIdAndCampaignIdAndStatus(
                USER_ID, CAMPAIGN_ID, SubscriptionStatus.ACTIVE)).thenReturn(false);
        doThrow(new IllegalStateException("Insufficient balance"))
                .when(walletService).deductBalance(anyString(), anyDouble(), anyString());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> subscriptionService.createSubscription(request));

        assertEquals("Insufficient balance", ex.getMessage());
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void cancelSubscription_success() {
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(activeSubscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(i -> i.getArgument(0));

        SubscriptionResponse response = subscriptionService.cancelSubscription(1L, USER_ID);

        assertEquals(SubscriptionStatus.CANCELLED, response.getStatus());
        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    void cancelSubscription_notFound_throwsException() {
        when(subscriptionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> subscriptionService.cancelSubscription(99L, USER_ID));
    }

    @Test
    void cancelSubscription_wrongUser_throwsException() {
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(activeSubscription));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> subscriptionService.cancelSubscription(1L, "other-user"));

        assertTrue(ex.getMessage().contains("do not own"));
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void updateFrequency_success() {
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(activeSubscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(i -> i.getArgument(0));

        SubscriptionResponse response = subscriptionService.updateFrequency(1L, USER_ID, SubscriptionFrequency.WEEKLY);

        assertEquals(SubscriptionFrequency.WEEKLY, response.getFrequency());
        assertNotNull(response.getNextDebitDate());
        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    void updateFrequency_wrongUser_throwsException() {
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(activeSubscription));

        assertThrows(IllegalStateException.class,
                () -> subscriptionService.updateFrequency(1L, "other-user", SubscriptionFrequency.DAILY));
    }

    @Test
    void getSubscriptionsByUser_returnsList() {
        Subscription sub2 = Subscription.builder()
                .id(2L).userId(USER_ID).campaignId(2L).amount(30_000L)
                .frequency(SubscriptionFrequency.WEEKLY).status(SubscriptionStatus.ACTIVE)
                .nextDebitDate(LocalDate.now().plusWeeks(1))
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        when(subscriptionRepository.findByUserIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(Arrays.asList(activeSubscription, sub2));

        List<SubscriptionResponse> result = subscriptionService.getSubscriptionsByUser(USER_ID);

        assertEquals(2, result.size());
        verify(subscriptionRepository).findByUserIdOrderByCreatedAtDesc(USER_ID);
    }

    @Test
    void getSubscriptionsByUser_emptyList() {
        when(subscriptionRepository.findByUserIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(List.of());

        List<SubscriptionResponse> result = subscriptionService.getSubscriptionsByUser(USER_ID);

        assertTrue(result.isEmpty());
    }
}
