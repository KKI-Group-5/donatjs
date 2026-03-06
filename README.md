README for DonatJS.

## Work Plan & Milestones
This project is divided into 5 main milestones. The tasks below detail the development plan for each module. Please add your specific module tasks and assign a Person In Charge (PIC) for each item.

### Milestone 1: Preparation (Due Feb 20)
**Focus:** Initial setup, CI/CD, and basic integration slice. 
* [ ] **Authentication & User Profile:** Set up the module repository, database schemas, and establish the CI/CD pipeline. 
* [ ] **Authentication & User Profile:** Implement the basic account creation endpoint using email, password, name, date of birth, and bio to demonstrate database integration. 
* [ ] **Authentication & User Profile:** Implement the basic login mechanism allowing users to authenticate into the system. 

### Milestone 2: 25% Progress (Due March 6)
**Focus:** Core feature functionality and continuous deployment readiness. 
* [ ] **Authentication & User Profile:** Implement the features allowing users to view and update their profile information. 
* [ ] **Authentication & User Profile:** Integrate third-party authentication logic (e.g., Google login) as an alternative login method. 
* [ ] **Authentication & User Profile:** Establish access control so unauthenticated guests can view campaigns but are redirected when attempting donations. 

### Milestone 3: 50% Progress
**Focus:** Inter-module communication and secondary features. 
* [ ] **Authentication & User Profile:** Build the endpoints to aggregate and display the user's campaign activities, donations, and subscriptions on their profile. 
* [ ] **Authentication & User Profile:** Implement the asynchronous communication pipeline so profile updates reflect immediately in Campaign and Donation modules without disrupting user activities. 
* [ ] **Authentication & User Profile:** Create the base data structure to receive incoming updates regarding a user's FRAUD/REJECTED activity from the Campaign and Donation modules. 

### Milestone 4: 75% Progress
**Focus:** Complex logic, subscriptions, and asynchronous processes. 
* [ ] **Authentication & User Profile:** Implement the threshold logic to flag user accounts if their combined total of rejected campaigns and donations reaches 3 or more. 
* [ ] **Authentication & User Profile:** Build the automated notification system that sends flagged account details to admins for review. 
* [ ] **Authentication & User Profile:** Implement the Admin action endpoints to suspend accounts and block them from creating campaigns, making donations, or topping up wallets. 

### Milestone 5: 100% Progress (Final)
**Focus:** Edge cases, bulk actions, and final polish. 
* [ ] **Authentication & User Profile:** Implement the dispute feature allowing suspended users to appeal their suspension to the admins. 
* [ ] **Authentication & User Profile:** Finalize integration testing to ensure consistent data reference across all modules whenever identity data is queried. 
* [ ] **Authentication & User Profile:** Conduct final edge-case testing, resolve remaining bugs, and ensure the module meets all code quality standards and 100% test coverage. 