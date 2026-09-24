# JobApply AI

Android job-search assistant for user-controlled Naukri job discovery, extraction, matching, and application tracking.


## Current MVP

- Opens Naukri in an Android WebView.
- User signs in normally inside Naukri.
- Searches for a job title such as Senior .NET Developer.
- Reads visible job cards from the current Naukri page.
- Deduplicates listings and stores them locally with Room.
- Displays title, company, location, experience, salary and description.
- Opens the original listing when View is tapped.
- GitHub Actions builds a debug APK.

## Boundaries

The app does not collect or transmit the Naukri password, bypass CAPTCHA, evade anti-bot controls, defeat rate limits, or fabricate application answers. External-company application pages remain external.

The Naukri page structure can change. The extractor is isolated in NaukriJobExtractor so selectors can be updated without redesigning the rest of the app.
