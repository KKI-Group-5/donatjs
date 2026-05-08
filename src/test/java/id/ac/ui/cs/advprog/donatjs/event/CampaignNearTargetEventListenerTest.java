package id.ac.ui.cs.advprog.donatjs.event;

import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import id.ac.ui.cs.advprog.donatjs.model.SavedCampaign;
import id.ac.ui.cs.advprog.donatjs.repository.SavedCampaignRepository;
import id.ac.ui.cs.advprog.donatjs.repository.UserRepository;
import id.ac.ui.cs.advprog.donatjs.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class CampaignNearTargetEventListenerTest {

    @Mock private SavedCampaignRepository savedCampaignRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;

    @InjectMocks
    private CampaignNearTargetEventListener listener;

    @Test
    void handleNearTarget_emailsAllUsersWithCampaignSaved() {
        UUID userIdA = UUID.randomUUID();
        UUID userIdB = UUID.randomUUID();

        when(savedCampaignRepository.findByCampaignId("42")).thenReturn(List.of(
                saved(userIdA), saved(userIdB)
        ));
        when(userRepository.findById(userIdA)).thenReturn(Optional.of(appUser(userIdA, "alice@example.com")));
        when(userRepository.findById(userIdB)).thenReturn(Optional.of(appUser(userIdB, "bob@example.com")));

        listener.handleNearTarget(new CampaignNearTargetEvent(this, 42L,
                "Test Campaign", new BigDecimal("980"), new BigDecimal("1000")));

        ArgumentCaptor<String> recipient = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        verify(emailService, times(2)).sendPlainText(recipient.capture(), subject.capture(), anyString());

        assertThat(recipient.getAllValues()).containsExactlyInAnyOrder("alice@example.com", "bob@example.com");
        assertThat(subject.getAllValues()).allMatch(s -> s.contains("Test Campaign"));
    }

    @Test
    void handleNearTarget_emitsNothingWhenNoSavedFavorites() {
        when(savedCampaignRepository.findByCampaignId("99")).thenReturn(List.of());

        listener.handleNearTarget(new CampaignNearTargetEvent(this, 99L,
                "Lonely campaign", new BigDecimal("980"), new BigDecimal("1000")));

        verify(emailService, never()).sendPlainText(anyString(), anyString(), anyString());
    }

    @Test
    void handleNearTarget_skipsUsersThatNoLongerExist() {
        UUID userId = UUID.randomUUID();
        when(savedCampaignRepository.findByCampaignId("42")).thenReturn(List.of(saved(userId)));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        listener.handleNearTarget(new CampaignNearTargetEvent(this, 42L,
                "Test", new BigDecimal("980"), new BigDecimal("1000")));

        verify(emailService, never()).sendPlainText(anyString(), anyString(), anyString());
    }

    @Test
    void handleNearTarget_skipsNonUuidUserIdsGracefully() {
        SavedCampaign sc = SavedCampaign.builder()
                .userId("not-a-uuid")
                .campaignId("42")
                .campaignTitle("t")
                .build();
        when(savedCampaignRepository.findByCampaignId("42")).thenReturn(List.of(sc));

        listener.handleNearTarget(new CampaignNearTargetEvent(this, 42L,
                "Test", new BigDecimal("980"), new BigDecimal("1000")));

        verify(emailService, never()).sendPlainText(anyString(), anyString(), anyString());
        verify(userRepository, never()).findById(any(UUID.class));
    }

    @Test
    void handleNearTarget_bodyIncludesRaisedAndTargetNumbers() {
        UUID userId = UUID.randomUUID();
        when(savedCampaignRepository.findByCampaignId("42")).thenReturn(List.of(saved(userId)));
        when(userRepository.findById(userId)).thenReturn(Optional.of(appUser(userId, "x@example.com")));

        listener.handleNearTarget(new CampaignNearTargetEvent(this, 42L,
                "Test", new BigDecimal("980000"), new BigDecimal("1000000")));

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPlainText(eq("x@example.com"), anyString(), body.capture());
        assertThat(body.getValue()).contains("980,000");
        assertThat(body.getValue()).contains("1,000,000");
    }

    private static SavedCampaign saved(UUID userId) {
        return SavedCampaign.builder()
                .userId(userId.toString())
                .campaignId("42")
                .campaignTitle("Test")
                .build();
    }

    private static AppUser appUser(UUID id, String email) {
        AppUser u = new AppUser();
        // AppUser fields are private with no setId; use reflection to set id for the test only.
        try {
            Field idField = AppUser.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(u, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        u.setEmail(email);
        return u;
    }
}
