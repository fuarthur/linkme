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
- Existing Firebase project linkme-e0b8d is active on Spark; do not create a replacement or enable billing.
- Existing data is preserved. Private contact details must not be exposed through discovery.
- User-delegated scope: a complete native interest-based friendship app with discovery, introductions, real-time private text chat, an inbox, profile editing, password recovery and sign-out.

## Interest discovery

- A searchable local catalog of148 topics in12 groups offers specific starting points and selected Chinese/English aliases.
- People can keep up to16 interests, create their own labels (1–40 UTF-16 units), and remove or replace them. Custom display wording is retained; equivalent spelling/case/whitespace is deduplicated.
- Literal topics in the introduction and three optional scene questions produce labeled suggestions. Suggestions stay unselected until the person chooses them. No remote AI, chat/contact analysis, paid API or background profiling is involved.
- The interest editor returns a local draft. Profile Save publishes it; discovery editing changes only the search filters. Back cancels the draft.
- Discovery distinguishes exact shared topics from related catalog groups and ranks actual labels. Stored lookup keys are hints, never evidence or authorization.
- Two server queries, each capped at60 results, cover new keys and legacy labels; at most60 merged profiles are shown. This is a bounded sample, not global compatibility ranking.

## Evidence on Hand

Original source at ce640c9e6a87fd21952b0214f42a93978e655815. Existing Firestore Users and Messages collections. No user count, engagement, geographic proximity or verification claims are justified. Demo data, if present in a debug-only demo, must be clearly labeled.

## Product Principles

- Make the shared topic or related category visible before asking someone to start a conversation.
- Offer inspiration without silently assigning an identity or publishing inferred interests.
- A saved or sent status means the backend acknowledged it.
- Keep email and telephone details private.
- Explain empty and failed states and provide a next action.
