# JobApply AI

Android job-search and application assistant focused on user control.

## Current features
- Naukri WebView with normal user sign-in/session.
- Keyword search and visible job extraction with deduplication.
- Candidate profile/preferences stored locally.
- Skill and location matching with configurable minimum match.
- Saved application statuses: SAVED, APPLIED, INTERVIEW, REJECTED, SKIPPED, FAILED, EXTERNAL.
- Application tracker and status updates.
- Application message generation and copy-to-clipboard.
- Daily scheduled review notification through WorkManager.
- Android 8+ (API 26), target API 35.
- GitHub Actions debug APK build and artifact upload.

## Safety and user control
The app does not store a Naukri password, bypass CAPTCHA, evade anti-bot controls, fabricate experience, or silently submit applications. Final application submission remains a user action after reviewing the original listing.

## Limitations
Naukri's page structure can change; extraction is intentionally limited to visible page content. Background scheduling notifies the user to review/search; it does not perform hidden browser automation while the app is closed. External-company applications open in the user's browser and are not silently submitted.


## Production status
Version 1.2. Automated submission, CAPTCHA bypass and anti-bot evasion are intentionally not implemented. The app assists with discovery, matching, tracking and user-reviewed application preparation. APK installation still requires a successful CI build artifact or local Android build.
