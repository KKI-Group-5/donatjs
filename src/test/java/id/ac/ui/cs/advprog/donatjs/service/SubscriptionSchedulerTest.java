package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.model.Subscription;
import id.ac.ui.cs.advprog.donatjs.model.Subscription.SubscriptionFrequency;
import id.ac.ui.cs.advprog.donatjs.model.Subscription.SubscriptionStatus;
import id.ac.ui.cs.advprog.donatjs.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class SubscriptionSchedulerTest {

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private WalletService walletService;
    @Mock private DonationService donationService;

    @InjectMocks
    private SubscriptionScheduler scheduler;

    private static final String USER_ID    = "user-001";
    private static final Long   CAMPAIGN_ID = 1L;
    private static final Long   AMOUNT      = 50_000L;

    private Subscription dueSubscription;

    @BeforeEach
    void setUp() {
        dueSubscription = Subscription.builder()
                .id(1L)
                .userId(USER_ID)
                .campaignId(CAMPAIGN_ID)
                .amount(AMOUNT)
                .frequency(SubscriptionFrequency.MONTHLY)
                .status(SubscriptionStatus.ACTIVE)
                .nextDebitDate(LocalDate.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void processSubscriptions_deductsAndAdvancesDate() {
        when(subscriptionRepository.findByStatusAndNextDebitDateLessThanEqual(
                SubscriptionStatus.ACTIVE, LocalDate.now()))
                .thenReturn(List.of(dueSubscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(i -> i.getArgument(0));

        scheduler.processSubscriptions();

        verify(walletService).deductBalance(eq(USER_ID), eq(AMOUNT.doubleValue()), anyString());
        verify(donationService).createDonation(any());

        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(captor.capture());
        assertEquals(LocalDate.now().plusMonths(1), captor.getValue().getNextDebitDate());
    }

    @Test
    void processSubscriptions_advancesDateCorrectlyForWeekly() {
        dueSubscription.setFrequency(SubscriptionFrequency.WEEKLY);
        when(subscriptionRepository.findByStatusAndNextDebitDateLessThanEqual(
                SubscriptionStatus.ACTIVE, LocalDate.now()))
                .thenReturn(List.of(dueSubscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(i -> i.getArgument(0));

        scheduler.processSubscriptions();

        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(captor.capture());
        assertEquals(LocalDate.now().plusWeeks(1), captor.getValue().getNextDebitDate());
    }

    @Test
    void processSubscriptions_advancesDateCorrectlyForDaily() {
        dueSubscription.setFrequency(SubscriptionFrequency.DAILY);
        when(subscriptionRepository.findByStatusAndNextDebitDateLessThanEqual(
                SubscriptionStatus.ACTIVE, LocalDate.now()))
                .thenReturn(List.of(dueSubscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(i -> i.getArgument(0));

        scheduler.processSubscriptions();

        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(captor.capture());
        assertEquals(LocalDate.now().plusDays(1), captor.getValue().getNextDebitDate());
    }

    @Test
    void processSubscriptions_skipsOnInsufficientBalance() {
        when(subscriptionRepository.findByStatusAndNextDebitDateLessThanEqual(
                SubscriptionStatus.ACTIVE, LocalDate.now()))
                .thenReturn(List.of(dueSubscription));
        doThrow(new IllegalStateException("Insufficient balance"))
                .when(walletService).deductBalance(anyString(), anyDouble(), anyString());

        // Should not throw — scheduler catches and logs
        scheduler.processSubscriptions();

        verify(walletService).deductBalance(anyString(), anyDouble(), anyString());
        verify(donationService, never()).createDonation(any());
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void processSubscriptions_noDueSubscriptions_doesNothing() {
        when(subscriptionRepository.findByStatusAndNextDebitDateLessThanEqual(
                SubscriptionStatus.ACTIVE, LocalDate.now()))
                .thenReturn(Collections.emptyList());

        scheduler.processSubscriptions();

        verify(walletService, never()).deductBalance(anyString(), anyDouble(), anyString());
        verify(subscriptionRepository, never()).save(any());
    }
}
