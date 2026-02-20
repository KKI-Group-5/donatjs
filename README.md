## Work Plan & Milestones
This project is divided into 5 main milestones. The tasks below detail the development plan for each module.

### Milestone 1: Preparation (Due Feb 20)
Focus: Initial setup, CI/CD, and basic integration slice.
* [ ] Saved Campaign & Subscription: Design database schemas for `SavedCampaign` and `Subscription` models to handle recurring donation periods[cite: 142].
* [ ] Saved Campaign & Subscription: Define API communication contracts for receiving status updates from the Campaign and Wallet modules[cite: 147].
* [ ] Saved Campaign & Subscription: Set up the initial repository structure ensuring integration with the group's CI/CD pipeline.

### Milestone 2: 25% Progress (Due March 6)
Focus: Integration between frontend, backend, and database for core module features.
* [ ] Saved Campaign & Subscription: Implement the "Save" and "Remove" campaign functionality for authenticated users[cite: 135, 139].
* [ ] Saved Campaign & Subscription: Build the frontend view for users to see their list of saved campaigns[cite: 136].
* [ ] Saved Campaign & Subscription: Establish the basic integration demonstrating data flow from frontend to database for the "Save" feature.

### Milestone 3: 50% Progress
Focus: Inter-module communication and secondary features.
* [ ] Saved Campaign & Subscription: Develop the Subscription engine logic for daily, weekly, and monthly donation periods[cite: 142].
* [ ] Saved Campaign & Subscription: Integrate with the Wallet module to enforce the "Internal Wallet Only" payment constraint for subscriptions[cite: 143].
* [ ] Saved Campaign & Subscription: Implement UI and backend logic for changing subscription durations or canceling active subscriptions[cite: 144, 145].

### Milestone 4: 75% Progress
Focus: Complex logic, subscriptions, and asynchronous processes.
* [ ] Saved Campaign & Subscription: Build the automated email trigger for campaigns on a user's favorite list that reach 98% of their target[cite: 138].
* [ ] Saved Campaign & Subscription: Implement the "Automatic Termination" logic to stop subscriptions if a campaign is deleted or fails[cite: 132].
* [ ] Saved Campaign & Subscription: Develop the confirmation warning and automatic debiting system for new subscriptions[cite: 141].

### Milestone 5: 100% Progress (Final)
Focus: Edge cases, bulk actions, and final polish.
* [ ] Saved Campaign & Subscription: Implement "Insufficient Balance" notification logic for failed subscription debits[cite: 120].
* [ ] Saved Campaign & Subscription: Finalize asynchronous consistency to ensure saved data reflects campaign status changes without interrupting other user activities[cite: 133, 151].
* [ ] Saved Campaign & Subscription: Complete final unit testing and technical design justification for the module's architecture.