## Work Plan & Milestones

This project is divided into 5 main milestones. The tasks below detail the development plan for the **Application Wallet** module.

### Milestone 1: Preparation (Due Feb 20)
**Focus:** Initial setup and basic integration slice.
* [ ] Create `Wallet` and `Transaction` Entities.
* [ ] Implement a basic endpoint to view a dummy wallet balance to prove Database -> Backend -> Frontend integration.

### Milestone 2: 25% Progress (Due March 6)
**Focus:** Core CRUD operations and independent module functionality.
* [ ] Implement the "View Wallet Balance" use case for authenticated users.
* [ ] Implement the "View Wallet Transaction History" use case.
* [ ] Build the UI/Frontend pages for the Wallet dashboard and transaction history.

### Milestone 3: 50% Progress
**Focus:** Inter-module communication and secondary features.
* [ ] Implement the "Withdraw Funds" use case, including withdrawal notifications and balance deduction.
* [ ] Create the internal API/Service layer to allow the **Donation Module** to deduct balances when a user makes a donation.
* [ ] Ensure balance never goes negative during standard donation deductions.

### Milestone 4: 75% Progress
**Focus:** Complex logic, subscriptions, and asynchronous processes.
* [ ] Implement automatic subscription debits.
* [ ] Create the logic to send an immediate notification if a user's balance is insufficient for an upcoming subscription debit.
* [ ] Establish the event-driven communication (or API structure) to listen for Campaign status changes (specifically `CANCELLED` or failed deadlines).

### Milestone 5: 100% Progress (Final)
**Focus:** Edge cases, bulk actions, and final polish.
* [ ] Implement the automatic bulk refund mechanism for when a campaign fails to reach its target or is cancelled.
* [ ] Ensure campaigns with a `FRAUD` status *do not* trigger automatic refunds, per system constraints.
* [ ] Final UI/UX polish, bug fixing, and ensuring 100% test coverage for wallet services.