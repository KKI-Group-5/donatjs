# Milestone 3 Implementation Report
## Module: Saved Campaign & Subscription
**Branch:** `feat/savedcampaignsandsubscription-rafi`
**Author:** Rafi (sirpratama)

---

## Summary

This report documents all changes made for Milestone 3 (50% progress). The milestone implements the full Subscription engine: creating subscriptions, enforcing wallet-only payment, changing or cancelling subscriptions, and a scheduled daily debit engine.

---

## Files Created (New)

| File | Description |
|------|-------------|
| `model/Subscription.java` | JPA entity for subscriptions. Contains nested enums `SubscriptionFrequency` (DAILY, WEEKLY, MONTHLY) and `SubscriptionStatus` (ACTIVE, CANCELLED, TERMINATED). `campaignId` stored as plain `Long` — Campaign is not a JPA entity. |
| `repository/SubscriptionRepository.java` | Spring Data JPA repository with derived queries for finding due subscriptions and checking for duplicate active subscriptions. |
| `dto/CreateSubscriptionRequest.java` | Request DTO for creating a subscription (userId, campaignId, amount, frequency). |
| `dto/SubscriptionResponse.java` | Response DTO with a static `from(Subscription)` factory method following the `DonationResponse` pattern. |
| `dto/UpdateSubscriptionRequest.java` | Request DTO for changing subscription frequency. |
| `service/SubscriptionService.java` | Service interface defining createSubscription, cancelSubscription, updateFrequency, getSubscriptionsByUser. |
| `service/SubscriptionServiceImpl.java` | Service implementation. Enforces wallet-only payment, duplicate-active guard, immediate first debit, donation record creation. |
| `service/SubscriptionScheduler.java` | `@Scheduled(cron = "0 0 0 * * *")` component that runs daily at midnight to process due subscriptions. Skips gracefully on insufficient balance (M5 adds notification). |
| `controller/SubscriptionController.java` | REST controller at `/api/subscriptions/**` (POST create, DELETE cancel, PATCH frequency, GET by user). |
| `templates/subscriptions.html` | Thymeleaf page at `/subscriptions/{userId}` showing active/past subscriptions with cancel and frequency-change controls. |
| `test/service/SubscriptionServiceImplTest.java` | Unit tests for the service (Mockito, 9 test cases). |
| `test/service/SubscriptionSchedulerTest.java` | Unit tests for the scheduler engine (Mockito, 5 test cases). |
| `test/controller/SubscriptionControllerTest.java` | Web layer tests for the controller (@WebMvcTest, 8 test cases). |

---

## Files Modified (Shared — Needs Team Awareness)

### 1. `model/TransactionType.java` — **Wallet Module (Gunta)**
**Change:** Added `SUBSCRIPTION` enum value.
```java
// Before
public enum TransactionType { DEPOSIT, WITHDRAWAL, DONATION }

// After
public enum TransactionType { DEPOSIT, WITHDRAWAL, DONATION, SUBSCRIPTION }
```
**Why:** Subscription debits must be distinguishable from regular donations in the transaction history.
**Impact:** Additive change. Does not break any existing code. Gunta should be aware when he merges.

---

### 2. `service/WalletService.java` — **Wallet Module (Gunta)**
**Change:** Added `deductBalance` method to the interface.
```java
Wallet deductBalance(String userId, double amount, String description);
```
**Why:** The Subscription module needs to deduct from a user's wallet when a subscription is created or debited. The existing interface had no deduction capability.
**Impact:** Any class that implements `WalletService` must now implement `deductBalance`. Currently only `WalletServiceImpl` implements it — already updated. If Gunta adds another implementation in future, he must add `deductBalance` there too.

---

### 3. `service/WalletServiceImpl.java` — **Wallet Module (Gunta)**
**Change:** Implemented `deductBalance`.
```java
@Override
public Wallet deductBalance(String userId, double amount, String description) {
    Wallet wallet = walletRepository.findByUserId(userId)
            .orElseThrow(() -> new RuntimeException("Wallet not found for user: " + userId));
    if (wallet.getBalance() < amount) {
        throw new IllegalStateException("Insufficient balance");
    }
    wallet.setBalance(wallet.getBalance() - amount);
    walletRepository.save(wallet);
    transactionRepository.save(Transaction.builder()
            .wallet(wallet).amount(amount)
            .type(TransactionType.SUBSCRIPTION)
            .description(description)
            .timestamp(LocalDateTime.now())
            .build());
    return wallet;
}
```
**Why:** Fulfills the interface contract added above.
**Impact:** Gunta should review this logic and confirm it aligns with his module's planned withdrawal/deduction feature for M3 (to avoid duplicating logic or conflicting implementations).

---

### 4. `DonatJsApplication.java` — **Shared (Everyone)**
**Change:** Added `@EnableScheduling` annotation.
```java
@SpringBootApplication
@EnableScheduling  // ← new
public class DonatJsApplication { ... }
```
**Why:** Required to activate the `@Scheduled` annotation in `SubscriptionScheduler`.
**Impact:** **All team members are affected.** After merging, the scheduler will be active in every local dev environment. The scheduler runs at midnight and only processes subscriptions in the database — so it is harmless in dev (there will be zero subscriptions to process). However, if any team member's environment has the scheduler fire unexpectedly, it is safe to ignore.

