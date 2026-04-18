package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.dto.CreateSubscriptionRequest;
import id.ac.ui.cs.advprog.donatjs.dto.UpdateSubscriptionRequest;
import id.ac.ui.cs.advprog.donatjs.exception.InsufficientBalanceException;
import id.ac.ui.cs.advprog.donatjs.model.Campaign;
import id.ac.ui.cs.advprog.donatjs.model.CampaignStatus;
import id.ac.ui.cs.advprog.donatjs.model.Subscription;
import id.ac.ui.cs.advprog.donatjs.model.Subscription.Interval;
import id.ac.ui.cs.advprog.donatjs.model.Subscription.Status;
import id.ac.ui.cs.advprog.donatjs.repository.SubscriptionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private CampaignService campaignService;
    @Mock private WalletService walletService;

    @InjectMocks private SubscriptionService subscriptionService;

    private Campaign openCampaign;

    @BeforeEach
    void setUp() {
        openCampaign = mock(Campaign.class);
        lenient().when(openCampaign.getId()).thenReturn(42L);
        lenient().when(openCampaign.getTitle()).thenReturn("Demo Campaign");
        lenient().when(openCampaign.getStatus()).thenReturn(CampaignStatus.OPEN);
        lenient().when(campaignService.findById(42L)).thenReturn(Optional.of(openCampaign));
        // Return whatever we save so the service can chain set()/save()
        lenient().when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // subscribe

    @Test
    void subscribe_happyPath_createsActiveSubscription() {
        when(subscriptionRepository.findByUserIdAndCampaignIdAndStatus("u1", 42L, Status.ACTIVE))
                .thenReturn(Optional.empty());

        CreateSubscriptionRequest req = new CreateSubscriptionRequest();
        req.setCampaignId(42L);
        req.setAmount(new BigDecimal("50000"));
        req.setInterval(Interval.WEEKLY);

        Subscription saved = subscriptionService.subscribe("u1", req);

        assertEquals(Status.ACTIVE, saved.getStatus());
        assertEquals(Interval.WEEKLY, saved.getInterval());
        assertEquals(42L, saved.getCampaignId());
        assertEquals("Demo Campaign", saved.getCampaignTitle());
        assertNotNull(saved.getNextBillingAt());
        assertTrue(saved.getNextBillingAt().isAfter(LocalDateTime.now()));
    }

    @Test
    void subscribe_nonOpenCampaign_rejected() {
        when(openCampaign.getStatus()).thenReturn(CampaignStatus.CLOSED);

        CreateSubscriptionRequest req = new CreateSubscriptionRequest();
        req.setCampaignId(42L);
        req.setAmount(new BigDecimal("10000"));
        req.setInterval(Interval.DAILY);

        assertThrows(ResponseStatusException.class,
                () -> subscriptionService.subscribe("u1", req));
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void subscribe_missingCampaign_throws() {
        when(campaignService.findById(anyLong())).thenReturn(Optional.empty());

        CreateSubscriptionRequest req = new CreateSubscriptionRequest();
        req.setCampaignId(999L);
        req.setAmount(new BigDecimal("10000"));
        req.setInterval(Interval.DAILY);

        assertThrows(EntityNotFoundException.class,
                () -> subscriptionService.subscribe("u1", req));
    }

    @Test
    void subscribe_duplicateActive_throwsConflict() {
        when(subscriptionRepository.findByUserIdAndCampaignIdAndStatus("u1", 42L, Status.ACTIVE))
                .thenReturn(Optional.of(Subscription.builder().id(7L).build()));

        CreateSubscriptionRequest req = new CreateSubscriptionRequest();
        req.setCampaignId(42L);
        req.setAmount(new BigDecimal("10000"));
        req.setInterval(Interval.DAILY);

        assertThrows(ResponseStatusException.class,
                () -> subscriptionService.subscribe("u1", req));
    }

    @Test
    void subscribe_negativeAmount_rejected() {
        CreateSubscriptionRequest req = new CreateSubscriptionRequest();
        req.setCampaignId(42L);
        req.setAmount(new BigDecimal("-10"));
        req.setInterval(Interval.DAILY);

        assertThrows(ResponseStatusException.class,
                () -> subscriptionService.subscribe("u1", req));
    }

    // update / cancel / resume

    @Test
    void update_changingInterval_resetsNextBillingAt() {
        Subscription existing = Subscription.builder()
                .id(11L).userId("u1").campaignId(42L)
                .amount(new BigDecimal("50000"))
                .interval(Interval.MONTHLY).status(Status.ACTIVE)
                .nextBillingAt(LocalDateTime.now().plusMonths(1))
                .build();
        when(subscriptionRepository.findById(11L)).thenReturn(Optional.of(existing));

        UpdateSubscriptionRequest req = new UpdateSubscriptionRequest();
        req.setInterval(Interval.DAILY);

        Subscription result = subscriptionService.updateSubscription("u1", 11L, req);

        assertEquals(Interval.DAILY, result.getInterval());
        assertTrue(result.getNextBillingAt().isBefore(LocalDateTime.now().plusDays(2)));
    }

    @Test
    void update_notOwner_throwsForbidden() {
        Subscription existing = Subscription.builder()
                .id(11L).userId("someone-else").campaignId(42L)
                .interval(Interval.DAILY).status(Status.ACTIVE)
                .nextBillingAt(LocalDateTime.now().plusDays(1))
                .build();
        when(subscriptionRepository.findById(11L)).thenReturn(Optional.of(existing));

        UpdateSubscriptionRequest req = new UpdateSubscriptionRequest();
        req.setInterval(Interval.WEEKLY);

        assertThrows(ResponseStatusException.class,
                () -> subscriptionService.updateSubscription("u1", 11L, req));
    }

    @Test
    void cancel_setsStatusCancelled() {
        Subscription existing = Subscription.builder()
                .id(11L).userId("u1").campaignId(42L).interval(Interval.DAILY)
                .status(Status.ACTIVE).nextBillingAt(LocalDateTime.now()).build();
        when(subscriptionRepository.findById(11L)).thenReturn(Optional.of(existing));

        Subscription result = subscriptionService.cancel("u1", 11L);

        assertEquals(Status.CANCELLED, result.getStatus());
    }

    @Test
    void resume_fromPaused_returnsActive() {
        Subscription paused = Subscription.builder()
                .id(11L).userId("u1").campaignId(42L).interval(Interval.WEEKLY)
                .status(Status.PAUSED).nextBillingAt(LocalDateTime.now().minusDays(1))
                .lastFailureMessage("Insufficient balance").build();
        when(subscriptionRepository.findById(11L)).thenReturn(Optional.of(paused));

        Subscription result = subscriptionService.resume("u1", 11L);

        assertEquals(Status.ACTIVE, result.getStatus());
        assertNull(result.getLastFailureMessage());
        assertTrue(result.getNextBillingAt().isAfter(LocalDateTime.now()));
    }

    @Test
    void resume_alreadyCancelled_throws() {
        Subscription cancelled = Subscription.builder()
                .id(11L).userId("u1").interval(Interval.DAILY)
                .status(Status.CANCELLED).nextBillingAt(LocalDateTime.now()).build();
        when(subscriptionRepository.findById(11L)).thenReturn(Optional.of(cancelled));

        assertThrows(ResponseStatusException.class,
                () -> subscriptionService.resume("u1", 11L));
    }

    // processDueBillings

    @Test
    void processDueBillings_chargesEachSubscriptionAndAdvancesBillingDate() {
        Subscription s = Subscription.builder()
                .id(1L).userId("u1").campaignId(42L)
                .amount(new BigDecimal("10000"))
                .interval(Interval.DAILY).status(Status.ACTIVE)
                .nextBillingAt(LocalDateTime.now().minusMinutes(5))
                .build();
        when(subscriptionRepository.findByStatusAndNextBillingAtBefore(
                eq(Status.ACTIVE), any(LocalDateTime.class))).thenReturn(List.of(s));

        subscriptionService.processDueBillings();

        verify(walletService).deductForDonation(eq("u1"), anyDouble(), anyString());
        verify(campaignService).recordSuccessfulDonation(eq(42L), any(BigDecimal.class));
        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(captor.capture());
        assertTrue(captor.getValue().getNextBillingAt().isAfter(LocalDateTime.now()));
    }

    @Test
    void processDueBillings_insufficientBalance_pausesSubscription() {
        Subscription s = Subscription.builder()
                .id(1L).userId("u1").campaignId(42L)
                .amount(new BigDecimal("10000000"))
                .interval(Interval.WEEKLY).status(Status.ACTIVE)
                .nextBillingAt(LocalDateTime.now().minusMinutes(1))
                .build();
        when(subscriptionRepository.findByStatusAndNextBillingAtBefore(
                eq(Status.ACTIVE), any(LocalDateTime.class))).thenReturn(List.of(s));
        doThrow(new InsufficientBalanceException("Insufficient"))
                .when(walletService).deductForDonation(anyString(), anyDouble(), anyString());

        subscriptionService.processDueBillings();

        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(captor.capture());
        assertEquals(Status.PAUSED, captor.getValue().getStatus());
        assertEquals("Insufficient", captor.getValue().getLastFailureMessage());
        verify(campaignService, never()).recordSuccessfulDonation(anyLong(), any());
    }

    @Test
    void processDueBillings_campaignNoLongerOpen_autoCancels() {
        Subscription s = Subscription.builder()
                .id(1L).userId("u1").campaignId(42L)
                .amount(new BigDecimal("10000"))
                .interval(Interval.WEEKLY).status(Status.ACTIVE)
                .nextBillingAt(LocalDateTime.now().minusMinutes(1))
                .build();
        when(subscriptionRepository.findByStatusAndNextBillingAtBefore(
                eq(Status.ACTIVE), any(LocalDateTime.class))).thenReturn(List.of(s));
        when(openCampaign.getStatus()).thenReturn(CampaignStatus.CLOSED);

        subscriptionService.processDueBillings();

        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(captor.capture());
        assertEquals(Status.CANCELLED, captor.getValue().getStatus());
        verify(walletService, never()).deductForDonation(anyString(), anyDouble(), anyString());
    }
}
