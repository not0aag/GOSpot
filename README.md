# GOSpot

A live parking-availability companion for GO Transit commuters on the Lakeshore West line, built for a three-person final project (PROG39402 — Advanced Android).

> **Current status:** this repo implements Step 2's scope (Auth, MVVM, Firestore) and the core **Maps & Routing** features from Step 3. The geofencing and push notification components are designed and in active development.

## Team

| Name | Student ID |
|---|---|
| Alen Aiju George | 991716469 |
| Rupin Munjal | 991715259 |
| Ashish Garg | 991729422 |

Package name: `week11.st695922.finalproject`

## What's implemented

- **Google Maps Integration (Step 3)** — Live interactive map using Google Maps SDK with real-time station markers color-coded by occupancy (Green/Orange/Red)
- **Routing & Navigation (Step 3)** — Intent-based routing to the nearest or selected station with available parking
- **Firebase Authentication** — email/password sign in, sign up, sign out, and forgot-password (`sendPasswordResetEmail`)
- **MVVM architecture** — Repository → ViewModel → Compose UI, with all Firebase calls isolated behind suspend functions
- **Firestore CRUD** — a shared `stations` collection with real-time listeners, plus manual Check In / Check Out that updates live occupancy
- **Per-user data** — home station preference and a check-in/check-out event log under `users/{uid}`, backing a live Alerts screen
- **One-time location** — `FusedLocationProviderClient` powers a "Nearest" sort on the Stations list
- **State-driven navigation** — screen switching via a sealed `Route`/`UiState`, no `androidx.navigation.compose`
- **Firestore security rules** — first draft (`firestore.rules`)

## Scope of this build

This project follows a 14-week Advanced Android Application Development timeline. This repository currently reflects **Step 2** of a 4-step final project plan:

1.  **Step 1:** Project Proposal & Figma Prototype (Completed)
2.  **Step 2:** Firebase Auth, MVVM Architecture, and Firestore CRUD (Completed - Current State)
3.  **Step 3:** Maps & Routing — Advanced Topic Deliverables (In Progress)
4.  **Step 4:** Final Presentation & Demo (Upcoming)

Features currently being refined or in active development as part of Step 3 (due Week 13) include:

| Feature | Current Status | Step 3 Deliverable |
|---|---|---|
| Live interactive map | Implemented (Step 3) | Full Google Maps SDK rendering with real-time station markers |
| Automatic geofencing | Manual Stand-in | Background entry/exit detection via `GeofencingClient` |
| Route/Navigation | Implemented (Step 3) | Intent-based routing to the nearest available station |
| Lot-full push alerts | Mocked | Firebase Cloud Messaging (FCM) integration |

## Setup

1. Clone the repo and open it in Android Studio.
2. Create a Firebase project and register an Android app with package name `week11.st695922.finalproject`.
3. Download `google-services.json` and place it at `app/google-services.json` (it's gitignored — every developer needs their own copy or a shared one passed outside of git).
4. In the Firebase console, enable **Authentication → Sign-in method → Email/Password** and create a **Firestore Database** (Standard edition, test-mode rules to start).
5. Publish `firestore.rules` from this repo to the Firestore **Rules** tab.
6. **Google Maps API Key**:
   - Open [local.properties](local.properties) in the root directory.
   - Add the following line: `MAPS_API_KEY=your_api_key_here`
   - The project uses the `secrets-gradle-plugin` to safely inject this key into the manifest without pushing it to GitHub.
7. Run the app. On first launch, use the "Load demo stations" button (shown when the stations list is empty) to seed the six Lakeshore West lots with the capacities/occupancy from the Figma mocks.

## Project structure

```
app/src/main/java/week11/st695922/finalproject/
├── model/          Station, UserProfile, CheckInEvent
├── data/           Repositories - all Firebase calls live here
├── viewmodel/      AuthViewModel, StationViewModel, ProfileViewModel, AlertViewModel, LocationViewModel, MapViewModel
├── ui/
│   ├── state/      UiState / AuthUiState sealed wrappers
│   ├── navigation/ Route sealed interface (state-driven screen switching)
│   ├── theme/      Material 3 theme, green palette matching the Figma mocks
│   ├── components/ Reusable buttons, text fields, occupancy bar, bottom nav
│   ├── screens/    One composable per screen
│   └── GoSpotApp.kt  Root composable wiring auth state → navigation → screens
└── MainActivity.kt
```

Design references are under `app/src/main/screen design/` (Figma exports used to match layouts).