---

### 5. `controller/PageController.java` — **This Module (Rafi)**
**Change:** Added `SubscriptionService` dependency and `/subscriptions/{userId}` route.
```java
@GetMapping("/subscriptions/{userId}")
public String subscriptionsPage(@PathVariable String userId, Model model) {
    List<SubscriptionResponse> subscriptions = subscriptionService.getSubscriptionsByUser(userId);
    model.addAttribute("subscriptions", subscriptions);
    model.addAttribute("userId", userId);
    return "subscriptions";
}
```
**Why:** Serves the Thymeleaf subscriptions page.
**Impact:** `PageController` now requires a second constructor argument (`SubscriptionService`). Any `@WebMvcTest` for `PageController` must add `@MockitoBean SubscriptionService subscriptionService`. There are currently no tests for `PageController` — no action needed.

---

## API Endpoints Added

| Method | URL | Description | Success | Error |
|--------|-----|-------------|---------|-------|
| `POST` | `/api/subscriptions` | Create a subscription | 201 Created | 409 Conflict (duplicate active), 422 Unprocessable (insufficient balance) |
| `DELETE` | `/api/subscriptions/{id}?userId=...` | Cancel a subscription | 200 OK | 404 Not Found, 403 Forbidden (wrong user) |
| `PATCH` | `/api/subscriptions/{id}/frequency?userId=...` | Change frequency | 200 OK | 404 Not Found, 403 Forbidden |
| `GET` | `/api/subscriptions/user/{userId}` | List user's subscriptions | 200 OK | — |
| `GET` | `/subscriptions/{userId}` | Subscription management page (Thymeleaf) | 200 OK | — |

**Security:** All `/api/subscriptions/**` endpoints require authentication (covered by `.anyRequest().authenticated()` in `SecurityConfig`). No change to `SecurityConfig` was needed.

---

## Business Rules Implemented

1. **Wallet-only constraint** — Subscriptions always deduct from the internal wallet. There is no payment method selection; `WALLET` is hardcoded in `SubscriptionServiceImpl` and passed to `DonationService`.
2. **Duplicate active guard** — A user cannot create a second active subscription to the same campaign. Returns 409.
3. **Immediate first debit** — On subscription creation, the first payment is deducted immediately and a `Donation` record is created.
4. **Recurring debit engine** — `SubscriptionScheduler` runs at midnight daily, finds all ACTIVE subscriptions where `nextDebitDate <= today`, deducts from wallet, records a donation, and advances `nextDebitDate` by the subscription's frequency.
5. **Graceful insufficient-balance skip** — If a user has insufficient balance during the scheduler run, the subscription is skipped (not terminated). M5 adds the notification logic for this case.
6. **Ownership enforcement** — Cancel and frequency-change operations verify the `userId` matches the subscription owner; returns 403 otherwise.

---

## Donation Record Linkage

Every subscription debit — both the initial creation and each scheduler-triggered debit — creates a `Donation` record via `DonationService` with:
- `type = SUBSCRIPTION`
- `paymentMethod = WALLET`
- `status = SUCCESS` (or `REJECTED` if amount > Rp 5,000,000, per existing DonationService limit)

This means subscription debits appear in the donation history (Khayru's module) and are visible via `GET /api/donations/user/{userId}`.

---

## Pre-existing Test Failures (Not Caused by This Work)

The full test suite shows **3 failing tests** in `CampaignControllerMvcTest`. These failures exist in the `main` branch before any Milestone 3 code was written (confirmed via `git log`). They are Adit's responsibility:
- `getCampaigns_returnsOnlyOpenCampaignsInModel`
- `getCampaign_notFound_returns404`
- `postCreate_withBlankRequiredFields_returnsCreateViewWithErrors`

**Root cause:** The test uses deprecated `@MockBean` and lacks `@WithMockUser`, causing Spring Security to reject the requests.

---

## What Each Teammate Needs to Know

| Person | Action Required |
|--------|----------------|
| **Gunta** (Wallet) | Review `deductBalance` implementation in `WalletServiceImpl`. Coordinate to avoid duplicate deduction logic in his own M3 wallet withdrawal feature. `TransactionType.SUBSCRIPTION` added. |
| **Khayru** (Donation) | No action needed. Subscription debits will appear in `GET /api/donations/user/{userId}` as `type=SUBSCRIPTION` records. |
| **Adit** (Campaign) | Fix `CampaignControllerMvcTest` — add `@WithMockUser` and replace deprecated `@MockBean` with `@MockitoBean`. |
| **Aldebaran** (Auth) | No action needed. The `/api/subscriptions/**` endpoints are auth-protected by the existing `anyRequest().authenticated()` rule. |
| **Everyone** | `@EnableScheduling` is now active globally. The scheduler runs at midnight but is safe in dev (no data = nothing processed). |

---

## Test Results

```
My new tests:   22 passed, 0 failed
Full suite:     86 passed, 3 failed (3 pre-existing failures in CampaignControllerMvcTest — not my code)
```
