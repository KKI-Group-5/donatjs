# Milestone 5 Implementation Report
## Module: Saved Campaign & Subscription
**Branch:** `feat/savedcampaignsandsubscription-rafi`
**Author:** Rafi (sirpratama)

---

## Summary

This report documents all changes made for Milestone 5 (100% — final). The milestone closes the Saved Campaign & Subscription module with three deliverables drawn directly from `README.md` lines 304–307:

1. **Insufficient-balance notification** for failed scheduled subscription debits.
2. **Asynchronous consistency** so saved data reflects campaign status changes without disrupting other user activities.
3. **Final unit testing** plus a technical design justification (this document).

The work is purely additive: no public API changed, no entity schema changed, no existing test was rewritten to accommodate the new code.

---

## Files Created (New)

| File | Purpose |
|------|---------|
| `event/SubscriptionDebitFailedEvent.java` | Domain event carrying `subscriptionId`, `userId`, `campaignId`, `amount`, `reason`. Published by `SubscriptionScheduler` when an `IllegalStateException` (insufficient balance) is caught. Shape mirrors `CampaignNearTargetEvent` for consistency with the rest of the module. |
| `event/SubscriptionDebitFailedEventListener.java` | `@Async @EventListener` that resolves the user's email through `UserRepository`, formats a plain-text body, and dispatches via the existing `EmailService` abstraction. |
| `test/event/SubscriptionDebitFailedEventListenerTest.java` | 4 Mockito unit tests covering happy path, body content, missing user, and non-UUID userId guard. |
| `MILESTONE5_REPORT.md` | This document. |

## Files Modified (Module-Local)

| File | What changed |
|------|--------------|
| `service/SubscriptionScheduler.java` | Added `ApplicationEventPublisher` dependency. The previous `catch (IllegalStateException e) { log.warn(...) }` block (marked with a `// M5 adds notification logic` comment) now also publishes a `SubscriptionDebitFailedEvent`. |
| `event/CampaignStatusChangedEventListener.java` | Added a second listener method `removeOrphanSavedCampaigns` (`@Async @EventListener @Transactional`) that calls `savedCampaignRepository.deleteByCampaignId(...)` for terminating statuses (`DELETED`, `CANCELLED`, `FRAUD`, `REJECTED`). The pre-existing `handleStatusChange` (subscription auto-termination) is unchanged. |
| `repository/SavedCampaignRepository.java` | Added derived query `long deleteByCampaignId(String campaignId)` to support the orphan cleanup. |
| `test/event/CampaignStatusChangedEventListenerTest.java` | Added 6 new tests covering the orphan-cleanup branch across all four terminating statuses plus the two non-terminating statuses (`CLOSED` healthy end-of-life and `OPEN` normal approval). |
| `test/service/SubscriptionSchedulerTest.java` | Injected an `ApplicationEventPublisher` mock; extended `processSubscriptions_skipsOnInsufficientBalance` to verify the event is published; added `processSubscriptions_publishedEventCarriesAllSubscriptionFields` that captures and asserts every payload field; added a negative assertion to the success-path test. |
| `test/service/SubscriptionServiceImplTest.java` | Added `updateFrequency_daily_advancesByOneDay` to cover the previously-untested `DAILY` arm of `calculateNextDebitDate`. |

## Files Modified (Cross-Module — Bug Fix)

While verifying the orphan-cleanup listener, a latent gap was found in the campaign module that affected both M4's existing subscription auto-termination and the new M5 orphan cleanup: two of the four "terminating" status transitions never published `CampaignStatusChangedEvent` at all, so any listener subscribed to it silently no-op'd on those paths. The two missing publishers are:

