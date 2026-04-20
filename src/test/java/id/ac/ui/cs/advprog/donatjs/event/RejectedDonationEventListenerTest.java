package id.ac.ui.cs.advprog.donatjs.event;

import id.ac.ui.cs.advprog.donatjs.dto.DonationResponse;
import id.ac.ui.cs.advprog.donatjs.dto.UserActivityUpdate;
import id.ac.ui.cs.advprog.donatjs.model.Donation;
import id.ac.ui.cs.advprog.donatjs.service.UserActivityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RejectedDonationEventListenerTest {

    @Mock
    private UserActivityService userActivityService;

    @InjectMocks
    private RejectedDonationEventListener listener;

    @Test
    void handleRejectedDonation_recordsRejectedActivity() {
        DonationResponse donation = DonationResponse.builder()
                .id(99L)
                .userId("user-123")
                .campaignId(10L)
                .amount(5_000_001L)
                .status(Donation.DonationStatus.REJECTED)
                .build();

        listener.handleRejectedDonation(new RejectedDonationEvent(this, donation));

        ArgumentCaptor<UserActivityUpdate> captor = ArgumentCaptor.forClass(UserActivityUpdate.class);
        verify(userActivityService).recordActivity(captor.capture());

        assertThat(captor.getValue().getUserId()).isEqualTo("user-123");
        assertThat(captor.getValue().getEntityId()).isEqualTo("99");
        assertThat(captor.getValue().getActivityType()).isEqualTo(UserActivityUpdate.ActivityType.DONATION);
        assertThat(captor.getValue().getStatus()).isEqualTo(UserActivityUpdate.ActivityStatus.REJECTED);
    }
}
