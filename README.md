## Work Plan & Milestones
This project is divided into 5 main milestones. The tasks below detail the development plan for each module. Please add your specific module tasks and assign a Person In Charge (PIC) for each item.

### Milestone 1: Preparation (Due Feb 20)
**Focus:** Initial setup, CI/CD, and basic integration slice.
* [ ] **Campaign Management :** Define campaign lifecycle (statuses, transitions, and rules)
* [ ] **Campaign Management :** Design database schema, API endpoints, and service event interactions
* [ ] **Campaign Management :** Create ERD and state transition diagram

### Milestone 2: 25% Progress (Due March 6)
**Focus:** 
* [ ] **Campaign Management :** Implement campaign creation and viewing (public shows OPEN campaigns only)
* [ ] **Campaign Management :** Implement user actions (edit description and delete before donations)
* [ ] **Campaign Management :** Add validation for deadline, target amount, and required fields

### Milestone 3: 50% Progress
**Focus:** Inter-module communication and secondary features.
* [ ] **Campaign Management :** Implement admin moderation (approve, reject, admin edit)
* [ ] **Campaign Management :** Integrate donation events to update total raised and detect target reached
* [ ] **Campaign Management :** Enforce permission rules and prevent invalid operations

### Milestone 4: 75% Progress
**Focus:** Complex logic, subscriptions, and asynchronous processes.
* [ ] **Campaign Management :** Implement deadline automation (CLOSED if success, CANCELLED if failed)
* [ ] **Campaign Management :** Trigger payout and refund events to wallet service
* [ ] **Campaign Management :** Handle fraud cases and send notification events

### Milestone 5: 100% Progress (Final)
**Focus:** Edge cases, bulk actions, and final polish.
* [ ] **Campaign Management :** Handle concurrency and late-event edge cases
* [ ] **Campaign Management :** Add logging, monitoring, and reliability handling
* [ ] **Campaign Management :** Final integration testing and documentation