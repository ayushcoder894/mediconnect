# Product backlog

User stories behind MediConnect, grouped by the 1-week sprint they were delivered in.
Each story is tracked as a GitHub issue under the matching milestone.

## Sprint 1: Foundations
- As a **patient**, I want to create an account and log in securely, so that my health data is private.
- As a **doctor**, I want to register with my specialization and fee, so that patients can find me once I am approved.
- As an **admin**, I want to approve new doctors, so that only verified doctors appear in search.
- As a **developer**, I want the whole stack to start with one Docker Compose command, so that anyone can run it locally.

## Sprint 2: Booking
- As a **patient**, I want to search doctors by specialty, city and fee, so that I can find the right one quickly.
- As a **patient**, I want to see free slots for the next two weeks and book one, so that I don't have to call the hospital.
- As a **patient**, I want booking to never double-book a slot, so that I am not turned away on arrival.
- As a **patient**, I want to cancel or reschedule, so that I can adapt to changes in my plans.
- As a **doctor**, I want to set my weekly hours and slot length, so that bookings match when I actually work.
- As a **doctor**, I want a daily schedule where I can confirm visits and mark no-shows.

## Sprint 3: Clinical workflow
- As a **patient**, I want to book lab tests from a catalog with prices, so that I know the cost up front.
- As **lab staff**, I want to mark samples collected and enter results, with abnormal values flagged automatically.
- As a **patient**, I want to download my lab report and prescriptions as PDFs.
- As a **doctor**, I want to write a digital prescription and order follow-up tests during a visit.
- As a **doctor**, I want to see my patient's past visits, labs and prescriptions before the appointment.
- As an **admin**, I want every access to a patient record to be audit-logged.
- As a **patient**, I want to rate a completed visit, so that others can choose doctors with confidence.

## Sprint 4: AI, notifications and analytics
- As a **patient**, I want a diabetes risk estimate prefilled from my latest lab results, with the reasons behind it.
- As a **patient** with high risk, I want a one-click path to book an endocrinologist.
- As a **patient**, I want to ask insurance questions and get answers with policy citations.
- As a **patient**, I want confirmation and 24-hour reminder emails, never duplicated.
- As **front-desk staff**, I want to check patients in by scanning the QR on their slip.
- As an **admin**, I want a dashboard of appointments, revenue, no-shows, lab demand and risk distribution.
- As a **developer**, I want CI to run all test suites on every pull request and CD to publish images from `main`.
