# Application Wallet — 100% Milestone Design

**Date:** 2026-05-20
**Module:** Application Wallet (`feat/applicationwallet-gunta`)
**Scope:** Milestone 5 (100%) — bulk campaign refund, FRAUD guard, full test coverage

---

## Context

The wallet branch is behind `main` by ~30 commits. All 75% work was already merged into `main` via `integration/milestone75`. The implementation begins with a rebase of `feat/applicationwallet-gunta` onto `origin/main`, then adds the 100% features on top.

`main` already has:
- `CampaignRefundRequestedEvent` fired by `SimpleCampaignService` for both CANCELLED and FRAUD campaigns
- `LoggingCampaignWalletGateway.requestRefund()` — stub that only logs
- `DonationRepository.findByCampaignIdAndStatus()` — available for wallet use
- `Donation.PaymentMethod` enum with `WALLET` and external methods
- `WalletServiceImpl` with `IdrMoney` whole-rupiah math and auto-provisioning

What is missing (the gap this milestone closes):
- No `REFUND` transaction type
- No actual refund logic anywhere
- No wallet event listener for `CampaignRefundRequestedEvent`
- No `/api/internal/wallet/bulk-refund` endpoint
- Incomplete test coverage on wallet controller and service layers

---

## Architecture & Data Flow

```
SimpleCampaignService.processExpiredCampaigns()  [CANCELLED, target not met]
SimpleCampaignService.markAsFraud()              [FRAUD]
  │
  └── fires CampaignRefundRequestedEvent(campaign, amount)
              │
              ▼
       CampaignRefundEventListener           (NEW)
         if campaign.status == FRAUD
           → log.info("Skipping refund for FRAUD campaign {}", id)
           → return
         else
           → walletService.bulkRefundForCampaign(campaign.getId(), campaign.getTitle())
                     │
                     ▼
              WalletServiceImpl.bulkRefundForCampaign()
                → DonationRepository.findByCampaignIdAndStatus(campaignId, SUCCESS)
                → for each Donation where paymentMethod == WALLET:
                    wallet = getWalletByUserId(donation.getUserId())   // auto-provisions if new
                    wallet.balance += IdrMoney.wholeRupiah(donation.getAmount())
                    save Transaction(type=REFUND, amount, "Refund: <campaignName>", now)
                    donation.status = REFUNDED
                    save Donation
                → for each Donation where paymentMethod != WALLET:
                    log.warn("Skipped non-wallet donation id={} method={} userId={}",
                             donation.getId(), donation.getPaymentMethod(), donation.getUserId())
                → return count of refunded donations
```

The `POST /api/internal/wallet/bulk-refund` endpoint on `WalletApiController` exposes the same `bulkRefundForCampaign` call for direct HTTP trigger and controller-layer testing.

---

## Modified Files

| File | Change |
|------|--------|
| `model/TransactionType.java` | Add `REFUND` to enum |
| `service/WalletService.java` | Add `int bulkRefundForCampaign(Long campaignId, String campaignName)` |
| `service/WalletServiceImpl.java` | Implement `bulkRefundForCampaign`; inject `DonationRepository` |
| `controller/WalletApiController.java` | Add `POST /api/internal/wallet/bulk-refund` |

## New Files

| File | Purpose |
|------|---------|
| `event/CampaignRefundEventListener.java` | `@EventListener` on `CampaignRefundRequestedEvent`; FRAUD guard |

---

## API Contract

```
POST /api/internal/wallet/bulk-refund
Content-Type: application/json

Request:
{
  "campaignId": 42,
  "campaignName": "Help Build a School"
}

Response 200:
{
  "success": true,
  "refunded": 3,
  "message": "3 wallet donation(s) refunded for campaign 42."
}

Response 400:  { "success": false, "message": "campaignId and campaignName are required." }
Response 500:  { "success": false, "message": "<error detail>" }
```

---

## Test Plan

### `WalletServiceImplTest` (updated)
- `bulkRefundForCampaign_walletsOnlyRefunded` — 2 WALLET donations → both credited, both marked REFUNDED, 2 REFUND transactions saved
- `bulkRefundForCampaign_nonWalletDonationsSkippedWithLog` — 1 WALLET + 1 GOPAY → only wallet refunded, GOPAY skipped (verify log via `@Captor` or count)
- `bulkRefundForCampaign_noSuccessDonations_returnsZero` — empty list from repo → nothing saved, returns 0
- All existing tests kept; constructor updated to match new `DonationRepository` param

### `CampaignRefundEventListenerTest` (new)
- `fraudCampaign_skipsRefund` — status FRAUD → `walletService.bulkRefundForCampaign` never called
- `cancelledCampaign_triggersRefund` — status CANCELLED → service called with correct campaignId
- `zeroAmount_skipsRefund` — amount == 0 → service not called

### `WalletApiControllerTest` (new)
- `bulkRefund_success_returns200` — valid body → 200, correct JSON
- `bulkRefund_missingCampaignId_returns400`
- `bulkRefund_serviceThrows_returns500`
- Existing `/deduct` and `/balance` tests kept

### `WalletControllerTest` (new)
- `getWalletDashboard_returns200_withWalletAndTransactions`
- `withdraw_success_redirectsWithFlashMessage`
- `withdraw_insufficientBalance_redirectsWithError`
- `withdraw_invalidAmount_redirectsWithError`

---

## Constraints

- FRAUD campaigns: wallet listener skips — no donor money returned
- Only `Donation.PaymentMethod.WALLET` donations are automatically refunded
- Non-wallet donations: logged as warnings, not refunded automatically
- Balance math uses `IdrMoney.wholeRupiah()` throughout — no float arithmetic
- `@Transactional` on `bulkRefundForCampaign` — all-or-nothing per campaign refund run
