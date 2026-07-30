# GOSpot

A live parking-availability companion for GO Transit commuters on the Lakeshore West line, built for a three-person final project (PROG39402 — Advanced Android).

> **Step 2 status:** this repo currently implements Step 2's scope — Firebase Auth, MVVM architecture, and a first-draft Firestore CRUD feature. The full GOSpot vision (live interactive map, automatic geofence check-in, push notifications) is designed in the Figma mocks but intentionally **not** implemented yet — see [Scope of this build](#scope-of-this-build) below for why.

## Team

| Name | Student ID |
|---|---|
| Alen Aiju George | 991716469 |
| Rupin Munjal | 991715259 |
| Ashish Garg | 991729422 |

Package name: `week11.st695922.finalproject`

## What's implemented

- **Firebase Authentication** — email/password sign in, sign up, sign out, and forgot-password (`sendPasswordResetEmail`)
- **MVVM architecture** — Repository → ViewModel → Compose UI, with all Firebase calls isolated behind suspend functions
- **Firestore CRUD** — a shared `stations` collection with real-time listeners, plus manual Check In / Check Out that updates live occupancy
- **Per-user data** — home station preference and a check-in/check-out event log under `users/{uid}`, backing a live Alerts screen
- **One-time location** — `FusedLocationProviderClient` powers a "Nearest" sort on the Stations list
- **State-driven navigation** — screen switching via a sealed `Route`/`UiState`, no `androidx.navigation.compose`
- **Firestore security rules** — first draft (`firestore.rules`)

## Scope of this build

This project is built against the material covered in a 9-week intro Android course (Firebase Auth, Firestore CRUD, MVVM, one-time location). The full GOSpot concept designed in Figma calls for a few things that course material doesn't cover yet:

| Design feature | Why it's stubbed | What stands in for it |
|---|---|---|
| Live interactive map | No Google Maps SDK / `GoogleMap` composable covered | Static layout with placeholder pins |
| Automatic geofence check-in | No `GeofencingClient` / background location covered | Manual Check In / Check Out button |
| Lot-full push alerts | No Firebase Cloud Messaging covered | Local UI toggle, not wired to real notifications |

## Setup

1. Clone the repo and open it in Android Studio.
2. Create a Firebase project and register an Android app with package name `week11.st695922.finalproject`.
3. Download `google-services.json` and place it at `app/google-services.json` (it's gitignored — every developer needs their own copy or a shared one passed outside of git).
4. In the Firebase console, enable **Authentication → Sign-in method → Email/Password** and create a **Firestore Database** (Standard edition, test-mode rules to start).
5. Publish `firestore.rules` from this repo to the Firestore **Rules** tab.
6. Run the app. On first launch, use the "Load demo stations" button (shown when the stations list is empty) to seed the six Lakeshore West lots with the capacities/occupancy from the Figma mocks.

## Project structure

```
app/src/main/java/week11/st695922/finalproject/
├── model/          Station, UserProfile, CheckInEvent
├── data/           Repositories - all Firebase calls live here
├── viewmodel/      AuthViewModel, StationViewModel, ProfileViewModel, AlertViewModel, LocationViewModel
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
