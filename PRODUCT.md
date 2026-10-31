# LinkMe

<!-- impeccable:product-schema 1 -->

## Platform

android

## Product Purpose

Help people meet through shared interests, then start a one-to-one conversation. Preserve the original LinkMe name, native Android app and interest-based social idea.

## Users

People looking for friends around shared interests. A campus-only audience is not confirmed; this pass does not require school identity or location.

## Capabilities and Constraints

- Kotlin Android app, Material components, Firebase Authentication and Cloud Firestore.
- Strictly Firebase Spark ($0/month). Do not enable billing, Cloud Functions, SMS authentication or Cloud Storage.
- Existing Firebase project linkme-e0b8d is active and was verified in the console on October 30, 2026 (America/Chicago).
- Existing data is preserved. Private contact details must not be exposed through discovery.
- Proposed scope, selected as a working assumption while the optional user preference is pending: multiple-interest discovery, bios, real-time text chat, conversation inbox, private profile editing, password recovery and sign-out.

## Evidence on Hand

Original source at ce640c9e6a87fd21952b0214f42a93978e655815. Existing Firestore Users and Messages collections. No user count, engagement, geographic proximity or verification claims are justified. Demo data, if present in a debug-only demo, must be clearly labeled.

## Product Principles

- Make the shared interest visible before asking someone to start a conversation.
- A saved or sent status means the backend acknowledged it.
- Keep email and telephone details private.
- Explain empty and failed states and provide a next action.
