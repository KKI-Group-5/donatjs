package id.ac.ui.cs.advprog.donatjs.event;

import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import id.ac.ui.cs.advprog.donatjs.repository.UserRepository;
import id.ac.ui.cs.advprog.donatjs.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionDebitFailedEventListener {

    private final UserRepository userRepository;
    private final EmailService emailService;

    // Plain @EventListener (not @TransactionalEventListener) is intentional: the
    // scheduler transaction is marked rollback-only by the inner wallet failure,
    // so an AFTER_COMMIT listener would never fire. The notification is about
    // the failure itself, so it must be delivered regardless of outer txn fate.
    @Async
    @EventListener
    public void handleDebitFailed(SubscriptionDebitFailedEvent event) {
        Optional<String> emailAddr = resolveEmail(event.getUserId());
        if (emailAddr.isEmpty()) {
            log.warn("Skipping insufficient-balance email — user not resolvable. userId={}, subscriptionId={}",
                    event.getUserId(), event.getSubscriptionId());
            return;
        }

        String subject = "Subscription payment failed — insufficient balance";
        String body = buildBody(event);

        emailService.sendPlainText(emailAddr.get(), subject, body);
        log.info("Insufficient-balance email dispatched. userId={}, subscriptionId={}, campaignId={}",
                event.getUserId(), event.getSubscriptionId(), event.getCampaignId());
    }

    private Optional<String> resolveEmail(String userIdString) {
        try {
            return userRepository.findById(Objects.requireNonNull(UUID.fromString(userIdString)))
                    .map(AppUser::getEmail);
        } catch (IllegalArgumentException ex) {
            log.warn("Skipping non-UUID userId on debit-failed event: {}", userIdString);
            return Optional.empty();
        }
    }

    private String buildBody(SubscriptionDebitFailedEvent event) {
        NumberFormat rp = NumberFormat.getNumberInstance(Locale.US);
        return ""
                + "Hi there,\n\n"
                + "We tried to process your recurring subscription donation but couldn't — your wallet balance was too low.\n\n"
                + "  Subscription ID:  " + event.getSubscriptionId() + "\n"
                + "  Campaign ID:      " + event.getCampaignId() + "\n"
                + "  Amount requested: Rp " + rp.format(event.getAmount()) + "\n\n"
                + "The subscription is still active — we'll try again on the next cycle. To avoid further failures, top up your wallet on DonatJS or cancel the subscription if you no longer wish to continue.\n\n"
                + "— DonatJS";
    }
}
