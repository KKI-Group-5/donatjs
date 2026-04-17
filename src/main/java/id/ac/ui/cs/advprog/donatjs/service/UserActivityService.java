package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.dto.UserActivityUpdate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class UserActivityService {

    private final Map<String, ConcurrentLinkedDeque<UserActivityUpdate>> userActivities = new ConcurrentHashMap<>();

    public void recordActivity(UserActivityUpdate update) {
        userActivities.computeIfAbsent(update.getUserId(), ignored -> new ConcurrentLinkedDeque<>())
                .addFirst(update);
    }

    public List<UserActivityUpdate> getUserActivities(String userId) {
        ConcurrentLinkedDeque<UserActivityUpdate> activities = userActivities.get(userId);
        if (activities == null) {
            return Collections.emptyList();
        }
        return List.copyOf(activities);
    }
}
