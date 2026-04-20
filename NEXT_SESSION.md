# DonatJS — Next Session Handoff

## Context

This is a Spring Boot crowdfunding app (Java 21, Gradle Kotlin DSL, Thymeleaf, Spring Security, PostgreSQL via Supabase) for the Adpro course at UI. The working directory is `C:\Users\Rafi\OneDrive\Documents\CSUI24\Sem 4\Adpro\donatjs`.

## What Was Done In The Previous Session

We ran a comprehensive audit of the `staging` branch (which includes the merged `feat/campaignmanagement-adit` branch, commit `3c4c438`). The following bugs were found and **fixed**:

### Fixes Applied (all committed/pushed)
1. **`DonationController` ClassCastException** — replaced direct `(AppUser)` cast with a `resolveUserId(Authentication)` helper that handles both `OAuth2User` (Google) and `UserDetails` (form login) principals.
2. **`CreateDonationRequest` @NotNull on `userId`** — removed the `@NotNull` constraint since `userId` is set programmatically from auth after validation runs.
3. **`campaigns/detail.html` missing status badges** — added CSS + `th:classappend` logic for `REJECTED` and `DELETED` statuses.
4. **`index.html` broken CTA button** — "Start a Campaign" button was missing `th:href`.
5. **`DonationServiceTest.java` truncated** — file was cut off mid-method; completed the missing test and fixed `UnnecessaryStubbingException`.
6. **`application.properties` — PostgreSQL/Supabase pgBouncer fix** — added `spring.datasource.hikari.data-source-properties.prepareThreshold=0` to disable server-side prepared statements (required because Supabase uses pgBouncer at port 6543 in transaction mode).
7. **`DonationController` — subscription+bank 500→400** — added `@ExceptionHandler` for `IllegalArgumentException` and `IllegalStateException` so invalid donation requests return proper 400/409 instead of 500.
8. **Supabase DB constraint** — `donations_status_check` was missing `REFUNDED`. Fixed directly in Supabase via `ALTER TABLE donations DROP CONSTRAINT donations_status_check; ALTER TABLE donations ADD CONSTRAINT donations_status_check CHECK (status = ANY (ARRAY['SUCCESS','REJECTED','REFUNDED']));`

### Verified Working
- Donation creation (SUCCESS / REJECTED for over-5M / proper 400 for subscription+non-wallet)
- Campaign create → WAITING → moderate to OPEN → edit description → admin-edit → soft-delete
- `GET /api/profile/me` — returns logged-in user's profile, donation history
- `GET /api/donations/{id}`, `GET /api/donations/campaign/{id}`, `GET /api/donations/campaign/{id}/total`
- `PATCH /api/donations/{id}/notes`
- `POST /api/donations/campaign/{id}/refund`
- `GET /wallet` page renders (with hardcoded user — see known issues)
- 96 tests passing, 0 failures

---

## What Is Left To Do

### 1. Wire Campaign Creator from Auth Session (Medium)
**File:** `src/main/java/.../controller/CampaignController.java:51`

The `createCampaign` endpoint reads the creator ID from an `X-User-Id` header:
```java
@RequestHeader(value = "X-User-Id", required = false) String userId
```
This is never set by the Thymeleaf form, so `creatorId` is always `null`. It needs to resolve the logged-in user from `Authentication` (same pattern as `DonationController.resolveUserId()`).

**Impact:** Campaign ownership is broken — no user "owns" their campaigns, so the `deleteIfNoDonations` permission check (`validateActorPermission`) is bypassed by anyone.

### 2. Wire Wallet to Authenticated User (Medium)
**File:** `src/main/java/.../controller/WalletController.java:23`

```java
private static final String CURRENT_USER_ID = "user-demo-001";
```
The wallet page always shows the balance/transactions for `user-demo-001` regardless of who is logged in. This needs to resolve the real user's ID from `Authentication`, look up their UUID via `UserRepository`, and pass it to `WalletService`.

**Impact:** Every logged-in user sees the same fake wallet.

### 3. Fix `index.html` Hardcoded Campaign Cards (Low/Medium)
**File:** `src/main/resources/templates/index.html`

The homepage has 6 hardcoded static campaign cards (HTML). These should be replaced with dynamic data from `CampaignService.findOpenCampaigns()` (same as the `/campaigns` list page does). Also, the JavaScript at the bottom hardcodes `const CURRENT_USER_ID = 'user-demo-001'` for the save-campaign buttons.

### 4. Fix `campaign.totalRaised` Not Being Updated (Medium)
**File:** `src/main/java/.../service/DonationService.java:80`

There is a `// TODO: campaignService.updateTotalRaised(...)` comment. When a donation succeeds, the campaign's `totalRaised` field (in-memory) is never incremented. This has two consequences:
- Campaign progress bars always show 0%
- The `deleteIfNoDonations` check always passes (since `totalRaised == 0`), allowing deletion of campaigns that have active donations

The fix: after saving a `SUCCESS` donation, call `campaignService.recordSuccessfulDonation(saved.getCampaignId(), saved.getAmount())`.

### 5. Add `server.servlet.session.cookie.secure=false` for Local Dev (Low)
**File:** `src/main/resources/application.properties`

`server.servlet.session.cookie.secure=true` is correct for prod (Koyeb/HTTPS) but makes local HTTP testing with curl awkward. Consider using Spring profiles: keep `secure=true` in the base `application.properties` and override with `secure=false` in `application-local.properties` (which already exists via `LocalDataInitializer` being `@Profile("local")`).

### 6. Verify Test Count Discrepancy (Low)
Previous session had 97 tests; after the `DonationController` changes we have 96. Check if a `DonationControllerTest` test for the old behavior was invalidated by the new `@ExceptionHandler` handlers and either update or restore it.

Run: `./gradlew test` and check `build/test-results/test/*.xml` for anything skipped.

---

## Architecture Reminders

- **`Campaign` is NOT a JPA entity** — uses `InMemoryCampaignRepository` (resets on restart). This is intentional for Milestone 50%.
- **DB is Supabase PostgreSQL at port 6543** (pgBouncer, transaction mode). The `prepareThreshold=0` fix is required and is in `application.properties`.
- **Auth principals:** `CustomUserDetailsService` returns Spring's `User` (not `AppUser`). To get the real user, resolve by email via `UserRepository.findByEmail()`. See `DonationController.resolveUserId()` for the pattern.
- **`application.properties` uses `.env`** loaded by `co.uzzu.dotenv.gradle` — only active when running via `./gradlew bootRun`.

---

## How to Run

```bash
cd "C:\Users\Rafi\OneDrive\Documents\CSUI24\Sem 4\Adpro\donatjs"
./gradlew bootRun       # starts server at http://localhost:8080
./gradlew test          # runs all 96 tests
./gradlew compileJava   # trigger DevTools hot reload after Java edits
./gradlew processResources  # trigger DevTools reload after .properties edits
```

Test credentials (form login — must register first via `POST /api/auth/register`):
```json
{ "email": "qatest@donatjs.com", "password": "qatest123", "name": "QA Tester" }
```
