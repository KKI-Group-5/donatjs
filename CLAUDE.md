# DonatJS — Claude Session Setup

## Project at a Glance

**DonatJS** is a Spring Boot 3.5 / Java 21 / Thymeleaf donation platform, university group project (CSUI Advanced Programming, KKI-Group-5). Near-complete (Milestone 5 / 100%).

- **GitHub:** https://github.com/KKI-Group-5/donatjs.git
- **Git user:** sirpratama (mnazirpratama@gmail.com)
- **Build:** `./gradlew.bat bootRun` → http://localhost:8080
- **Profiles:** `local` (H2 in-memory) | `supabase` (PostgreSQL via pgBouncer on Cloud Run)
- **H2 console (local only):** http://localhost:8080/h2-console — URL: `jdbc:h2:mem:donatjsdb`, user: `sa`
- **Demo accounts:** `test@donatjs.com` / `password123` and `admin@donatjs.com` / `admin123`

---

## Supabase MCP — Status & Verification

The Supabase MCP is configured globally at `~/.claude/mcp.json`:

```json
{
  "mcpServers": {
    "supabase": {
      "command": "npx",
      "args": [
        "-y",
        "@supabase/mcp-server-supabase@latest",
        "--project-ref", "qyusdglhlnhbemthwobp",
        "--access-token", "<token in mcp.json>"
      ]
    }
  }
}
```

**To verify it loaded:** Check if `mcp__supabase__*` tools are available (e.g. try `list_tables`). If not, the `npx` process failed to start — ask the user to run:

```
! npx -y @supabase/mcp-server-supabase@latest --help
```

If that errors, Node/npx is the problem. Fix it, then restart the Claude Code session.

---

## Database Tables (Supabase PostgreSQL)

These 6 tables are JPA-managed (Hibernate DDL). **Campaigns are NOT in Supabase** — they live in `InMemoryCampaignRepository` (ConcurrentHashMap, resets on redeploy).

| Table | PK | Key columns |
|---|---|---|
| `users` | UUID (auto) | `email` (unique), `password`, `name`, `bio`, `date_of_birth` |
| `wallets` | UUID (auto) | `user_id` (unique), `balance` (Double) |
| `transactions` | UUID (auto) | `wallet_id` (FK), `amount`, `type` (TOPUP/DEDUCTION), `timestamp`, `description` |
| `donations` | BIGSERIAL | `user_id`, `campaign_id`, `type` (ONE_TIME/SUBSCRIPTION), `amount`, `fee`, `total_amount`, `status` (SUCCESS/REJECTED/REFUNDED), `payment_method`, `notes` |
| `saved_campaigns` | BIGSERIAL | `user_id` + `campaign_id` (unique pair), `campaign_title`, `campaign_organizer`, `campaign_image_url`, `saved_at` |
| `subscriptions` | BIGSERIAL | `user_id`, `campaign_id`, `amount`, `frequency` (DAILY/WEEKLY/MONTHLY), `status` (ACTIVE/CANCELLED/TERMINATED), `next_debit_date` |

**Payment fees (hardcoded in `PaymentFee.java`):**
- WALLET → Rp 0
- Bank transfer (BCA/Mandiri/BNI/BRI) → Rp 1,500
- E-wallet (GoPay/OVO/DANA/ShopeePay/LinkAja) → Rp 2,000

---

## Module Map

| Module | Key classes | Notes |
|---|---|---|
| Auth & Profile | `AuthController`, `ProfileService`, `CustomOAuth2UserService` | Spring Security + OAuth2 Google + form login |
| Wallet | `WalletServiceImpl`, `WalletApiController` | Internal API; balance in `Double` |
| Donation | `DonationService` | 5M IDR limit, REJECTED/SUCCESS, wallet debit |
| Campaign | `SimpleCampaignService`, `InMemoryCampaignRepository` | **In-memory only**, not persisted |
| Saved Campaign & Subscription | `SavedCampaignServiceImpl`, `SubscriptionServiceImpl`, `SubscriptionScheduler` | Daily/weekly/monthly auto-debit via scheduler |

**Event bus (Spring `ApplicationEventPublisher`):**
`ProfileUpdatedEvent`, `RejectedDonationEvent`, `CampaignStatusChangedEvent`, `CampaignNearTargetEvent`, `CampaignPayoutRequestedEvent`, `CampaignRefundRequestedEvent`, `CampaignFraudDetectedEvent`

---

## CI/CD

- **CI** (`ci.yml`): `./gradlew test` on every push/PR (ubuntu-22.04, Java 21)
- **CD** (`cd.yml`): push to `main`/`staging` → tests → Docker multi-stage build → GCP Artifact Registry → Cloud Run
- **Secrets (GCP Secret Manager):** `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `RESEND_API_KEY`

---

## Planned Improvements (from risk analysis)

- Redis cache for campaign listings
- Rate limiting on donation/wallet endpoints (Redis counters)
- `SubscriptionDebitRequestedEvent` to decouple Subscription → Wallet
- Extract `SubscriptionScheduler` to standalone Cloud Run Job (GCP Cloud Scheduler)
