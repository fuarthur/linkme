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

- A searchable local starter catalog of 148 topics in 12 groups offers specific starting points and selected Chinese/English aliases. Its existing aliases remain available.
- Browse Wiki topics adds an offline bundled snapshot of the 30,919 Wikidata-sourced topics in 23 domains already used by OtherWise’s public pool. All 718 separately authored OtherWise records are excluded. This is an English-title Wiki library, not a fully translated Chinese catalog.
- The native Wiki picker supports search, one field filter at a time, visible titles/descriptions/fields, checked selections, full topic details and Wikidata source links. The toolbar Sources dialog credits Wikidata, Wikipedia/Wikimedia contributors and OtherWise, with CC0 and CC BY-SA 4.0 licensing. It works without starting OtherWise or adding a backend service, API cost or network dependency for topic browsing.
- People can keep up to 16 interests, create their own labels (1–40 UTF-16 units), and remove or replace them. Custom display wording is retained; equivalent spelling/case/whitespace is deduplicated. Wiki search accepts up to 120 UTF-16 units; stored values keep the existing 40-unit bound.
- Ordinary Wiki titles retain literal stored values. The 152 compact Wiki references preserve 147 long titles and five meanings that would otherwise be narrowed by existing starter aliases. Full labels resolve through a small generated lookup; profile rendering does not load the 31k-topic library. Existing interest keys and the current Firestore policy remain unchanged.
- Literal topics in the introduction and three optional scene questions produce labeled suggestions. Suggestions stay unselected until the person chooses them. No remote AI, chat/contact analysis, paid API or background profiling is involved.
- The Wiki picker receives the parent editor’s current mix and optional query. Only its explicit Use action returns a local draft; Back cancels that picker draft. The parent editor’s Use returns its own local draft to Profile or Discover. Profile Save owns publication; discovery editing changes only the separate search filters. Back from the parent editor cancels its draft.
- Discovery distinguishes exact shared topics from related catalog groups and ranks actual labels. Stored lookup keys are hints, never evidence or authorization.
- Two server queries, each capped at 60 results, cover new keys and legacy labels; at most 60 merged profiles are shown. This is a bounded sample, not global compatibility ranking.

## Evidence on Hand

Original source at ce640c9e6a87fd21952b0214f42a93978e655815. Existing Firestore Users and Messages collections. No user count, engagement, geographic proximity or verification claims are justified. Demo data, if present in a debug-only demo, must be clearly labeled.

## Product Principles

- Make the shared topic or related category visible before asking someone to start a conversation.
- Offer inspiration without silently assigning an identity or publishing inferred interests.
- A saved or sent status means the backend acknowledged it.
- Keep email and telephone details private.
- Explain empty and failed states and provide a next action.
