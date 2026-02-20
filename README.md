\## Work Plan \& Milestones

This project is divided into 5 main milestones. The tasks below detail the development plan for each module. 



\### Milestone 1: Preparation (Due Feb 20)

\*\*Focus:\*\* Initial setup, CI/CD, and basic integration slice.

\* \[cite\_start]\[ ] \*\*Donation Management:\*\* Set up the initial Spring Boot repository including the basic `Campaign` entity and `LandingController` for the integration slice. \*\*(PIC: Khayru)\*\*

\* \[cite\_start]\[ ] \*\*Donation Management:\*\* Create the basic HTML view to prove frontend-backend-database integration runs locally without errors. \*\*(PIC: Khayru)\*\*

\* \[cite\_start]\[ ] \*\*Donation Management:\*\* Implement and verify the dummy CI/CD GitHub Actions script to satisfy the preparation deadline constraints. \*\*(PIC: Khayru)\*\*



\### Milestone 2: 25% Progress (Due March 6)

\*\*Focus:\*\* Basic CRUD, database transition, and AWS Deployment.

\* \[cite\_start]\[ ] \*\*Donation Management:\*\* Implement the core backend endpoint for users to create a donation record by entering an amount and adding notes\[cite: 96]. \*\*(PIC: Khayru)\*\*

\* \[cite\_start]\[ ] \*\*Donation Management:\*\* Add payment method selection logic, ensuring bank methods incur a Rp 1,500 fee and digital/e-wallets incur a Rp 2,000 fee\[cite: 97, 98]. \*\*(PIC: Khayru)\*\*

\* \[cite\_start]\[ ] \*\*Donation Management:\*\* Deploy the working API to AWS, completing the delayed Continuous Deployment (CD) requirement for the Even 2025/2026 semester\[cite: 403]. \*\*(PIC: Khayru)\*\*



\### Milestone 3: 50% Progress

\*\*Focus:\*\* Inter-module communication and secondary features.

\* \[cite\_start]\[ ] \*\*Donation Management:\*\* Build validation logic to enforce a maximum donation limit of 5 million rupiah, setting the status to REJECTED if exceeded or SUCCESS if valid\[cite: 103, 104, 106]. \*\*(PIC: Khayru)\*\*

\* \[cite\_start]\[ ] \*\*Donation Management:\*\* Develop the system notification feature to alert admins when a user makes a REJECTED donation, including the donation details\[cite: 105]. \*\*(PIC: Khayru)\*\*

\* \[cite\_start]\[ ] \*\*Donation Management:\*\* Integrate with the Campaign module to ensure the campaign's total funds are consistently updated when a SUCCESS donation is recorded\[cite: 94, 108]. \*\*(PIC: Khayru)\*\*



\### Milestone 4: 75% Progress

\*\*Focus:\*\* Complex logic, subscriptions, and asynchronous processes.

\* \[cite\_start]\[ ] \*\*Donation Management:\*\* Create the API endpoints and frontend views for authenticated users to view their list of past donations on their profile\[cite: 100]. \*\*(PIC: Khayru)\*\*

\* \[cite\_start]\[ ] \*\*Donation Management:\*\* Implement the logic to display the total accumulated donations on the specific campaign details page\[cite: 101]. \*\*(PIC: Khayru)\*\*

\* \[cite\_start]\[ ] \*\*Donation Management:\*\* Integrate with the Wallet module so that donations correctly calculate and notify the wallet to deduct the appropriate balance\[cite: 109, 117]. \*\*(PIC: Khayru)\*\*



\### Milestone 5: 100% Progress (Final)

\*\*Focus:\*\* Edge cases, bulk actions, and final polish.

\* \[cite\_start]\[ ] \*\*Donation Management:\*\* Implement restriction logic ensuring users cannot manually withdraw their donations, routing withdrawal requests to require admin approval\[cite: 93]. \*\*(PIC: Khayru)\*\*

\* \[cite\_start]\[ ] \*\*Donation Management:\*\* Finalize the "update donation record" functionality \[cite: 99] \[cite\_start]and verify donations halt if a campaign status is no longer OPEN\[cite: 92]. \*\*(PIC: Khayru)\*\*

\* \[cite\_start]\[ ] \*\*Donation Management:\*\* Conduct final integration testing across User Profile, Campaign, and Wallet modules to ensure data consistency without direct component calls\[cite: 151, 157]. \*\*(PIC: Khayru)\*\*

