package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.dto.UserActivityUpdate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserActivityServiceTest {

    private UserActivityService userActivityService;

    @BeforeEach
    void setUp() {
        userActivityService = new UserActivityService();
    }

    @Test
    void getUserActivities_noActivitiesRecorded_returnsEmptyList() {
        List<UserActivityUpdate> result = userActivityService.getUserActivities("unknown-user");
        assertThat(result).isEmpty();
    }

    @Test
    void recordActivity_thenGetActivities_returnsRecordedActivity() {
        UserActivityUpdate update = new UserActivityUpdate(
                "user-1", "campaign-42",
                UserActivityUpdate.ActivityType.CAMPAIGN,
                UserActivityUpdate.ActivityStatus.FRAUD, null);

        userActivityService.recordActivity(update);

        List<UserActivityUpdate> result = userActivityService.getUserActivities("user-1");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo("user-1");
        assertThat(result.get(0).getActivityType()).isEqualTo(UserActivityUpdate.ActivityType.CAMPAIGN);
    }

    @Test
    void recordActivity_multipleActivities_returnsAllInOrder() {
        UserActivityUpdate first = new UserActivityUpdate("user-2", "d-1",
                UserActivityUpdate.ActivityType.DONATION, UserActivityUpdate.ActivityStatus.REJECTED, null);
        UserActivityUpdate second = new UserActivityUpdate("user-2", "d-2",
                UserActivityUpdate.ActivityType.DONATION, UserActivityUpdate.ActivityStatus.NORMAL, null);

        userActivityService.recordActivity(first);
        userActivityService.recordActivity(second);

        List<UserActivityUpdate> result = userActivityService.getUserActivities("user-2");
        assertThat(result).hasSize(2);
    }

    @Test
    void getUserActivities_differentUsers_isolatesResults() {
        userActivityService.recordActivity(new UserActivityUpdate("user-A", "e1",
                UserActivityUpdate.ActivityType.CAMPAIGN, UserActivityUpdate.ActivityStatus.NORMAL, null));
        userActivityService.recordActivity(new UserActivityUpdate("user-B", "e2",
                UserActivityUpdate.ActivityType.DONATION, UserActivityUpdate.ActivityStatus.FRAUD, null));

        assertThat(userActivityService.getUserActivities("user-A")).hasSize(1);
        assertThat(userActivityService.getUserActivities("user-B")).hasSize(1);
        assertThat(userActivityService.getUserActivities("user-C")).isEmpty();
    void recordActivity_StoresActivityForUser() {
        UserActivityUpdate update = new UserActivityUpdate("user-1", "camp-1", UserActivityUpdate.ActivityType.CAMPAIGN, UserActivityUpdate.ActivityStatus.NORMAL, "Created");
        
        userActivityService.recordActivity(update);
        
        List<UserActivityUpdate> activities = userActivityService.getUserActivities("user-1");
        assertThat(activities).hasSize(1).contains(update);
    }

    @Test
    void getUserActivities_ReturnsEmptyListIfNoActivities() {
        List<UserActivityUpdate> activities = userActivityService.getUserActivities("unknown-user");
        assertThat(activities).isEmpty();
    }

    @Test
    void recordActivity_StoresActivitiesInReverseChronologicalOrder() {
        UserActivityUpdate update1 = new UserActivityUpdate("user-1", "don-1", UserActivityUpdate.ActivityType.DONATION, UserActivityUpdate.ActivityStatus.NORMAL, "First");
        UserActivityUpdate update2 = new UserActivityUpdate("user-1", "don-2", UserActivityUpdate.ActivityType.DONATION, UserActivityUpdate.ActivityStatus.NORMAL, "Second");
        
        userActivityService.recordActivity(update1);
        userActivityService.recordActivity(update2);
        
        List<UserActivityUpdate> activities = userActivityService.getUserActivities("user-1");
        
        assertThat(activities).hasSize(2);
        // addFirst is used in the service, so the last recorded should be first
        assertThat(activities.get(0)).isEqualTo(update2);
        assertThat(activities.get(1)).isEqualTo(update1);
    }

    @Test
    void recordActivity_DifferentUsersAreIsolated() {
        UserActivityUpdate update1 = new UserActivityUpdate("user-1", "don-1", UserActivityUpdate.ActivityType.DONATION, UserActivityUpdate.ActivityStatus.NORMAL, "First");
        UserActivityUpdate update2 = new UserActivityUpdate("user-2", "don-2", UserActivityUpdate.ActivityType.DONATION, UserActivityUpdate.ActivityStatus.NORMAL, "Second");
        
        userActivityService.recordActivity(update1);
        userActivityService.recordActivity(update2);
        
        assertThat(userActivityService.getUserActivities("user-1")).hasSize(1).contains(update1);
        assertThat(userActivityService.getUserActivities("user-2")).hasSize(1).contains(update2);
    }
}