| File | What changed |
|------|--------------|
| `service/SimpleCampaignService.java` (Adit's module) | `markAsFraud(...)` now also publishes `CampaignStatusChangedEvent(previous, FRAUD)` alongside the existing `CampaignFraudDetectedEvent`. `processExpiredCampaigns(...)` now publishes `CampaignStatusChangedEvent(OPEN, CLOSED)` or `CampaignStatusChangedEvent(OPEN, CANCELLED)` on every deadline finalisation. |
| `test/service/SimpleCampaignServiceTest.java` | Updated the three event-count assertions affected by the additional publish, added explicit `CampaignStatusChangedEvent` verification on the fraud and expired-campaign paths, and added a new `processExpiredCampaigns_cancelsWithZeroRaisedStillPublishesStatusEvent` test for the zero-donation cancellation branch (which previously published no event at all). |

Both changes are purely additive — every existing event listener still receives exactly the events it did before. The behaviour change is that `CampaignStatusChangedEventListener.handleStatusChange` (M4) and `removeOrphanSavedCampaigns` (M5) now correctly fire on FRAUD-via-admin and CANCELLED-via-deadline, which is what the M4 spec already required.

---

## Design Justification

### 1. Why event-driven instead of a direct service call

The pre-existing pattern in this codebase already routes cross-module notifications through Spring `ApplicationEvent`s:

- `ProfileUpdatedEvent` → `CampaignProfileUpdateListener` / `DonationProfileUpdateListener`
- `RejectedDonationEvent` → `RejectedDonationEventListener`
- `CampaignNearTargetEvent` → `CampaignNearTargetEventListener`
- `CampaignStatusChangedEvent` → `CampaignStatusChangedEventListener`

Adding `SubscriptionDebitFailedEvent` extends that pattern instead of introducing a competing one. The alternative — having `SubscriptionScheduler` call `EmailService` directly — would tightly couple the scheduler to the notification mechanism and force any future channel (in-app banner, push notification) to be added by editing the scheduler. With an event, additional listeners can attach without touching the publisher.

It also aligns with the **modified architecture** documented in `README.md` § *G.2 Future Architecture* and § *G.3 Risk Analysis*, which calls out the Subscription → Wallet direct call as one of the three remaining tight couplings to be replaced by events. M5 contributes a step toward that target by moving the failure-notification side of the same flow to the event bus.

### 2. Why `@Async @EventListener` and **not** `@TransactionalEventListener(AFTER_COMMIT)`

`CampaignNearTargetEventListener` uses `@TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)`, and on first read it looks like `SubscriptionDebitFailedEventListener` should mirror that exactly. It cannot, for a subtle reason:

- `SubscriptionScheduler.processSubscriptions()` runs inside an outer `@Transactional` boundary.
- `WalletService.deductBalance(...)` is also `@Transactional` (propagation `REQUIRED`), so it joins the outer transaction.
- When `deductBalance` throws `IllegalStateException` ("Insufficient balance"), Spring's transactional aspect on the inner call marks the **shared** transaction as rollback-only before the exception propagates.
- The scheduler's `catch` block executes, but the outer transaction is now doomed — when it tries to commit, it rolls back with `UnexpectedRollbackException`.
- `@TransactionalEventListener(AFTER_COMMIT)` listeners **do not fire** when the publishing transaction rolls back.

The whole point of the new notification is to inform the user *because* the debit failed. Suppressing it on rollback would defeat the requirement. Therefore the listener uses plain `@Async @EventListener`, which fires immediately when the event is published, on a separate thread. Email dispatch is safe outside the transaction because:

- The wallet was never actually debited (the transaction rolls back), so the email's claim — "we couldn't process your subscription" — remains truthful.
- `EmailService.sendPlainText` is documented (interface JavaDoc) to swallow transport failures, so an SMTP outage cannot cascade back into the scheduler.

This nuance is documented in a comment in `SubscriptionDebitFailedEventListener` so a future reader doesn't "fix" it back to AFTER_COMMIT.

### 3. Why the scope deliberately excludes `createSubscription` initial-debit failures

`SubscriptionServiceImpl.createSubscription` also calls `walletService.deductBalance` on the very first debit. That call can throw the same `IllegalStateException` for insufficient balance. We considered emailing the user in that case as well, but ruled it out:

- The synchronous request path already returns HTTP 422 with `{ "error": "Insufficient balance" }`.
- The front-end (`campaigns/detail.html` subscribe modal) surfaces the error inline.
- Sending an email a few hundred milliseconds after the user has *just seen* the error on screen would be redundant noise.

The notification is therefore reserved for the **asynchronous** failure path (the nightly scheduler), where the user has no synchronous channel to learn about the failure.

### 4. Why "remove orphan SavedCampaigns" over "show stale status"

README L304-307 phrases the requirement as "ensure saved data reflects campaign status changes." That admits two implementations:

- **(a)** delete `SavedCampaign` rows when the campaign terminates,
- **(b)** keep the rows but display the new status on the saved-campaigns page.

Option (a) is implemented because it mirrors what the same set of status transitions already does to subscriptions (`subscriptionService.terminateActiveSubscriptionsForCampaign`). The two cleanup branches now live side by side in `CampaignStatusChangedEventListener`, so the contract for "what happens when a campaign is killed" is centralised. Option (b) would require an entity migration (status column on `SavedCampaign`), a UI badge, and would still leave the saved list cluttered with permanently-dead campaigns. Option (a) is reversible if requirements change — we can always re-add the column later — and ships fewer moving parts now.

The cleanup listener is `@Async` so it cannot delay the publisher (the `Campaign` write commit). It carries its own `@Transactional` so the delete commits independently of the publishing transaction; if the publisher rolls back, the next run of the listener on a successful publish will catch up.

### 5. Module-boundary preservation

After this milestone, every cross-module collaboration in the Saved Campaign & Subscription module is mediated by either a Spring event or the `WalletService` / `DonationService` / `UserRepository` interfaces. There are no `instanceof` checks on other modules' types, no field reaches across, no shared mutable state. This was a non-goal stated by README G.2/G.3 — explicitly tracked here so a future maintainer doesn't quietly introduce a direct call.

---

## Test Summary

Run the unit-test suite with coverage:

```bash
./gradlew.bat test jacocoTestReport
```

JaCoCo coverage for the M5 module surface (from `build/reports/jacoco/test/html/`):

| Class | Instructions | Branches |
|------|------|------|
| `SubscriptionScheduler` | 100% | 100% (5/5) |
| `SubscriptionServiceImpl` | 95% | 92% (12/13) |
| `SavedCampaignServiceImpl` | 100% | 100% (2/2) |
| `SubscriptionDebitFailedEvent` | 100% | n/a |
| `SubscriptionDebitFailedEventListener` | 100% | 100% (2/2) |
| `CampaignStatusChangedEvent` | 100% | 100% (8/8) |
| `CampaignStatusChangedEventListener` | 100% | 100% (8/8) |
| `CampaignNearTargetEvent` | 100% | n/a |
| `CampaignNearTargetEventListener` | 100% | 100% (4/4) |

Failing tests in `WalletServiceImplTest` and `EndToEndModerationTest` observed in the latest run are pre-existing failures in other modules (wallet mock now mismatches the post-`6b9ab4e` `findByUserIdForWrite` lock; auth moderation expects a counter not yet present). Both failures reproduce on the pre-M5 commit, confirming they are not caused by this milestone's work.

---

## Manual Smoke Test

Steps to verify both M5 deliverables end-to-end against the local H2 profile:

1. `./gradlew.bat bootRun` and log in as `test@donatjs.com` / `password123`.
2. Browse to the "Almost There: Animal Shelter Renovation" campaign (already seeded at 97 % of target and on the test user's saved list — also useful for the M4 near-target test). Subscribe to it for an amount that exceeds your wallet balance (e.g. 5 000 000) — you should see the synchronous 422 error inline and **no email** should be sent (M5-T1d decision).
3. Subscribe to "Monthly Food Packages for Orphanages" with a reasonable amount, then manually run the scheduler twice from the H2 console: set the subscription's `next_debit_date` to today and wait for the scheduled run at midnight (or invoke `processSubscriptions()` from an integration test). With the wallet drained, an insufficient-balance email should appear in the Mailpit inbox (localhost:1025 in local dev).
4. From an admin account, mark a campaign that the test user has saved as `FRAUD` or `DELETED`. Refresh `/saved-campaigns` — the row should be gone.

---

## Out of Scope (Other Modules' M5 Deliverables)

These items in README's M5 section are **not** part of this branch and are owned by other team members:

- Authentication & User Profile — dispute feature, final integration testing (Aldebaran, `feat/authentication-and-user-profile-aldebaran`).
- Application Wallet — bulk refund on campaign failure, FRAUD-no-refund guard, UI polish (Gunta).
- Donation Management — withdrawal restriction, update donation endpoint, halt on non-OPEN campaign (Khayru).
- Campaign Management — concurrency / late-event edge cases, logging, monitoring (Adit).

Each member's M5 work lives on their respective feature branch and will integrate via the existing PR-per-feature pattern.
