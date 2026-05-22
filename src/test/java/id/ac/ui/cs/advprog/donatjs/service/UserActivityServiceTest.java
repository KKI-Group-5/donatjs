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
